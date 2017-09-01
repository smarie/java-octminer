/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.octave.translation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.octave.OctaveScriptOperator;
import com.rapidminer.tools.Ontology;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.exception.OctaveClassCastException;
import dk.ange.octave.type.OctaveCell;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveString;
import dk.ange.octave.type.OctaveStruct;

/**
 * This class will transform an ExampleSet to a data frame and back. If special
 * attributes like class or weight are present, they will be part of the data
 * frame and their name will be stored in a variable named as the expression
 * with an attached postfix .class respectively .weight.
 * 
 * @author Sebastian Land
 * 
 */
public class ExampleSetTranslator implements OctaveTranslator<ExampleSet> {

	public static final String VARIABLE_CLASS_POSTFIX = ".label";
	public static final String VARIABLE_WEIGHT_POSTFIX = ".weight";

	@Override
	public ExampleSet importObject(OctaveEngine octaveEngine, String expression)
			throws OperatorException {

		// 1. ask octave for the result and convert it to a cell or struct
		OctaveObject dataExp = octaveEngine.get(expression);

		if (dataExp instanceof OctaveCell)
			return importCell((OctaveCell) dataExp, expression);
		else if (dataExp instanceof OctaveStruct)
			return importStruct((OctaveStruct) dataExp, expression);
		else
			throw new OperatorException(
					"Only cell and struct results can be translated to RapidMiner at the moment");

	}

	// The 1 X 1 struct is the only one which is supported by JavaOctave

	private ExampleSet importStruct(OctaveStruct dataExpStruct,
			String expression) throws UserError {

		String[] attributeNames;
		String labelName = null;
		String weightName = null;

		// two type of the OctaveStruct result , cell and matrix
		// "role" and "name" are the type cell, anther data is matrix

		String role = "role";
		String name = "name";
		String data = "data";

		OctaveObject nameObject = dataExpStruct.get(name);
		OctaveObject roleObject = dataExpStruct.get(role).shallowCopy();
		OctaveObject dataObject = dataExpStruct.get(data);
		

		
		return importStructoCell((OctaveCell) nameObject, (OctaveCell) roleObject,
				(OctaveCell) dataObject, expression);

	}

	private ExampleSet importStructoCell(OctaveCell nameExpCell,
			OctaveCell roleExpCell, OctaveCell dataExpObject, String expression)
			throws UserError {
		

		OctaveObject rolecell = roleExpCell.get(1,1);
		OctaveObject namecell = nameExpCell.get(1,1);
		OctaveObject dataMatrix = dataExpObject.get(1,1);
		
		
			return importStrucCell((OctaveCell) namecell, (OctaveCell) rolecell,
						(OctaveDouble) dataMatrix, expression);

	}

	
	
	private ExampleSet importStrucCell(OctaveCell nameExpCell,
			OctaveCell roleExpCell, OctaveDouble dataExpObject, String expression)
			throws UserError {
		try {
			
			
			String[] attributeNames;
			String[] attributeRoles;

			String labelName = null;
			String weightName = null;
			
			int[] size = nameExpCell.getSize();
			int nbCols = size[1];
			int nbRows = size[0];

			attributeRoles = new String[nbCols];
			attributeNames = new String[nbCols];

			// save all attributeNames and attributeRoles
			try {
				for (int j = 0; j < nbCols; j++) {
					attributeNames[j] = nameExpCell.get(OctaveString.class, 1,
							j + 1).getString();
					attributeRoles[j] = roleExpCell.get(OctaveString.class, 1,
							j + 1).getString();
					if ("label".compareToIgnoreCase(attributeRoles[j]) == 0) {
						labelName = attributeNames[j];
					} else if ("weight".compareToIgnoreCase(attributeRoles[j]) == 0) {
						weightName = attributeNames[j];
					} else {
						// dont even care about the role provided in the cell
					}
				}
			} catch (OctaveClassCastException e) {
				throw e;
			}

			// creating attributes

			Attribute attributes[] = new Attribute[nbCols];

			for (int i = 0; i < nbCols; i++) {
				if (attributeNames[i] != null)
					attributes[i] = getAttribute(attributeNames[i], dataExpObject);
				else
					attributes[i] = getAttribute("att" + i, dataExpObject);
			}

			// Save all data

			int[] sizeData = dataExpObject.getSize();
			int nbColsD = sizeData[1];
			int nbRowsD = sizeData[0];

			double[] dataValues = new double[nbColsD*nbRowsD];
			dataValues=dataExpObject.getData();
			double[][] dataValuesMatrix = new double[nbRowsD][nbColsD];
			
			for (int r = 0; r < nbRowsD; r++) {
				for (int c = 0; c < nbColsD; c++) {
					dataValuesMatrix[r][(c)] = dataValues[(r) + (c) * nbRowsD];
				}
			}
			// creating memory example table
			

			MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

			for (int i = 0; i < nbRowsD; i++) {
				double[] dataRowData = new double[attributes.length];
				for (int j = 0; j < attributes.length; j++)
					dataRowData[j] = dataValuesMatrix[i][j];
				DataRow dataRow = new DoubleArrayDataRow(dataRowData);
				exampleTable.addDataRow(dataRow);
			}

			// now generate ExampleSet and setting special attributes

			ExampleSet exampleSet = exampleTable.createExampleSet();
			if (labelName != null) {
				Attribute labelAttribute = exampleSet.getAttributes().get(
						labelName);
				exampleSet.getAttributes().setSpecialAttribute(labelAttribute,
						Attributes.LABEL_NAME);
			}
			if (weightName != null) {
				Attribute weightAttribute = exampleSet.getAttributes().get(
						weightName);
				exampleSet.getAttributes().setSpecialAttribute(weightAttribute,
						Attributes.WEIGHT_NAME);
			}

			return exampleSet;

		} catch (Exception e) {
			throw new UserError(null, e,
					"octave.data_translation_error.import", expression,
					ExampleSet.class.getSimpleName());
		}

	}

	/**
	 * Imports a cell
	 * 
	 * @param dataExpCell
	 * @param expression
	 * @return
	 * @throws UserError
	 */
	private ExampleSet importCell(OctaveCell dataExpCell, String expression)
			throws UserError {
		try {

			/*
			 * 2. Checking dimensions. The attribute names will be the first row
			 * and the attribute roles will be the second row
			 */
			int[] size = dataExpCell.getSize();
			if (size.length < 2)
				throw new Exception("Resulting cell has less than 2 dimensions");

			int nbRows = size[0];
			if (nbRows != 3)
				throw new Exception(
						"Resulting cell should have 3 rows: name, role, and data vectors");
			int nbCols = size[1];

			/*
			 * 3. getting attribute names and roles
			 */
			String[] attributeNames;
			String labelName = null;
			String weightName = null;
			try {
				attributeNames = new String[nbCols];
				for (int j = 0; j < nbCols; j++) {
					attributeNames[j] = dataExpCell.get(OctaveString.class, 1,
							j + 1).getString();
					String attributeRole = dataExpCell.get(OctaveString.class,
							2, j + 1).getString();
					if ("label".compareToIgnoreCase(attributeRole) == 0) {
						labelName = attributeNames[j];
					} else if ("weight".compareToIgnoreCase(attributeRole) == 0) {
						weightName = attributeNames[j];
					} else {
						// dont even care about the role provided in the cell
					}
				}
			} catch (OctaveClassCastException e) {
				throw e;
			}

			/*
			 * 4. creating attributes
			 */
			Attribute attributes[] = new Attribute[nbCols];

			// for each entry in the 3d row, create the Rapidminer "Attribute"
			// description
			OctaveObject column;
			for (int i = 0; i < nbCols; i++) {
				column = dataExpCell.get(3, i + 1);
				if (attributeNames[i] != null)
					attributes[i] = getAttribute(attributeNames[i], column);
				else
					attributes[i] = getAttribute("att" + i, column);
			}

			// for each entry in the 3d row, copy the data
			double[][] dataValues = new double[nbCols][];
			int numberOfExamples = -1;
			for (int i = 0; i < nbCols; i++) {
				column = dataExpCell.get(3, i + 1);
				if (column instanceof OctaveCell) {
					// if the attribute is a cell, then it is an enum attribute
					// TODO
				
					throw new UserError(
							null,
							"octave.data_translation_error.import.enumsattributes_unsupported",
							expression);
				} else if (column instanceof OctaveDouble) {
					// if the attribute is a double[], we can handle it directly
					OctaveDouble doubleVector = (OctaveDouble) column;
					dataValues[i] = doubleVector.getData();
				}
				if (numberOfExamples > -1
						&& numberOfExamples != dataValues[i].length)
					throw new UserError(null,
							"octave.data_translation_error.import.example_set",
							expression);
				numberOfExamples = dataValues[i].length;
			}

			// creating memory example table
			MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);
			for (int i = 0; i < numberOfExamples; i++) {
				double[] dataRowData = new double[attributes.length];
				for (int j = 0; j < attributes.length; j++)
					dataRowData[j] = dataValues[j][i];
				DataRow dataRow = new DoubleArrayDataRow(dataRowData);
				exampleTable.addDataRow(dataRow);
			}

			// now generate ExampleSet and setting special attributes
			ExampleSet exampleSet = exampleTable.createExampleSet();
			if (labelName != null) {
				Attribute labelAttribute = exampleSet.getAttributes().get(
						labelName);
				exampleSet.getAttributes().setSpecialAttribute(labelAttribute,
						Attributes.LABEL_NAME);
			}
			if (weightName != null) {
				Attribute weightAttribute = exampleSet.getAttributes().get(
						weightName);
				exampleSet.getAttributes().setSpecialAttribute(weightAttribute,
						Attributes.WEIGHT_NAME);
			}

			return exampleSet;
		} catch (Exception e) {
			throw new UserError(null, e,
					"octave.data_translation_error.import", expression,
					ExampleSet.class.getSimpleName());
		}

	}

	/**
	 * Builds the Rapidminer attribute description by checking the type
	 * (numerical or "nominal", meaning an enumeration).
	 * 
	 * @param name
	 * @param columnOctave
	 * @return
	 * @throws UserError
	 */
	private Attribute getAttribute(String name, OctaveObject columnOctave)
			throws UserError {
		if (columnOctave != null) {
			if (columnOctave instanceof OctaveCell) {
				throw new UserError(null,
						"octave.data_translation_error.import.enumsattributes_unsupported");
				// TODO currently unsupported because would require to parse the
				// cell
				// in order to extract the distinct "levels" that can be taken

				// // the vector contain text elements
				// Attribute attribute =
				// AttributeFactory.createAttribute(name,Ontology.NOMINAL);
				//
				// // first run : extract all distinct values from the list
				// for (int level = 0; level < factor.levels().length; level++)
				// {
				// attribute.getMapping().mapString(factor.levels()[level]);
				// }
				// return attribute;

			} else if (columnOctave instanceof OctaveDouble) {
				return AttributeFactory.createAttribute(name,
						Ontology.NUMERICAL);
			}
		}
		throw new UserError(null,
				"octave.data_translation_error.import.missing_attribute_names");
	}

	@Override
	public void exportObject(OctaveEngine octaveEngine, String expression,
			IOObject ioObject, String getParameter) throws OperatorException {

		ExampleSet exampleSet = (ExampleSet) ioObject;
		Attributes attributes = exampleSet.getAttributes();
		
		int numberOfExamples = exampleSet.size();
		int numberOfAttributes = attributes.size();
				
		Attribute labelAttribute = attributes.getLabel();
		Attribute weightAttribute = attributes.getWeight();
		
		if (labelAttribute != null)
			numberOfAttributes++;
		if (weightAttribute != null)
			numberOfAttributes++;

		/*
		 * Building full size column arrays to avoid translation of indices...
		 */
		int[][] nominalData = new int[numberOfAttributes][];
		String[][] nominalDataValues = new String[numberOfAttributes][];
		double[][] numericalData = new double[numberOfAttributes][];

		// creating array of attributes to regard
		String[] attributeNames = new String[numberOfAttributes];
		Attribute[] copyAttributes = new Attribute[numberOfAttributes];
		
		
//		To get the simple attributes	
		int i = 0;
		for (Attribute attribute : attributes) {
			attributeNames[i] = attribute.getName();
			copyAttributes[i] = attribute;
			i++;
		}
		
//		To get the Special attributes	
		
//		Attribute labelAttributeSri = attributes.getId();
//		int numberOfAttributesAll = attributes.allSize();
//		
//		Attribute labelAttributeS = attributes.getSpecial("id");
//		String[] copyAttributeSpecial = new String[numberOfAttributes];
//		String[] attributeNameSpecial = new String[numberOfAttributesAll-numberOfAttributes];
//		
//		int sizeSp = labelAttributeS.getMapping().getValues().size();
//		for (int j=0;j<numberOfAttributesAll-numberOfAttributes;j++) {
//			attributeNameSpecial[j] = labelAttributeS.getName();
//			
//			for(int k = 0;k<sizeSp;k++)
//				
//			copyAttributeSpecial[k] = labelAttributeS.getMapping().getValues().get(k);
//			
//		}
//		
		
		if (labelAttribute != null) {
			attributeNames[i] = labelAttribute.getName();
			copyAttributes[i++] = labelAttribute;
		}
		if (weightAttribute != null) {
			attributeNames[i] = weightAttribute.getName();
			copyAttributes[i++] = weightAttribute;
		}

		// create data structure for attributes to copy
		i = 0;
		for (Attribute attribute : copyAttributes) {
			// data copy int
			if (attribute.isNominal()) {
				nominalData[i] = new int[numberOfExamples];
				NominalMapping mapping = attribute.getMapping();
				nominalDataValues[i] = new String[mapping.size()];
				for (int j = 0; j < mapping.size(); j++) {
					nominalDataValues[i][j] = mapping.mapIndex(j);
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(
					attribute.getValueType(), Ontology.DATE_TIME)) {
				nominalData[i] = new int[numberOfExamples];
				nominalDataValues[i] = new String[numberOfExamples];
				int j = 0;
				for (Example example : exampleSet) {
					nominalDataValues[i][j] = example
							.getValueAsString(attribute);
					j++;
				}
			} else if (attribute.isNumerical()) {
				numericalData[i] = new double[numberOfExamples];
			}

			i++;
		}

		// copying data
		int exampleIndex = 0;
		for (Example example : exampleSet) {
			int attributeIndex = 0;
			for (Attribute attribute : copyAttributes) {
				double value = example.getValue(attribute);
				if (attribute.isNominal()) {
					nominalData[attributeIndex][exampleIndex] = (int) value;
				} else if (attribute.isNumerical()) {
					numericalData[attributeIndex][exampleIndex] = value;

				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(
						attribute.getValueType(), Ontology.DATE_TIME)) {
					nominalData[attributeIndex][exampleIndex] = exampleIndex;
				}
				attributeIndex++;
			}
			exampleIndex++;
		}

		// TODO later : if parameter says structure, create a structure.
		// if cell, create a cell.
		// right now we create a structure only.
		
		
		 if (getParameter.equals("cell")){
			 OctaveObject oc;
			 oc = writeAsCell(attributeNames,copyAttributes, 
						attributes, numberOfExamples, numberOfAttributes, numericalData);
			 octaveEngine.put(expression, oc);	
			 System.out.println("The Data make as OctaceCell type");
		 } else if (getParameter.equals("struct")){
			 OctaveObject o;
				//if struct
				o = writeAsStruct(attributeNames,copyAttributes, 
						attributes, numberOfExamples, numberOfAttributes, numericalData);
				 octaveEngine.put(expression, o);	
			 System.out.println("The Data make as OctaceStruct type");
		 }else{
			 System.out.println("Errors");
		 }
	}

	
	// create ExempleSet like the OctaveStruct data
	private OctaveObject writeAsStruct(String[] attributeNames,
			Attribute[] copyAttributes, Attributes attributes,
			int numberOfExamples, int numberOfAttributes, Object[] numericalData) {

		final Map<String, OctaveObject> data = new HashMap<String, OctaveObject>();

		// create names and roles
		OctaveCell cellNames = new OctaveCell(1,attributeNames.length);
		OctaveCell cellRoles = new OctaveCell(1,attributeNames.length);
		for (int j = 0; j < attributeNames.length; j++) {
			cellNames.set(new OctaveString(attributeNames[j]), 1,j + 1);
			String role = attributes.findRoleByName(
					copyAttributes[j].getName()).getSpecialName();
			cellRoles.set(
					new OctaveString(role == null ? "":role), 1, j + 1);
		}
		data.put("name", cellNames);
		data.put("role", cellRoles);
		double[] vectData = new double[numberOfExamples * numberOfAttributes];

		for (int n = 0; n < numberOfAttributes; n++) {
			// OctaveDouble datatest = new OctaveDouble(numericalData[n], 1,
			// numberOfExamples);
			System.arraycopy(numericalData[n], 0, vectData, n
					* numberOfExamples, numberOfExamples);
		}
//		OctaveCell cellData = new OctaveCell(1,1);
//		cellData.set(new OctaveDouble(vectData, numberOfExamples,
//				numberOfAttributes), 1,1);
//		data.put("data", cellData);
		
		OctaveDouble datatest = new OctaveDouble(vectData, numberOfExamples,
				numberOfAttributes);
		data.put("data", datatest);

		return new OctaveStruct(data);
	}
	
	
	// create ExempleSet like the OctaveCell data
	
		private OctaveObject writeAsCell(String[] attributeNames,
				Attribute[] copyAttributes, Attributes attributes,
				int numberOfExamples, int numberOfAttributes, double[][] numericalData) {

			final Map<String, OctaveObject> data = new HashMap<String, OctaveObject>();

			// create names and roles
			OctaveCell cellData = new OctaveCell(3,attributeNames.length);
			
			//OctaveCell cellRoles = new OctaveCell(1,attributeNames.length);
			
			for (int j = 0; j < attributeNames.length; j++) {
				cellData.set(new OctaveString(attributeNames[j]), 1,j + 1);
				
				String role = attributes.findRoleByName(
						copyAttributes[j].getName()).getSpecialName();
				cellData.set(
						new OctaveString(role == null ? "":role), 2, j + 1);
				
				OctaveDouble datatest = new OctaveDouble(numericalData[j], 1,
				 numberOfExamples);
				
				cellData.set(datatest, 3,j + 1);
				
			}
			return cellData;
		}

	@Override
	public String getName() {
		return "Example Set";
	}

	@Override
	public Class<ExampleSet> getSupportedClass() {
		return ExampleSet.class;
	}

	@Override
	public boolean supportsFromOctave() {
		return true;
	}

	@Override
	public boolean supportsToOctave() {
		return true;
	}
}

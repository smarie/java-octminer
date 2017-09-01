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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omg.CORBA.UserException;

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

	public Log tlog = LogFactory
			.getLog("com.rapidminer.operator.octave.ExampleSetTranslator");

	public static final String VARIABLE_CLASS_POSTFIX = ".label";
	public static final String VARIABLE_WEIGHT_POSTFIX = ".weight";

	public static final String ROLE = "role";
	public static final String NAME = "name";
	public static final String DATA = "data";
	public static final String LEVELNAME = "levelname";

	/* the mandatory methods claiming what the translator is used for */

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

	/**
	 * The main method called by OctaveTranslator to import from Octave to
	 * Rapidminer
	 * 
	 * @see com.rapidminer.tools.octave.translation.OctaveTranslator#importObject(dk.ange.octave.OctaveEngine,
	 *      java.lang.String)
	 */
	@Override
	public ExampleSet importObject(OctaveEngine octaveEngine, String expression)
			throws OperatorException {

		// ask octave for the result and convert it to a cell or struct
		OctaveObject dataExp = octaveEngine.get(expression);

		if (dataExp instanceof OctaveCell)
			return importCell((OctaveCell) dataExp, expression);
		else if (dataExp instanceof OctaveStruct)
			return importStruct((OctaveStruct) dataExp, expression);
		else
			throw new OperatorException(
					"Only cell and struct results can be translated to RapidMiner at the moment");
	}

	/**
	 * Submethod for struct import from Octave to Rapidminer. The structure
	 * should have three members:
	 * <ul>
	 * <li><b>name</b>: a 1xn horizontal cell array with the attribute names
	 * <li><b>role</b>: a 1xn horizontal cell array with the role names.
	 * Currently only "label" and "weight" are supported. Otherwise use "".
	 * <li><b>data</b>: a 1x1 cell containing a mxn matrix of scalars where m is
	 * the number of samples.
	 * </ul>
	 * 
	 * @param dataExpStruct
	 * @param expression
	 * @return
	 * @throws UserError
	 */
	private ExampleSet importStruct(OctaveStruct dataExpStruct,
			String expression) throws UserError {

		OctaveObject nameObject = dataExpStruct.get(NAME);
		OctaveObject roleObject = dataExpStruct.get(ROLE);// .shallowCopy();
		OctaveObject dataObject = dataExpStruct.get(DATA);
		OctaveObject levelnameObject = dataExpStruct.get(LEVELNAME);

		// cast fields to cells
		OctaveCell namecell;
		OctaveCell rolecell;
		OctaveCell dataMatrixCell;
		OctaveDouble dataMatrix;
		OctaveCell levelnameCell = null;
		int[] size;
		int nbCols;
		int nbRows;
		int nbColsData ;
		int nbRowsData ;
		double[] levelId = null;

		/* NAME */
		if (nameObject instanceof OctaveCell) {
			namecell = (OctaveCell) nameObject;
			size = namecell.getSize();
			nbCols = size[1];
			nbRows = size[0];
			if (nbRows != 1)
				throw new UserError(null,
						"octave.data_translation_error.import", expression,
						"the name field should be a 1xn cell");
		} else {
			throw new UserError(null, "octave.data_translation_error.import",
					expression, "the name field was not a cell");
		}

		/* ROLE */
		if (roleObject instanceof OctaveCell) {
			rolecell = (OctaveCell) roleObject;
			size = rolecell.getSize();
			if (size[1] != nbCols || size[0] != nbRows)
				throw new UserError(null,
						"octave.data_translation_error.import", expression,
						"the role field should be a 1xn cell");
		} else {
			throw new UserError(null, "octave.data_translation_error.import",
					expression, "the role field was not a cell");
		}

		/* DATA */
		/**
		 * Change the function type if you want all the data like an matrix
		 * direct, Or in to an cell 1X1 that with the data matrix . now it is in
		 * cell;
		 */
		if (dataObject instanceof OctaveCell) {
			dataMatrixCell = (OctaveCell) dataObject;
			// dataMatrixCell = ((OctaveDouble) dataObject);
			size = dataMatrixCell.getSize();
			nbColsData = size[1];
			nbRowsData = size[0];
			// dataMatrixCell = (OctaveCell) dataObject;
			if ((1 == nbRowsData) && (1 == nbColsData)
					&& dataMatrixCell.get(1, 1) instanceof OctaveDouble) {
				dataMatrix = (OctaveDouble) dataMatrixCell.get(1, 1);
				
			} else {
				throw new UserError(null,
						"octave.data_translation_error.import", expression,
						"the data field should be a cell of size 1x1 containing a matrix");
			}
		} else {
			throw new UserError(null, "octave.data_translation_error.import",
					expression, "the data field was not a cell");
		}

		// LEVELNAME (Strings used to represent a number)
		if (levelnameObject != null) {
			if (levelnameObject instanceof OctaveCell) {

				levelnameCell = (OctaveCell) levelnameObject;
				size = levelnameCell.getSize();
				
				// size[1] must be the same size of special attribute,
				// size[0] must be the same size like ExempleSet
				
				if (size[0] != dataMatrix.size(1))
					throw new UserError(null,
							"octave.data_translation_error.import", expression,
							"the levelname field should be a nx1 cell");
			} else {
				throw new UserError(null,
						"octave.data_translation_error.import", expression,
						"the levelname field was not a cell");
			}
		}

		try {
			/* the destination Rapidminer structures */
			String[] attributeNames;
			String[] attributeRoles;

			/* the names of the special attributes */
			String idName = null;
			String labelName = null;
			String weightName = null;
			String predictionName = null;
			String clusterName = null;
			String batchName = null;

			attributeRoles = new String[nbCols];
			attributeNames = new String[nbCols];
			int numbrespecial = 0 ;
			// save all attributeNames and attributeRoles
			// for the structe exempelset data , 
			// the nbCols is the number of the data numeric;
			try {
				for (int j = 0; j < nbCols; j++) {
					attributeNames[j] = namecell.get(OctaveString.class, 1,
							j + 1).getString();
					attributeRoles[j] = rolecell.get(OctaveString.class, 1,
							j + 1).getString();

					// find the special attribute , 
					// if there is , the number of attribute + 1
					if (Attributes.LABEL_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						labelName = attributeNames[j];
						numbrespecial++;
					} else if (Attributes.WEIGHT_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						weightName = attributeNames[j];
						numbrespecial++;
					} else if (Attributes.ID_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						idName = attributeNames[j];
						numbrespecial++;
					} else if (Attributes.PREDICTION_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						predictionName = attributeNames[j];
						numbrespecial++;
					} else if (Attributes.CLUSTER_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						clusterName = attributeNames[j];
						numbrespecial++;
					} else if (Attributes.BATCH_NAME
							.compareToIgnoreCase(attributeRoles[j]) == 0) {
						batchName = attributeNames[j];
						numbrespecial++;
					} else {
						// dont even care about the role provided in the cell
					}
				}
			} catch (OctaveClassCastException e) {
				throw e;
			}

			// creating Rapidminer attributes
			Attribute attributes[] = new Attribute[nbCols];

			String attname;
			// if there is special attribute , we record the index about each element;
			if (levelnameObject == null) {
				// A- if all of attribute is numeric, copy data directly
				for (int i = 0; i < nbCols - numbrespecial; i++) {
					attname = (attributeNames[i] != null) ? attributeNames[i]
							: ("att" + i);
					attributes[i] = getAttribute(attname, dataMatrix);
					
				}
				
				
			} else {
				// B- if attribute is not numeric, copy the index of level to data matrix
				double[] levelIds = new double[((OctaveCell) levelnameCell).size(1)];
				for (int i = 0; i < nbCols - numbrespecial; i++) {
					attname = (attributeNames[i] != null) ? attributeNames[i]
							: ("att" + i);
					attributes[i] = getAttribute(attname, dataMatrix);
					
				}
				
				/***
				 * TODO if there are two role attribute
				 */
				for (int i = 0; i < numbrespecial; i++) {
					attname = attributeNames[nbCols-numbrespecial+i] ;
					
					attributes[nbCols-numbrespecial+i]=getAttributeWithLevels(attname, levelnameCell);
					

					for (int j = 0; j < ((OctaveCell) levelnameCell).size(1); j++) {
						OctaveObject level = ((OctaveCell) levelnameCell)
								.get(j+1, 1);
						if (level instanceof OctaveString) {
							levelIds[j] = attributes[nbCols-numbrespecial+i].getMapping().mapString(
									((OctaveString) level).getString());
					
				}
					}
				}
				
				levelId=levelIds;
			}

			// Save all data
			int[] sizeData = dataMatrix.getSize();
			int nbColsD = sizeData[1];
			int nbRowsD = sizeData[0];

			double[] dataValues = new double[nbColsD * nbRowsD];
			dataValues = dataMatrix.getData();
			double[][] dataValuesMatrix = new double[nbRowsD][nbColsD];
			//dataValuesMatrix[nbColsD-1]=levelIds;
			for (int r = 0; r < nbRowsD; r++) {
				for (int c = 0; c < nbColsD; c++) {
					dataValuesMatrix[r][(c)] = dataValues[(r) + (c) * nbRowsD];
				}
			}

			// creating memory example table
			MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

			for (int i = 0; i < nbRowsD; i++) {
				double[] dataRowData = new double[nbColsD];
				for (int j = 0; j < nbColsD; j++){
					dataRowData[j] = dataValuesMatrix[i][j];
					}
				if (levelId != null){
					
					dataRowData[nbColsD-1]=levelId[i];
					
				} 
				
				DataRow dataRow = new DoubleArrayDataRow(dataRowData);
				
				exampleTable.addDataRow(dataRow);

			}

			// now generate ExampleSet and setting special attributes
			ExampleSet exampleSet = exampleTable.createExampleSet();
			tagExampleSetWithSpecialAttributes(exampleSet, labelName,
					weightName, idName, predictionName, clusterName, batchName);

			return exampleSet;

		} catch (Exception e) {
			throw new UserError(null, e,
					"octave.data_translation_error.import", expression,
					ExampleSet.class.getSimpleName());
		}

	}

	/**
	 * Imports a cell
	 /**
	 * Submethod for cell import from Octave to Rapidminer. The structure
	 * should have three members:
	 * <ul>
	 * <li><b>name</b>: the first cell is a 1xn horizontal cell array with the attribute names in each cell
	 * <li><b>role</b>: the seconder cell is a 1xn horizontal cell array with the role names,if it exist ,if not " "; 
	 * <li><b>data</b>: the 3rd cell  containing a mxn matrix of cell where m is
	 * the number of samples.
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
			/* the names of the special attributes */
			String idName = null;
			String labelName = null;
			String weightName = null;
			String predictionName = null;
			String clusterName = null;
			String batchName = null;
			try {
				attributeNames = new String[nbCols];
				for (int j = 0; j < nbCols; j++) {
					attributeNames[j] = dataExpCell.get(OctaveString.class, 1,
							j + 1).getString();
					String attributeRole = dataExpCell.get(OctaveString.class,
							2, j + 1).getString();
					if (Attributes.LABEL_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						labelName = attributeNames[j];
					} else if (Attributes.WEIGHT_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						weightName = attributeNames[j];
					} else if (Attributes.ID_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						idName = attributeNames[j];
					} else if (Attributes.PREDICTION_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						predictionName = attributeNames[j];
					} else if (Attributes.CLUSTER_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						clusterName = attributeNames[j];
					} else if (Attributes.BATCH_NAME
							.compareToIgnoreCase(attributeRole) == 0) {
						batchName = attributeNames[j];
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
			OctaveObject attributeValues;
			for (int i = 0; i < nbCols; i++) {
				attributeValues = dataExpCell.get(3, i + 1);
				if (attributeNames[i] != null)
					attributes[i] = getAttribute(attributeNames[i],
							attributeValues);
				else
					attributes[i] = getAttribute("att" + i, attributeValues);
			}

			// for each entry in the 3d row of the main cell, copy the data
			double[][] dataValues = new double[nbCols][];
			int numberOfExamples = -1;
			for (int i = 0; i < nbCols; i++) {
				attributeValues = dataExpCell.get(3, i + 1);

				// A- if attribute is nominal
				if (attributeValues instanceof OctaveCell) {
					// loop through all rows of this attribute and convert the
					// values to numbers according to the levels mapping
					double[] levelIds = new double[((OctaveCell) attributeValues).size(1)];
					
					for (int j = 0; j < ((OctaveCell) attributeValues).size(1); j++) {
						OctaveObject level = ((OctaveCell) attributeValues)
								.get(j+1, 1);
						
						if (level != null) {
							if (level instanceof OctaveString) {
								levelIds[j] = attributes[i].getMapping().mapString(
										((OctaveString) level).getString());
							} else if (level instanceof OctaveDouble
									&& (((OctaveDouble) level).getData().length == 1)) {
								levelIds[j] = attributes[i].getMapping().mapString(
										Double.toString(((OctaveDouble) level)
												.getData()[0]));
							} else {
								// unsupported value for the level
								throw new UserError(
										null,
										"octave.data_translation_error.import.unsupported_attribute_value",
										"Attribute "
												+ attributes[i].getName()
												+ " has a value of unsupported type "
												+ level.getClass().getName());
							}
						} else {
							throw new UserError(
									null,
									"octave.data_translation_error.import.null_attribute_value",
									"Attribute " + attributes[i].getName()
											+ " has a null value");
						}
					}

					dataValues[i] = levelIds;

				}
				// B- if attribute is numeric, copy data directly
				else if (attributeValues instanceof OctaveDouble) {
					// if the attribute is a double[], we can handle it directly
					OctaveDouble doubleVector = (OctaveDouble) attributeValues;
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
			tagExampleSetWithSpecialAttributes(exampleSet, labelName,
					weightName, idName, predictionName, clusterName, batchName);

			return exampleSet;
		} catch (Exception e) {
			throw new UserError(null, e,
					"octave.data_translation_error.import", expression,
					ExampleSet.class.getSimpleName());
		}

	}

	/**
	 * Tags the specified attributes as special with according role in the given
	 * example set
	 * 
	 * @param exampleSet
	 * @param labelName
	 * @param weightName
	 * @param idName
	 * @param predictionName
	 * @param clusterName
	 * @param batchName
	 */
	private void tagExampleSetWithSpecialAttributes(ExampleSet exampleSet,
			String labelName, String weightName, String idName,
			String predictionName, String clusterName, String batchName) {

		if (labelName != null) {
			Attribute labelAttribute = exampleSet.getAttributes()
					.get(labelName);
			exampleSet.getAttributes().setSpecialAttribute(labelAttribute,
					Attributes.LABEL_NAME);
		}
		if (weightName != null) {
			Attribute weightAttribute = exampleSet.getAttributes().get(
					weightName);
			exampleSet.getAttributes().setSpecialAttribute(weightAttribute,
					Attributes.WEIGHT_NAME);
		}
		if (idName != null) {
			Attribute idAttribute = exampleSet.getAttributes().get(idName);
			exampleSet.getAttributes().setSpecialAttribute(idAttribute,
					Attributes.ID_NAME);
		}
		if (predictionName != null) {
			Attribute predictionAttribute = exampleSet.getAttributes().get(
					predictionName);
			exampleSet.getAttributes().setSpecialAttribute(predictionAttribute,
					Attributes.PREDICTION_NAME);
		}
		if (clusterName != null) {
			Attribute clusterAttribute = exampleSet.getAttributes().get(
					clusterName);
			exampleSet.getAttributes().setSpecialAttribute(clusterAttribute,
					Attributes.CLUSTER_NAME);
		}
		if (batchName != null) {
			Attribute batchAttribute = exampleSet.getAttributes()
					.get(batchName);
			exampleSet.getAttributes().setSpecialAttribute(batchAttribute,
					Attributes.BATCH_NAME);
		}

	}

	/**
	 * Builds the Rapidminer attribute description for an attibute with levels
	 * by checking the type in the levels object provided (only cells are
	 * supported for levels right now)
	 * 
	 * @param name
	 * @param levels
	 * @return
	 * @throws UserError
	 */
	private Attribute getAttributeWithLevels(String name, OctaveObject levels)
			throws UserError {
		if (levels != null) {
			if (levels instanceof OctaveCell) {
				return getAttribute(name, (OctaveCell) levels);
			} else
				throw new UserError(null,
						"octave.data_translation_error.import.wrong_attribute_levels_contenttype");
		} else
			throw new UserError(null,
					"octave.data_translation_error.import.wrong_attribute_levels_contenttype");
	}

	/**
	 * Builds the Rapidminer attribute description by checking the type in the
	 * column object provided (numerical or "nominal", meaning an enumeration).
	 * 
	 * @param name
	 * @param columnOctave
	 * @return
	 * @throws UserError
	 */
	private Attribute getAttribute(String name, OctaveObject column)
			throws UserError {
		if (column != null) {
			if (column instanceof OctaveDouble) {
				// return getAttribute(name,(OctaveDouble) column);
				return AttributeFactory.createAttribute(name,
						Ontology.NUMERICAL);

			} else if (column instanceof OctaveCell) {
				return getAttribute(name, (OctaveCell) column);
			} else
				throw new UserError(null,
						"octave.data_translation_error.import.wrong_attribute_contenttype");
		} else
			throw new UserError(null,
					"octave.data_translation_error.import.wrong_attribute_contenttype");
	}

	/**
	 * Builds the Rapidminer attribute description by checking the type
	 * (numerical or "nominal", meaning an enumeration).
	 * 
	 * @param attName
	 * @param dataOrLevels
	 * @return
	 * @throws UserError
	 */
	private Attribute getAttribute(String attName, OctaveCell dataOrLevels)
			throws UserError {

		if (dataOrLevels != null) {

			// the vector contain text elements
			Attribute attribute = AttributeFactory.createAttribute(attName,
					Ontology.NOMINAL);

			// first run : extract all distinct values from the list
			OctaveObject level;
			for (int i = 1; i < dataOrLevels.size(1); i++) {

				level = dataOrLevels.get(i, 1);
				if (level != null) {
					if (level instanceof OctaveString) {
						attribute.getMapping().mapString(
								((OctaveString) level).getString());
					} else if (level instanceof OctaveDouble
							&& (((OctaveDouble) level).getData().length == 1)) {
						attribute.getMapping().mapString(
								Double.toString(((OctaveDouble) level)
										.getData()[0]));
					} else {
						// unsupported value for the level
						throw new UserError(
								null,
								"octave.data_translation_error.import.unsupported_attribute_value",
								"Attribute " + attName
										+ " has a value of unsupported type "
										+ level.getClass().getName());
					}
				} else {
					throw new UserError(
							null,
							"octave.data_translation_error.import.null_attribute_value",
							"Attribute " + attName + " has a null value");
				}
			}
			return attribute;

		}
		throw new UserError(null,
				"octave.data_translation_error.import.missing_attribute_names",
				"Attribute " + attName + " has null contents");
	}

	/**
	 * The main method called by OctaveTranslator to export from Rapidminer to
	 * Octave
	 * 
	 * @see com.rapidminer.tools.octave.translation.OctaveTranslator#exportObject(dk.ange.octave.OctaveEngine,
	 *      java.lang.String, com.rapidminer.operator.IOObject,
	 *      java.lang.String)
	 */
	@Override
	public void exportObject(OctaveEngine octaveEngine, String expression,
			IOObject ioObject, String getParameter) throws OperatorException {

		ExampleSet exampleSet = (ExampleSet) ioObject;
		Attributes attributes = exampleSet.getAttributes();

		int numberOfExamples = exampleSet.size();
		int numberOfAttributes = attributes.size();
		int numberOfAttributesAll = attributes.allSize();

		Attribute labelAttribute = attributes.getLabel();
		Attribute weightAttribute = attributes.getWeight();
		Attribute idAttribute = attributes.getId();
		Attribute PredictionAttribute = attributes.getPredictedLabel();
		Attribute clusterAttribute = attributes.getCluster();
		
		if (idAttribute != null)
			numberOfAttributes++;
		if (clusterAttribute != null)
			numberOfAttributes++;
		if (PredictionAttribute != null)
			numberOfAttributes++;
		if (labelAttribute != null)
			numberOfAttributes++;
		if (weightAttribute != null)
			numberOfAttributes++;

		/*
		 * Building full size column arrays to avoid translation of indices...
		 */
		int[][] nominalData = new int[numberOfAttributesAll][];
		String[][] nominalDataValues = new String[numberOfAttributesAll][];
		double[][] numericalData = new double[numberOfAttributesAll][];

		// creating array of attributes to regard
		String[] attributeNames = new String[numberOfAttributesAll];
		Attribute[] copyAttributes = new Attribute[numberOfAttributesAll];

		// To get the simple attributes
		int i = 0;
		for (Attribute attribute : attributes) {
			attributeNames[i] = attribute.getName();
			copyAttributes[i] = attribute;
			i++;
		}

		/**
		 * The role like an attributes special,
		 * it can not copy the data which is in the attributes special; Fix
		 * [Not Final]
		 */
	
		if (idAttribute != null) {
			attributeNames[i] = idAttribute.getName();
			copyAttributes[i++] = idAttribute;
//			tlog.info("The data have a attribut special id ::: "
//					+ idAttribute.getName() + "\n");
		} else {
//			tlog.info("the number Of Data Attributes :::"
//							+ numberOfAttributes + "\n");

		}

		if (labelAttribute != null) {
			attributeNames[i] = labelAttribute.getName();
			copyAttributes[i++] = labelAttribute;
//			tlog.info("The data have a attribut special label ::: "
//					+ labelAttribute.getName() + "\n");
		}
		if (weightAttribute != null) {
			attributeNames[i] = weightAttribute.getName();
			copyAttributes[i++] = weightAttribute;
//			tlog.info("The data have a attribut special weight ::: "
//					+ weightAttribute.getName() + "\n");
		}

		if (PredictionAttribute != null) {
			attributeNames[i] = PredictionAttribute.getName();
			copyAttributes[i++] = PredictionAttribute;
//			tlog.info("The data have a attribut special Prediction ::: "
//					+ PredictionAttribute.getName() + "\n");
		}
		if (clusterAttribute != null) {
			attributeNames[i] = weightAttribute.getName();
			copyAttributes[i++] = clusterAttribute;
//			tlog.info("The data have a attribut special cluster ::: "
//					+ clusterAttribute.getName() + "\n");
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

					// special attributes value
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(
						attribute.getValueType(), Ontology.DATE_TIME)) {
					nominalData[attributeIndex][exampleIndex] = exampleIndex;
				}
				attributeIndex++;
			}
			exampleIndex++;
		}

		// if parameter says structure, create a structure.
		// if cell, create a cell.
		
		if (getParameter.equals("cell")) {
			OctaveObject oc;
			oc = writeAsCell(attributeNames, copyAttributes, attributes,
					numberOfExamples, numberOfAttributes, numericalData,nominalData);
			octaveEngine.put(expression, oc);
			//tlog.info("The output data type is OctaceCell \n");
			
		} else if (getParameter.equals("struct")) {
			OctaveObject o;
			// if structure
			o = writeAsStruct(attributeNames, copyAttributes, attributes,
					numberOfExamples, numberOfAttributes, numericalData,nominalData);
			octaveEngine.put(expression, o);
			//tlog.info("The Data make as OctaceStruct type\n");
			
		} else {
			tlog.info("Errors : the type of exemple set outputted is "+getParameter+" which is not supported\n");
		}
	}
	
	
	/**
	 * create ExempleSet like the OctaveStruct data
	 * 
	 * @param attributeNames
	 * @param copyAttributes
	 * @param attributes
	 * @param numberOfExamples
	 * @param numberOfAttributes
	 * @param numericalData
	 * @param nominalData
	 * @return
	 */
	private OctaveObject writeAsStruct(String[] attributeNames,
			Attribute[] copyAttributes, Attributes attributes,
			int numberOfExamples, int numberOfAttributes, Object[] numericalData,int[][] nominalData) {

		final Map<String, OctaveObject> data = new HashMap<String, OctaveObject>();
		
		// create names and roles
		int numAttsimple = attributes.size();
		OctaveCell cellNames = new OctaveCell(1, attributeNames.length);
		OctaveCell cellRoles = new OctaveCell(1, attributeNames.length);
		/**
		 * Test there is special attributes or not, if it exist , we create
		 * cellRolesvalus for save the values
		 */
		Attribute labelAttribute = attributes.getLabel();
		Attribute weightAttribute = attributes.getWeight();
		Attribute idAttribute = attributes.getId();
		Attribute PredictionAttribute = attributes.getPredictedLabel();
		Attribute clusterAttribute = attributes.getCluster();
		
		if (numAttsimple != numberOfAttributes) {
			OctaveCell cellRolesvalus = new OctaveCell(numberOfExamples,numberOfAttributes-numAttsimple);
			
			String[] copyAttributeSpecial = new String[numberOfExamples];
			String[] attributeNameSpecial = new String[numberOfAttributes
					- numAttsimple];
			
			
				int numberOfAttributesAll = attributes.allSize();
				for (int j = 0; j < numberOfAttributesAll - numAttsimple; j++) {
					if (idAttribute != null){
						attributeNameSpecial[j] = idAttribute.getName();
						for (int k = 0; k < numberOfExamples; k++) {

							copyAttributeSpecial[k] = idAttribute
									.getMapping().getValues().get(k);

							cellRolesvalus
									.set(new OctaveString(idAttribute
											.getMapping().getValues().get(nominalData[numAttsimple+j][k])),
											k+1, j+1);
						}
					}
						
					if (clusterAttribute != null){
						attributeNameSpecial[j] = clusterAttribute.getName();
						for (int k = 0; k < numberOfExamples; k++) {

							copyAttributeSpecial[k] = clusterAttribute
									.getMapping().getValues().get(k);

							cellRolesvalus
									.set(new OctaveString(clusterAttribute
											.getMapping().getValues().get(nominalData[numAttsimple+j][k])),
											k+1, j+1);
						}
					}
						
					if (PredictionAttribute != null){
						attributeNameSpecial[j] = PredictionAttribute.getName();
						for (int k = 0; k < numberOfExamples; k++) {

							copyAttributeSpecial[k] = PredictionAttribute
									.getMapping().getValues().get(k);

							cellRolesvalus
									.set(new OctaveString(PredictionAttribute
											.getMapping().getValues().get(nominalData[numAttsimple+j][k])),
											k+1, j+1);
						}
					}
						
					if (labelAttribute != null){
						attributeNameSpecial[j] = labelAttribute.getName();
						for (int k = 0; k < numberOfExamples; k++) {

							copyAttributeSpecial[k] = labelAttribute
									.getMapping().getValues().get(k);

							cellRolesvalus
									.set(new OctaveString(labelAttribute
											.getMapping().getValues().get(nominalData[numAttsimple+j][k])),
											k+1, j+1);
						}
					}
						
					if (weightAttribute != null){
						attributeNameSpecial[j] = weightAttribute.getName();
						for (int k = 0; k < numberOfExamples; k++) {

							copyAttributeSpecial[k] = weightAttribute
									.getMapping().getValues().get(k);

							cellRolesvalus
									.set(new OctaveString(weightAttribute
											.getMapping().getValues().get(nominalData[numAttsimple+j][k])),
											k+1, j+1);
						}
					}

				}

			data.put("levelname", cellRolesvalus);
		} else {
			tlog.info(" There are no special attributes in the input\n");
		}

		for (int j = 0; j < attributeNames.length; j++) {
			cellNames.set(new OctaveString(attributeNames[j]), 1, j + 1);
			// get role type id ,label, weight, cluster,prediction,batch
			String role = attributes
					.findRoleByName(copyAttributes[j].getName())
					.getSpecialName();
			cellRoles.set(new OctaveString(role == null ? "" : role), 1, j + 1);

		}

		/**
		 *  For all of the attributes special ,we create the role.
		 */

		data.put("name", cellNames);
		data.put("role", cellRoles);
		double[] vectData = new double[numberOfExamples * numberOfAttributes];

		for (int n = 0; n < numAttsimple; n++) {
			// OctaveDouble datatest = new OctaveDouble(numericalData[n], 1,
			// numberOfExamples);
			System.arraycopy(numericalData[n], 0, vectData, n
					* numberOfExamples, numberOfExamples);
		}
		/**
		 * Make the data in an cell 1x1 or like an matrix direct
		 */
		// Cell

		OctaveCell cellData = new OctaveCell(1, 1);
		cellData.set(new OctaveDouble(vectData, numberOfExamples,
				numberOfAttributes), 1, 1);
		// test add special attributes in data struct
		// cellData.set(cellRolesvalus, 1,2);
		data.put("data", cellData);

		// Matrix
		// OctaveDouble datatest = new OctaveDouble(vectData, numberOfExamples,
		// numberOfAttributes);
		// data.put("data", datatest);
		//tlog.info(data);
		return new OctaveStruct(data);
	}

	// create ExempleSet like the OctaveCell data

	private OctaveObject writeAsCell(String[] attributeNames,
			Attribute[] copyAttributes, Attributes attributes,
			int numberOfExamples, int numberOfAttributes,
			double[][] numericalData,int[][] nominalData) {

	
		// create names and roles

	
		
		OctaveCell cellData = new OctaveCell(3, attributeNames.length);

		for (int j = 0; j < attributeNames.length; j++) {
			cellData.set(new OctaveString(attributeNames[j]), 1, j + 1);

			String role = attributes
					.findRoleByName(copyAttributes[j].getName())
					.getSpecialName();
			cellData.set(new OctaveString(role == null ? "" : role), 2, j + 1);
		

			if (role == null) {
			
			OctaveDouble datatest = new OctaveDouble(numericalData[j], numberOfExamples,
					1);

			cellData.set(datatest, 3, j + 1);	
			} else 
			{
				/**
				 * finish all the type of role
				 * Only for id role now 
				 */

				
				OctaveCell cellRolesvalus = new OctaveCell(numberOfExamples, 1);				
				for (int k = 0; k < numberOfExamples; k++) {
					
						cellRolesvalus.set(new OctaveString(attributes
								.findRoleByName(copyAttributes[j].getName()).getAttribute()
									.getMapping().getValues().get(nominalData[j][k])),
									k+1,1);
				}
			
				cellData.set(cellRolesvalus, 3, j + 1);
			}

		}
		return cellData;
	}

}

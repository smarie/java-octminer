/*
 *  				  RapidMiner Octave Extension.
 *				
 * Copyright (C) 2012-present by Schneider Electric Industries SAS.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of Schneider Electric nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * For more information on this software, see http://www.java.net/projects/octminer.
 */
package com.rapidminer.tools.octave.translation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;

import dk.ange.octave.type.OctaveCell;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveString;
import dk.ange.octave.type.OctaveStruct;

/**
 * This class will transform an ExampleSet to a struct or cell and back. If
 * special attributes like class or weight are present, they will be part of the
 * struct or cell
 * 
 * @author Sylvain Marié
 * @author Yaoyu Zhang
 * 
 */
public class ExampleSetTranslator implements OctaveTranslator<ExampleSet> {

	// in Octave or matlab, type : datenum('01-Jan-1970')
	public static int OCTAVEDATE_1_JAN_1970 = 719529;

	// special "role" created to identify datetime attributes in octave
	public static String OCTAVE_DATETIME_ROLE = "datetime";

	public Log tlog = LogFactory
			.getLog("com.rapidminer.operator.octave.ExampleSetTranslator");

	public int[] index2;

	private TimeZone timeZone;

	private double dateRoundingDigits = 9;

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
	public ExampleSet importObject(OctaveEngineProxy octaveEngine,
			String objectName, Object... parameters) throws OperatorException {

		double dateRoundingDigits = ((Double) parameters[0]).doubleValue(); 
		
		// ask octave for the result and convert it to a cell or struct
		OctaveObject dataExp = octaveEngine.get(objectName);

		if (dataExp instanceof OctaveCell)
			return importCell((OctaveCell) dataExp, objectName, dateRoundingDigits);
		else if (dataExp instanceof OctaveStruct)
			return importStruct((OctaveStruct) dataExp, objectName, dateRoundingDigits);
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
	 * <li><b>data</b>: a 1x1 cell containing a m x n matrix of scalars where m
	 * is the number of samples.
	 * <li><b>level name</b>: a 1xn horizontal cell array with the role values.
	 * </ul>
	 * 
	 * @param dataExpStruct
	 * @param structName
	 * @param dateRoundingDigits2 
	 * @return
	 * @throws UserError
	 */
	private ExampleSet importStruct(OctaveStruct dataExpStruct,
			String structName, double dateRoundingDigits) throws UserError {
		// try{
		/* check fields of the structure: name, role, data, levelname */
		StructImportHelper s = new StructImportHelper(this);
		OctaveCell namecell = s.checkStructAttributeNames(dataExpStruct,
				structName);
		int nbAttributes = namecell.getSize()[1];
		OctaveCell rolecell = s.checkStructAttributeRoles(dataExpStruct,
				structName, nbAttributes);
		OctaveDouble dataMatrix = s.checkStructAttributesData(dataExpStruct,
				structName, nbAttributes);
		OctaveCell levelnameCell = s.checkStructNominalAttributeLevels(
				dataExpStruct, structName, nbAttributes);

		/* Extract the names and roles */
		String[] attributeNames = s.extractAttributeNames(namecell, structName);
		String[] attributeRoles = s.extractAttributeRoles(rolecell, structName);
		// int nbSpecialAttributes =
		// computeNbSpecialAttributes(attributeRoles);

		/* create Rapidminer attributes definitions */
		Attribute attributes[] = s.createRMAttributesDescriptionsForStructs(
				attributeNames, attributeRoles, dataMatrix, levelnameCell);

		/* Save all data */
		MemoryExampleTable exampleTable = s.createRMExampleTable(attributes,
				dataMatrix, dateRoundingDigits);

		// now generate ExampleSet and declare its special attributes
		ExampleSet exampleSet = createRapidminerExampleSet(exampleTable,
				attributeRoles);

		return exampleSet;

	}

	/**
	 * Imports a cell from Octave to Rapidminer. The cell should be of size 3xn
	 * :
	 * <ul>
	 * <li>the first row contains attribute names
	 * <li>the second row contains the role names
	 * <li>the 3rd row contains a mix of nx1 cells or nx1 arrays
	 * 
	 * @param dataExpCell
	 * @param cellName
	 * @param dateRoundingDigits2 
	 * @return
	 * @throws UserError
	 */
	private ExampleSet importCell(OctaveCell dataExpCell, String cellName, double dateRoundingDigits)
			throws UserError {
		// try {

		/* check that the cell contains 3 rows: name, role, data and n columns */
		CellImportHelper c = new CellImportHelper(this, dataExpCell, cellName);

		/* Extract the names and roles */
		String[] attributeNames = c.extractAttributeNames();
		String[] attributeRoles = c.extractAttributeRoles();

		/* create Rapidminer attributes definitions */
		Attribute attributes[] = c.createRMAttributeDescriptions(
				attributeNames, attributeRoles);

		// for each entry in the 3d row of the main cell, copy the data
		MemoryExampleTable exampleTable = c.createRMexampleTable(attributes, dateRoundingDigits);

		// now generate ExampleSet and setting special attributes
		ExampleSet exampleSet = createRapidminerExampleSet(exampleTable,
				attributeRoles);

		return exampleSet;

		// } catch (Exception e) {
		// throw new UserError(null, e,
		// "error.octave.data_translation_error.import.data.wrongsimportcell",
		// cellName,
		// ExampleSet.class.getSimpleName());
		// }

	}

	/**
	 * Creates an exampleSet from an example table and the role names
	 * 
	 * @param exampleTable
	 * @param attributeRoles
	 * @return
	 */
	private ExampleSet createRapidminerExampleSet(
			MemoryExampleTable exampleTable, String[] attributeRoles) {
		ExampleSet exampleSet = exampleTable.createExampleSet();

		// we need to clone because otherwise there is a concurrent access
		// exception
		ExampleSet clone = (ExampleSet) exampleSet.copy();

		Iterator<Attribute> itt = exampleSet.getAttributes().iterator();
		int numRole = 0;
		while (itt.hasNext()) {
			Attribute dd = itt.next();
			if (!(attributeRoles[numRole]).equals("")) {
				// declare this special attribute
				clone.getAttributes().setSpecialAttribute(dd,
						attributeRoles[numRole]);
			}
			numRole++;
		}
		return clone;
	}

	// ------------- export functions --------------------

	/**
	 * The main method called by OctaveTranslator to export from Rapidminer to
	 * Octave
	 * 
	 * @see com.rapidminer.tools.octave.translation.OctaveTranslator#exportObject(dk.ange.octave.OctaveEngine,
	 *      java.lang.String, com.rapidminer.operator.IOObject,
	 *      java.lang.String)
	 */
	@Override
	public void exportObject(OctaveEngineProxy octaveEngine, String expression,
			IOObject ioObject, String getParameter, Object... parameters) throws OperatorException {

		double dateRoundingDigits = ((Double) parameters[0]).doubleValue();
		
		ExampleSet exampleSet = (ExampleSet) ioObject;
		Attributes attributes = exampleSet.getAttributes();

		int numberOfExamples = exampleSet.size();
		int numberOfRegularAttributes = attributes.size();
		int numberOfAttributesAll = attributes.allSize();

		// creating array of attributes to regard
		Attribute[] allAttributesInclSpecial = new Attribute[numberOfAttributesAll];

		/* Building full size column arrays to avoid translation of indices... */
		int[][] encodedNominalData = new int[numberOfAttributesAll][];
		String[][] nominalDataValues = new String[numberOfAttributesAll][];
		double[][] numericalData = new double[numberOfAttributesAll][];

		// Create a copy of Regular attributes descriptions
		int i = 0;
		for (Attribute attribute : attributes) {
			allAttributesInclSpecial[i] = attribute;
			i++;
		}

		// Create a copy of Special attributes descriptions
		Iterator<AttributeRole> it = attributes.specialAttributes();
		int index = numberOfRegularAttributes;
		while (it.hasNext()) {
			AttributeRole attrole = it.next();
			allAttributesInclSpecial[index] = attrole.getAttribute();
			index++;
		}

		// initialize the data structures that will receive the data.
		// and use also this opportunity to copy the "mapping" or "levels" of
		// nominal data
		i = 0;
		for (Attribute attribute : allAttributesInclSpecial) {

			// all cases: create the structures
			encodedNominalData[i] = new int[numberOfExamples];
			numericalData[i] = new double[numberOfExamples];

			// special data types
			if (attribute.isNominal()) {

				// fill the arrays containing the nominal data levels
				// because here we have the opportunity: we have the attribute
				NominalMapping mapping = attribute.getMapping();
				nominalDataValues[i] = new String[mapping.size()];
				for (int j = 0; j < mapping.size(); j++) {
					nominalDataValues[i][j] = mapping.mapIndex(j);
				}

			} else if (attribute.isDateTime()) {

				// date is like numerical : dont do any additional thing
			}
			i++;
		}

		// copy data for each example in the set
		int exampleIndex = 0;
		for (Example example : exampleSet) {
			int attributeIndex = 0;
			for (Attribute attribute : allAttributesInclSpecial) {

				double value = example.getValue(attribute);
				if (attribute.isNumerical()) {

					// NUMERICAL - copy data directly
					numericalData[attributeIndex][exampleIndex] = value;

				} else if (attribute.isNominal()) {

					// NOMINAL - copy data directly because it is an index using
					// the mapping
					int levelEncoded = (int) value;

					// for the cell encoding, we need to keep the encoded data
					// apart
					encodedNominalData[attributeIndex][exampleIndex] = levelEncoded + 1;
					// for the struct encoding, we need to mix the encoded data
					// with other data.
					numericalData[attributeIndex][exampleIndex] = levelEncoded + 1;

				} else if (/* date time */Ontology.ATTRIBUTE_VALUE_TYPE.isA(
						attribute.getValueType(), Ontology.DATE_TIME)) {

					// DATE_TIME can NOT be considered like a nominal. It is a
					// double value (see DateAttribute) which counts the nb of
					// milliseconds since January 1, 1970, 00:00:00 GMT.

					// we now fix the translation
					// numericalData[attributeIndex][exampleIndex] = value;
					numericalData[attributeIndex][exampleIndex] = convertJavaDateToOctave(value);

				}
				attributeIndex++;
			}
			exampleIndex++;
		}

		// if parameter says structure, create a structure. if cell, create a
		// cell.
		OctaveObject o = null;
		if (getParameter.equals("cell")) {
			o = writeAsCell(allAttributesInclSpecial, attributes,
					numberOfExamples, numericalData, encodedNominalData);
		} else if (getParameter.equals("struct")) {
			o = writeAsStruct(allAttributesInclSpecial, attributes,
					numberOfExamples, numericalData);
		} else {
			throw new UserError(
					null,
					"octave.data_translation_error.import.data.wrongswrittendata",
					"Errors : the type of exemple set outputted is "
							+ getParameter + " which is not supported");
		}

		// put in octave
		octaveEngine.put(expression, o);
	}

	/**
	 * create ExempleSet like the OctaveStruct data
	 * 
	 * @param allAttributesInclSpecial
	 * @param attributes
	 * @param numberOfExamples
	 * @param numericalDataColumns
	 * @return
	 * @throws UserError
	 */
	private OctaveObject writeAsStruct(Attribute[] allAttributesInclSpecial,
			Attributes attributes, int numberOfExamples,
			double[][] numericalDataColumns) throws UserError {

		// attributeNames: used for the loop
		// copyAttributes: used inside the loop to get att
		// numberOfAttributesAll : is it needed ?

		// create names and roles
		int nbNominal = 0;
		int numberOfAttributesAll = allAttributesInclSpecial.length;
		OctaveCell cellNames = new OctaveCell(1, numberOfAttributesAll);
		OctaveCell cellRoles = new OctaveCell(1, numberOfAttributesAll);
		OctaveCell cellLevels = new OctaveCell(1, numberOfAttributesAll);

		/* For each attribute, fill its name, role, */
		int j = 0;
		for (Attribute att : allAttributesInclSpecial) {

			cellNames.set(new OctaveString(att.getName()), 1, j + 1);
			// get role type id ,label, weight, cluster,prediction,batch
			String role = attributes.findRoleByName(
					allAttributesInclSpecial[j].getName()).getSpecialName();

			// NEW encode datetime as a special role in octave
			if (att.isDateTime()) {
				if (role != null && !role.isEmpty()) {
					throw new UserError(
							null,
							"octave.data_translation_error.export.data.datetimewithrole",
							att.getName(), role);
				} else {
					role = OCTAVE_DATETIME_ROLE;
				}
			}
			cellRoles.set(new OctaveString(role == null ? "" : role), 1, j + 1);

			if (allAttributesInclSpecial[j].isNominal()) {

				int attributeNbLevels = attributes
						.get(allAttributesInclSpecial[j].getName())
						.getMapping().getValues().size();
				OctaveCell attributeLevels = new OctaveCell(attributeNbLevels,
						1);

				// copy all levels into a vertical cell, and put it in the
				// horizontal cell of levels
				for (int k = 0; k < attributeNbLevels; k++) {
					attributeLevels
							.set(new OctaveString(attributes
									.get(allAttributesInclSpecial[j].getName())
									.getMapping().getValues().get(k)), k + 1, 1);
				}
				cellLevels.set(attributeLevels, 1, j + 1);
				nbNominal++;

			} else {
				// the levels entry is empty
			}
			j++;

		}
		/*
		 * concatenate all data columns in the same array because JavaOctave
		 * needs it
		 */
		double[] allData = new double[numberOfExamples * numberOfAttributesAll];
		for (int n = 0; n < numberOfAttributesAll; n++) {
			System.arraycopy(numericalDataColumns[n], 0, allData, n
					* numberOfExamples, numberOfExamples);
		}

		/* Finally create the struct */
		final Map<String, OctaveObject> struct = new HashMap<String, OctaveObject>();
		struct.put("name", cellNames);
		struct.put("role", cellRoles);
		if (nbNominal != 0) {
			struct.put("levelname", cellLevels);
		}
		struct.put("data", new OctaveDouble(allData, numberOfExamples,
				numberOfAttributesAll));

		return new OctaveStruct(struct);
	}

	// create ExempleSet like the OctaveCell data

	/**
	 * @param attributeNames
	 * @param copyAttributes
	 * @param exampleSet
	 * @param attributes
	 * @param numberOfExamples
	 * @param numberOfAttributesAll
	 * @param numericalData
	 * @param nominalData
	 * @return
	 * @throws UserError
	 */
	private OctaveObject writeAsCell(Attribute[] allAttributesInclSpecial,
			Attributes attributes, int numberOfExamples,
			double[][] numericalData, int[][] nominalData) throws UserError {

		// create the output cell
		OctaveCell cellData = new OctaveCell(3, allAttributesInclSpecial.length);

		int j = 0;
		for (Attribute att : allAttributesInclSpecial) {

			// name
			cellData.set(new OctaveString(att.getName()), 1, j + 1);

			// role
			String role = attributes.findRoleByName(att.getName())
					.getSpecialName();
			// NEW encode datetime as a special role in octave
			if (att.isDateTime()) {
				if (role != null && !role.isEmpty()) {
					throw new UserError(
							null,
							"octave.data_translation_error.export.data.datetimewithrole",
							att.getName(), role);
				} else {
					role = OCTAVE_DATETIME_ROLE;
				}
			}
			cellData.set(new OctaveString(role == null ? "" : role), 2, j + 1);

			// if (role == null) {
			if (att.isNominal()) {

				// create is a vertical cell
				OctaveCell attributeLevels = new OctaveCell(numberOfExamples, 1);
				for (int k = 0; k < numberOfExamples; k++) {
					// TODO: since numericalData should contain the nominal data
					// encoding, we could use numericalData in place of
					// nominalData
					attributeLevels.set(new OctaveString(att.getMapping()
							.getValues().get(nominalData[j][k] - 1)), k + 1, 1);
				}
				cellData.set(attributeLevels, 3, j + 1);

			} else {
				// create a vertical array
				OctaveDouble datatest = new OctaveDouble(numericalData[j],
						numberOfExamples, 1);
				cellData.set(datatest, 3, j + 1);
			}
			j++;

		}
		return cellData;
	}

	@Override
	public void setDateTimeZone(TimeZone param_timeZone) {
		timeZone = param_timeZone;
	}

	public double convertOctaveDateToJava(double d, double dateRoundingDigits) {
		// MATLAB dates are doubles. They are therefore truncated (floored)
		// so we need to re-round upwards. By default 10e9 seems to work
		double date = (double) Math.ceil(d * Math.pow(10, dateRoundingDigits ))
				/ Math.pow(10, dateRoundingDigits);
		date = (date - OCTAVEDATE_1_JAN_1970) * (24 * 60 * 60 * 1000);
		return date - timeZone.getOffset((long) date);
	}

	public double convertJavaDateToOctave(double d) {
		return ((d + timeZone.getOffset((long) d)) / (24 * 60 * 60 * 1000))
				+ OCTAVEDATE_1_JAN_1970;
	}

}

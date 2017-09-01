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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;

import dk.ange.octave.exception.OctaveClassCastException;
import dk.ange.octave.type.OctaveCell;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveString;
import dk.ange.octave.type.matrix.AbstractGenericMatrix;

/**
 * A helper class
 * 
 * @author Sylvain Marié
 * @author Yaoyu Zhang
 */
public class CellImportHelper {

	private OctaveCell dataExpCell;
	private String globalCellName;
	private int nbAttributes;
	private Integer attributesLength;

	/**
	 * Contructor. Checks the validity of the format first
	 * 
	 * @param dataExpCell
	 * @param cellName
	 * @throws UserError
	 */
	public CellImportHelper(OctaveCell dataExpCell, String cellName) throws UserError {
		this.dataExpCell = dataExpCell;
		this.globalCellName = cellName;

		nbAttributes = checkCell(dataExpCell, cellName);
	}

	/**
	 * A method to check that the cell is well-formed.
	 * 
	 * @param dataExpCell
	 * @param cellName
	 * @return
	 * @throws UserError
	 */
	public static int checkCell(OctaveCell dataExpCell, String cellName) throws UserError {

		int[] size = dataExpCell.getSize();
		if (size.length < 2)
			throw new UserError(null, "octave.data_translation_error.import.cell.wrongsize", cellName);

		int nbRows = size[0];
		if (nbRows != 3)
			throw new UserError(null, "octave.data_translation_error.import.cell.wrongnbrows", cellName);

		return size[1];
	}

	/**
	 * Extracts the attribute names from the cell
	 * 
	 * @return
	 * @throws UserError
	 */
	public String[] extractAttributeNames() throws UserError {
		try {
			String[] attributeNames = new String[nbAttributes];

			for (int j = 0; j < nbAttributes; j++) {
				attributeNames[j] = dataExpCell.get(OctaveString.class, 1, j + 1).getString();
			}
			return attributeNames;
		} catch (OctaveClassCastException e) {
			throw new UserError(null, "octave.data_translation_error.import.cell.wrongcellname", globalCellName);
		}
	}

	/**
	 * Extracts the attribute roles from the cell
	 * 
	 * @return
	 * @throws UserError
	 */
	public String[] extractAttributeRoles() throws UserError {

		try {
			String[] attributeRoles = new String[nbAttributes];
			for (int j = 0; j < nbAttributes; j++) {
				attributeRoles[j] = dataExpCell.get(OctaveString.class, 2, j + 1).getString();
			}
			return attributeRoles;
		} catch (OctaveClassCastException e) {
			throw new UserError(null, "octave.data_translation_error.import.cell.wrongcellrole", globalCellName);
		}
	}

	/**
	 * Creates the Rapidminer attribute descriptions for the given cell content
	 * 
	 * @param attributeNames
	 * @param dataExpCell
	 * @return
	 * @throws UserError
	 */
	public Attribute[] createRMAttributeDescriptions(String[] attributeNames) throws UserError {

		Attribute[] attributes = new Attribute[nbAttributes];

		// for each entry in the 3d row, create the Rapidminer "Attribute"
		// description
		OctaveObject attributeValues;
		for (int i = 0; i < nbAttributes; i++) {
			// name
			String attname = (attributeNames[i] != null) ? attributeNames[i] : ("att" + i);

			// third row corresponding content
			attributeValues = dataExpCell.get(3, i + 1);
			if (attributeValues != null) {
				if (attributeValues instanceof OctaveDouble) {
					// numerical attribute
					checkAttributeSize((OctaveDouble) attributeValues, attname);
					attributes[i] = AttributeFactory.createAttribute(attname, Ontology.NUMERICAL);
				} else if (attributeValues instanceof OctaveCell) {
					// nominal attribute
					checkAttributeSize((OctaveCell) attributeValues, attname);
					attributes[i] = getNominalAttributeCell(attname, (OctaveCell) attributeValues);
				} else
					throw new UserError(null,
							"octave.data_translation_error.import.cell.wrongcelldata", attributeValues.getClass().getName());
			} else
				throw new UserError(null, "octave.data_translation_error.import.cell.wrongcelldataempty",globalCellName);

		}
		return attributes;
	}

	/**
	 * @param attributeValues
	 * @param attributeName
	 * @throws UserError
	 */
	private void checkAttributeSize(AbstractGenericMatrix mat, String attributeName) throws UserError {

		int n = mat.getSize()[0];
		int m = mat.getSize()[1];

		if (m != 1)
			throw new UserError(null, "octave.data_translation_error.import.cell.wrongcellattributesizecolumn",attributeName, m);

		if (attributesLength == null) {
			// save the length of the first attribute for future reference
			attributesLength = new Integer(n);

		} else if (n != attributesLength.intValue()) {
			throw new UserError(
					null,
					"octave.data_translation_error.import.cell.wrongcellattributerowsize",attributeName,n);
		}
	}

	/**
	 * Builds a Rapidminer nominal attribute from the attributes's content. A
	 * dictionary of levels is created on-the-fly from the content.
	 * 
	 * @param attName
	 * @param dataOrLevels
	 * @return
	 * @throws UserError
	 */

	public Attribute getNominalAttributeCell(String attName, OctaveCell dataOrLevels) throws UserError {

		if (dataOrLevels != null) {

			Attribute attribute = AttributeFactory.createAttribute(attName, Ontology.NOMINAL);

			// first run : extract all distinct values from the list
			OctaveObject level;
			for (int i = 1; i < dataOrLevels.size(1); i++) {

				level = dataOrLevels.get(i, 1);
				if (level != null) {
					if (level instanceof OctaveString) {
						attribute.getMapping().mapString(((OctaveString) level).getString());
					} else if (level instanceof OctaveDouble
							&& (((OctaveDouble) level).getData().length == 1)) {
						attribute.getMapping()
								.mapString(Double.toString(((OctaveDouble) level).getData()[0]));
					} else {
						// unsupported value for the level
						throw new UserError(null,
								"octave.data_translation_error.import.cell.wrongcelllevelvalue",
								attName ,level.getClass().getName());
					}
				} else {
					throw new UserError(null, "octave.data_translation_error.import.wrongcelllevelnull",
							attName);
				}
			}
			return attribute;

		}
		throw new UserError(null, "octave.data_translation_error.import.wrongcelllevelmiss",
				 attName);
	}

	/**
	 * Creates a RapidMiner Example table from the content provided
	 * 
	 * @param attributes
	 * @param dataExpCell
	 * @return
	 * @throws UserError 
	 */
	public MemoryExampleTable createRMexampleTable(Attribute[] attributes) throws UserError {

		// creating memory example table
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

		double[][] dataValues = new double[nbAttributes][];
		int numberOfExamples = -1;

		for (int column = 0; column < nbAttributes; column++) {
			if (attributes[column].isNominal()) {
				// (A) nominal attribute
				// no need to check the type, already done
				OctaveCell attributeValues = (OctaveCell) dataExpCell.get(3, column + 1);

				// convert the values to numbers according to the levels mapping
				dataValues[column] = getLevelIdsFromMapping(attributes[column], attributeValues);

			} else {
				// (B) numerical attribute : copy directly
				OctaveDouble doubleVector = (OctaveDouble) dataExpCell.get(3, column + 1);
				dataValues[column] = doubleVector.getData();
			}

			// this should have already been checked previously in
			// createRMAttributeDescriptions
			if (numberOfExamples > -1 && numberOfExamples != dataValues[column].length)
				throw new UserError(null, "octave.data_translation_error.cell.wrongcelllexamplesetsize", globalCellName);
			numberOfExamples = dataValues[column].length;
		}

		// add rows to example set (need to transpose dataValues)
		for (int row = 0; row < numberOfExamples; row++) {
			double[] dataRowData = new double[attributes.length];
			for (int column = 0; column < attributes.length; column++){
				dataRowData[column] = dataValues[column][row];
			}
			DataRow dataRow = new DoubleArrayDataRow(dataRowData);
			exampleTable.addDataRow(dataRow);
		}
		return exampleTable;
	}

	/**
	 * Loops through all values of this nominal attribute and encodes the values
	 * according to the attribute mappings
	 * 
	 * @param attribute
	 * @param attributeValues
	 * @return
	 * @throws UserError
	 */
	private double[] getLevelIdsFromMapping(Attribute attribute, OctaveCell attributeValues) throws UserError {

		double[] levelIds = new double[attributeValues.size(1)];
		for (int row = 0; row < attributeValues.size(1); row++) {
			OctaveObject level = attributeValues.get(row + 1, 1);

			if (level != null) {
				if (level instanceof OctaveString) {
					levelIds[row] = attribute.getMapping().mapString(((OctaveString) level).getString());
				} else if (level instanceof OctaveDouble && (((OctaveDouble) level).getData().length == 1)) {
					levelIds[row] = attribute.getMapping().mapString(
							Double.toString(((OctaveDouble) level).getData()[0]));
				} else {
					// unsupported value for the level
					throw new UserError(null,
							"octave.data_translation_error.import.cell.wrongcelllevelvalue",
									attribute.getName(),level.getClass().getName());
				}
			} else {
				throw new UserError(null, "octave.data_translation_error.import.cell.wrongcelllevelnull",
						attribute.getName());
			}
		}
		return levelIds;
	}

}

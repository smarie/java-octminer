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
import dk.ange.octave.type.OctaveStruct;
import dk.ange.octave.type.matrix.AbstractGenericMatrix;

/**
 * A helper static class for struct data import from octave to rapidminer
 * 
 */
public class StructImportHelper {

	public static final String ROLE = "role";
	public static final String NAME = "name";
	public static final String DATA = "data";
	public static final String LEVELNAME = "levelname";

	/**
	 * Checks that the given octave object contains the attribute names in the
	 * correct format
	 * 
	 * @param names
	 * @param structName
	 * @return
	 * @throws UserError
	 */
	public static OctaveCell checkStructAttributeNames(OctaveStruct struct, String structName)
			throws UserError {

		OctaveObject names = struct.get(NAME);
		OctaveCell namecell;
		if (names instanceof OctaveCell) {
			namecell = (OctaveCell) names;
			int[] size = namecell.getSize();
			int nbRows = size[0];
			if (nbRows != 1)
				throw new UserError(null, "oerror.octave.data_translation_error.import.struct.wrongstructnamesize", structName,
						"the name field should be a 1xn cell");
			else
				return namecell;
		} else {
			throw new UserError(null, "error.octave.data_translation_error.import.struct.wrongstructname", structName,
					"the name field was not a cell");
		}
	}

	/**
	 * Checks that the given octave object contains the attribute roles in the
	 * correct format
	 * 
	 * @param roleObject
	 * @param structName
	 * @return
	 * @throws UserError
	 */
	public static OctaveCell checkStructAttributeRoles(OctaveStruct struct, String structName, int nbCols)
			throws UserError {

		OctaveObject roleObject = struct.get(ROLE);
		OctaveCell rolecell;
		if (roleObject instanceof OctaveCell) {
			rolecell = (OctaveCell) roleObject;
			int[] size = rolecell.getSize();
			if (size[1] != nbCols || size[0] != 1)
				throw new UserError(null, "octave.data_translation_error.import", structName,
						"the role field should be a 1xn cell with same n than the name field");
			else
				return rolecell;
		} else {
			throw new UserError(null, "octave.data_translation_error.import", structName,
					"the role field was not a cell");
		}
	}

	/**
	 * Checks that the given octave object contains the attributes' data in the
	 * correct format
	 * 
	 * @param dataObject
	 * @param structName
	 * @param nbCols
	 * @return
	 * @throws UserError
	 */
	public static OctaveDouble checkStructAttributesData(OctaveStruct struct, String structName, int nbCols)
			throws UserError {
	
		OctaveObject dataObject = struct.get(DATA);
		OctaveDouble dataMatrix;
		if (dataObject instanceof OctaveDouble) {
			dataMatrix = (OctaveDouble) dataObject;
			int[] size = dataMatrix.getSize();
			

			
				if (dataMatrix.getSize()[1] != nbCols)
					throw new UserError(null, "octave.data_translation_error.import.wrongstructdatasize", structName,
							"the data matrix should be a mxn matrix with same n than the name (1xn) and role (1xn) cell fields");
				else
					return dataMatrix;
			
			
		} else {
			throw new UserError(null, "octave.data_translation_error.import.struct.wrongstructdata", structName,
					"the data field was not a matrix");
		}
	}

	/**
	 * Checks that the given octave object contains the attributes' levels in
	 * the correct format
	 * 
	 * @param levelnameObject
	 * @param structName
	 * @param nbCols
	 * @return
	 * @throws UserError
	 */
	public static OctaveCell checkStructNominalAttributeLevels(OctaveStruct struct, String structName,
			int nbCols) throws UserError {

		OctaveObject levelnameObject = struct.get(LEVELNAME);
		OctaveCell levelnameCell;
		if (levelnameObject != null) {
			if (levelnameObject instanceof OctaveCell) {

				levelnameCell = (OctaveCell) levelnameObject;
				int[] size = levelnameCell.getSize();

				if (size[0] != 1 || size[1] != nbCols)
					throw new UserError(null, "octave.data_translation_error.import.struct.wrongstructlevelnamesize", structName,
							"the levelname field should be a 1xn cell");
				else
					return levelnameCell;
			} else {
				throw new UserError(null, "octave.data_translation_error.import.struct.wrongstructlevelname", structName,
						"the levelname field should be a 1xn cell");
			}
		} else
			// no levels defined
			return null;
	}

	/**
	 * Creates attributeNames from namecell
	 * 
	 * @param namecell
	 * @return
	 * @throws UserError
	 */
	public static String[] extractAttributeNames(OctaveCell namecell, String structName) throws UserError {

		int nbAttributes = namecell.getSize()[1];
		String[] attributeNames = new String[nbAttributes];
		try {
			for (int j = 0; j < nbAttributes; j++) {
				attributeNames[j] = namecell.get(OctaveString.class, 1, j + 1).getString();
			}
			return attributeNames;
		} catch (OctaveClassCastException e) {
			throw new UserError(null, e, "octave.data_translation_error.import.struct.wrongstructnametype", structName,
					"the name field should be a 1xn cell with each member being a string");
		}
	}

	/**
	 * Creates attributeRoles from cell
	 * 
	 * @param rolecell
	 * @param structName
	 * @return
	 * @throws UserError
	 */
	public static String[] extractAttributeRoles(OctaveCell rolecell, String structName) throws UserError {
		int nbAttributes = rolecell.getSize()[1];
		String[] attributeRoles = new String[nbAttributes];
		try {
			for (int j = 0; j < nbAttributes; j++) {
				attributeRoles[j] = rolecell.get(OctaveString.class, 1, j + 1).getString();
			}
			return attributeRoles;
		} catch (OctaveClassCastException e) {
			throw new UserError(null, e, "octave.data_translation_error.import.struct.wrongstructroletype", structName,
					"the role field should be a 1xn cells with each member being a string");
		}
	}

	/**
	 * Computes the nb of special attributes (nb of attributes with role
	 * different from "")
	 * 
	 * @param attributeRoles
	 * @return
	 */
	public static int computeNbSpecialAttributes(String[] attributeRoles) {
		int nbAttributes = attributeRoles.length;
		int nbSpecialAttributes = 0;
		for (int j = 0; j < nbAttributes; j++) {
			if (!(attributeRoles[j]).equals("")) {
				nbSpecialAttributes++;
			}
		}
		return nbSpecialAttributes;
	}

	/**
	 * Creates the attribute descriptions in Rapidminer format, based on the
	 * given attribute names, roles, data, and levels
	 * 
	 * @param attributeNames
	 * @param dataMatrix
	 * @param levelsCell
	 * @return
	 * @throws UserError
	 */
	public static Attribute[] createRMAttributesDescriptionsForStructs(String[] attributeNames,
			OctaveDouble dataMatrix, OctaveCell levelsCell) throws UserError {

		int nbAttributes = attributeNames.length;
		Attribute[] attributes = new Attribute[nbAttributes];

		for (int j = 0; j < nbAttributes; j++) {

			// name
			String attname = (attributeNames[j] != null) ? attributeNames[j] : ("att" + j);

			// no need to test if special attribute
			// if ((attributeRoles[j]).equals("")) {

			if (levelsCell == null || levelsCell.get(1, j + 1) == null) {

				// >> a) no levels : numerical attribute
				attributes[j] = AttributeFactory.createAttribute(attname, Ontology.NUMERICAL);

			} else {
				if (levelsCell.get(1, j + 1) instanceof OctaveCell
						|| levelsCell.get(1, j + 1) instanceof OctaveDouble) {

					AbstractGenericMatrix attlevels = (AbstractGenericMatrix) levelsCell.get(1, j + 1);
					if (attlevels.getSize()[0] == 0) {

						// >> a) empty level: numerical attribute
						attributes[j] = AttributeFactory.createAttribute(attname, Ontology.NUMERICAL);

					} else {

						// >> b) provided level : nominal attribute
						attributes[j] = defineNominalAttributeWithLevels(attname, attlevels);

					}
				} else {
					// throw error : levels should all be octave cells even
					// empty ones ?
					throw new UserError(
							null,
							"octave.data_translation_error.import.struct.wrongstructlevelnametype",
							"Attribute "
									+ (j + 1)
									+ " levelname is not a cell - all levelname entries should be cells even the empty ones");
				}
			}
		}
		return attributes;

	}

	/**
	 * Used to define a Nominal Attribute by providing directly its levels.
	 * Levels can be provided as OctaveDouble of OctaveCell
	 * 
	 * @param attname
	 * @param attlevels
	 * @return
	 * @throws UserError
	 */
	public static Attribute defineNominalAttributeWithLevels(String attname, AbstractGenericMatrix attlevels)
			throws UserError {

		Attribute a = AttributeFactory.createAttribute(attname, Ontology.NOMINAL);

		// add all levels to the Attribute definition
		for (int i = 1; i <= attlevels.size(1); i++) {
			OctaveObject level;
			if (attlevels instanceof OctaveCell) {
				level = ((OctaveCell) attlevels).get(i, 1);
			} else if (attlevels instanceof OctaveDouble) {
				level = new OctaveDouble(new double[] { ((OctaveDouble) attlevels).get(i, 1) });
			} else {
				throw new UserError(null, "octave.data_translation_error.import.struct.wrongstructattributetype",
						"Attribute " + attname + " has a level entry of the wrong type");
			}

			if (level != null) {
				if (level instanceof OctaveString) {
					a.getMapping().mapString(((OctaveString) level).getString());
				} else if (level instanceof OctaveDouble && (((OctaveDouble) level).getData().length == 1)) {
					a.getMapping().mapString(Double.toString(((OctaveDouble) level).getData()[0]));
				} else {
					// unsupported value for the level
					throw new UserError(null,
							"octave.data_translation_error.import.struct.wrongstructattributevalues", "Attribute "
									+ attname + " has a level of unsupported type "
									+ level.getClass().getName());
				}
			} else {
				throw new UserError(null, "octave.data_translation_error.import.struct.wrongstructattributenull",
						"Attribute " + attname + " has a null level");
			}
		}
		return a;
	}

	/**
	 * Fills a newly created RapidMiner example table with the provided
	 * attributes and data matrix - for data it consists simply in reorganizing
	 * from a monodimensional array (OctaveDouble) to a temporary bidimensional
	 * array (dataValuesMatrix) and then to recreate DataRow objects with
	 * monodimensional arrays
	 * 
	 * @param attributes
	 * @param dataMatrix
	 */
	public static MemoryExampleTable createRMExampleTable(Attribute[] attributes, OctaveDouble dataMatrix) {

		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

		int[] sizeData = dataMatrix.getSize();
		int nbRowsD = sizeData[0];
		int nbColsD = sizeData[1];

		double[] dataValues = dataMatrix.getData();
		double[][] dataValuesMatrix = new double[nbRowsD][nbColsD];

		// fill dataValuesMatrix[][] with dataValues[]
		for (int row = 0; row < nbRowsD; row++) {
			for (int col = 0; col < nbColsD; col++) {
				if (attributes[col].isNominal()) {
					// the level indices shift from octave (1:...) to Rapidminer
					// (0:...)
					dataValuesMatrix[row][(col)] = dataValues[(row) + (col) * nbRowsD] - 1;
				} else {
					dataValuesMatrix[row][(col)] = dataValues[(row) + (col) * nbRowsD];
				}
			}
			// add new row
			DataRow dataRow = new DoubleArrayDataRow(dataValuesMatrix[row]);
			exampleTable.addDataRow(dataRow);
		}
		return exampleTable;
	}

}

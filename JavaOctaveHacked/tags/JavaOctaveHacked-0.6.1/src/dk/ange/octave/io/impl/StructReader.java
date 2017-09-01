/*
 * Copyright 2008, 2009 Ange Optimization ApS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.ange.octave.io.impl;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dk.ange.octave.exception.OctaveParseException;
import dk.ange.octave.io.OctaveIO;
import dk.ange.octave.io.spi.OctaveDataReader;
import dk.ange.octave.type.OctaveCell;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveStruct;

/**
 * The reader of struct.
 * 
 * Modified in a non-generic way to support the specific case of structures
 * converted to Rapidminer examplesets. This should be made more generic if
 * pushed to javaoctave.
 * 
 */
public final class StructReader extends OctaveDataReader {

	private final static CellReader cellReader = new CellReader();
	private final static MatrixReader matrixReader = new MatrixReader();
	private final static ScalarReader scalarReader = new ScalarReader();

	@Override
	public String octaveType() {
		return "scalar struct";
	}

	@Override
	public OctaveStruct read(final BufferedReader reader) {
		String line;

		line = OctaveIO.readerReadLine(reader);
		//
		// while(line != null){
		// System.out.println(line);
		// line = OctaveIO.readerReadLine(reader);
		// }
		//

		// # ndims: 1x1 struct for JavaOctave, ndims=2
		final String NDIMS = "# ndims: ";
		final int ndims = Integer.parseInt(line.substring(NDIMS.length()));

		// final String DMAX = " 1 1 ";
		line = OctaveIO.readerReadLine(reader);

		// # length: 4
		line = OctaveIO.readerReadLine(reader);

		final String LENGTH = "# length: ";
		if (!line.startsWith(LENGTH)) {
			throw new OctaveParseException("Expected '" + LENGTH + "' got '"
					+ line + "'");
		}
		final int length = Integer.valueOf(line.substring(LENGTH.length())); // only
																				// used
																				// during
																				// conversion

		final Map<String, OctaveObject> data = new HashMap<String, OctaveObject>();

		line = OctaveIO.readerReadLine(reader);
		for (int i = 0; i < length; i++) {

			// # name: <name,role or data>
			final String NAME = "# name: ";
			if (!line.startsWith(NAME)) {
				throw new OctaveParseException("Expected '" + NAME + "' got '"
						+ line + "'");
			}
			final String subname = line.substring(NAME.length());

			// check # type: cell or matrix
			final String CELL = "# type: cell";
			final String MATRIX = "# type: matrix";
			final String SCALAR = "# type: scalar";

			line = OctaveIO.readerReadLine(reader);

			if (("name".equalsIgnoreCase(subname) || "role"
					.equalsIgnoreCase(subname)) && line.equals(CELL)) {
				// line = OctaveIO.readerReadLine(reader);

				final OctaveCell cell = cellReader.read(reader);
				// new since the cellReader has read until next valid line, we
				// need to retrieve that one
				// or go back to before that line. Here is a solution with a
				// "go back"
				// try {
				// reader.reset();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				line = OctaveIO.readerReadLine(reader);

				if (cell.size(1) == 1) {
					// for(int j=1;j<=length;j++){
					// final OctaveObject value = cell.get(1, j);
					data.put(subname, cell);

					// }
				} else {
					throw new OctaveParseException(
							"JavaOctave does not support matrix structs, size="
									+ Arrays.toString(cell.getSize()));
				}

			} else if ("data".equalsIgnoreCase(subname)
					&& (line.equals(MATRIX) || line.equals(SCALAR))) {

				final OctaveDouble marix = matrixReader.read(reader);

				line = OctaveIO.readerReadLine(reader);

				if (marix.size(2) != 0) {

					data.put(subname, marix);

					// }
				} else {
					throw new OctaveParseException(
							"The matrix is not same as the data in structs, size="
									+ Arrays.toString(marix.getSize()));
				}

				// try {
				// reader.reset();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

			} else if ("data".equalsIgnoreCase(subname) && (line.equals(CELL))) {

				final OctaveCell cell = cellReader.read(reader);

				line = OctaveIO.readerReadLine(reader);

				if ((cell.size(1) != 1) || cell.size(2) != 1) {
					throw new OctaveParseException(
							"The cell containing the data should have dim 1");
				} else {

					data.put(subname, cell);
					// data.put(subname, cell.get(1,1));

					// }
				}

				// try {
				// reader.reset();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
			} else if ("data".equalsIgnoreCase(subname)
					&& (line.equals(SCALAR))) {

				final OctaveDouble scalar = scalarReader.read(reader);

				line = OctaveIO.readerReadLine(reader);

				if (scalar.size(1) == 1) {

					data.put(subname, scalar);

					// }
				} else {
					throw new OctaveParseException(
							"The data is not same as the data in structs, size="
									+ Arrays.toString(scalar.getSize()));
				}

				// try {
				// reader.reset();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
			} else {
				throw new OctaveParseException(
						"name and role should be cells, while data should be cell, got '"
								+ line + "'");
			}

		}

		return new OctaveStruct(data);
	}

}

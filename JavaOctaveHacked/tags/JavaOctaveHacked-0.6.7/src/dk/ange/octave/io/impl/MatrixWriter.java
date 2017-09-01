/*
 * Copyright 2008 Ange Optimization ApS
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
/**
 * @author Kim Hansen
 */
package dk.ange.octave.io.impl;

import java.io.IOException;
import java.io.Writer;

import dk.ange.octave.io.spi.OctaveDataWriter;
import dk.ange.octave.type.OctaveDouble;

/**
 * The writer of OctaveMatrix
 */
public final class MatrixWriter extends OctaveDataWriter<OctaveDouble> {

	@Override
	public Class<OctaveDouble> javaType() {
		return OctaveDouble.class;
	}

	@Override
	public void write(final Writer writer, final OctaveDouble octaveMatrix)
			throws IOException {
		if (octaveMatrix.getSize().length <= 2) {
			if (octaveMatrix.getSize().length == 2 && octaveMatrix.size(1) == 1
					&& octaveMatrix.size(2) == 1) {
				writer.write("# type: scalar\n");
				writer.write(octaveMatrix.get(1, 1) + "\n");
			} else {
				writer.write("# type: matrix\n");
				saveData2d(writer, octaveMatrix);
			}
		} else {
			writer.write("# type: matrix\n");
			saveDataVectorized(writer, octaveMatrix);
		}
	}

	private void saveData2d(final Writer writer, final OctaveDouble octaveMatrix) throws IOException {
        final int[] size = octaveMatrix.getSize();
        final double[] data = octaveMatrix.getData();
        final int nrows = size[0];
        final int ncols = size.length > 1 ? size[1] : 1;
        writer.write("# rows: " + nrows + "\n# columns: " + ncols + "\n");        
        
        for (int row = 0; row < nrows; row++) {
            for (int col = 0; col < ncols; col++) {
                writer.write(" " + data[row + col * nrows]);
            }
            writer.write('\n');

            // FIXED - issue OCTMINER-2: error: load: trouble reading ascii file `' 
            // https://java.net/jira/browse/OCTMINER-2
            // Solved it by flushing the data matrix after each row. Indeed 
            // I guess when it was too long it was flushing automatically, 
            // thus sometimes sending a minus character without the number behind it 
            writer.flush();
            // end FIX
        }
    }

	private void saveDataVectorized(final Writer writer,
			final OctaveDouble octaveMatrix) throws IOException {
		final int[] size = octaveMatrix.getSize();
		final double[] data = octaveMatrix.getData();
		writer.write("# ndims: " + size.length + "\n");
		for (final int sdim : size) {
			writer.write(" " + sdim);
		}
		for (final double d : data) {
			writer.write("\n " + d);
		}
		writer.write("\n");
	}

}
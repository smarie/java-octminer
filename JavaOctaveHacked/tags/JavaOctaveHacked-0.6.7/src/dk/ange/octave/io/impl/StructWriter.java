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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import dk.ange.octave.io.OctaveIO;
import dk.ange.octave.io.spi.OctaveDataWriter;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveStruct;

/**
 * The writer of OctaveStruct. Hacked because now Octave wants "scalar struct"
 * header and was modified for create our structe data, 
 * the data direct in the field of structe, not in the 1x1 cell ;
 * 
 */
 
public final class StructWriter extends OctaveDataWriter<OctaveStruct> {

	@Override
	public Class<OctaveStruct> javaType() {
		return OctaveStruct.class;
	}

	@Override
	public void write(final Writer writer, final OctaveStruct octaveStruct)
			throws IOException {
		final Map<String, OctaveObject> data = octaveStruct.getData();
		writer.write("# type: scalar struct\n# length: " + data.size() + "\n");
		for (final Map.Entry<String, OctaveObject> entry : data.entrySet()) {
			final String subname = entry.getKey();
			final OctaveObject value = entry.getValue();

			writer.write("# name: " + subname + "\n");
			//make the field in cell ,after put the data in it.
			
//			writer.write("# type: cell\n");
//			writer.write("# rows: 1\n");
//			writer.write("# columns: 1\n");
			OctaveIO.write(writer, "<cell-element>", value);
			writer.write("\n");
		}
	}

}

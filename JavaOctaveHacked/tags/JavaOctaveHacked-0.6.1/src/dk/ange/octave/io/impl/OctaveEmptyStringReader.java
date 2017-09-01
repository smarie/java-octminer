package dk.ange.octave.io.impl;

public class OctaveEmptyStringReader extends OctaveStringReader {

	@Override
    public String octaveType() {
        return "null_string";
    }
	
}

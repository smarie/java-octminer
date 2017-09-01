package dk.ange.octave.io.impl;

public class OctaveEmptySqStringReader extends OctaveStringReader {

	/**
	 * The reader of EmptyStringReader modified, if there is null element , 
	 * it can read them;
	 */
	@Override
    public String octaveType() {
        return "null_sq_string";
    }
	
}

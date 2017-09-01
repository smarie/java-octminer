package dk.ange.octave.io.impl;

public class OctaveEmptyStructReader extends OctaveStringReader {

	@Override
	/**
	 * The reader of EmptyStructReader was modified for ignore the line  ndims,
	 * that JavaOctave Can not read
	 */
    public String octaveType() {
        return "ndims";
    }
	
}



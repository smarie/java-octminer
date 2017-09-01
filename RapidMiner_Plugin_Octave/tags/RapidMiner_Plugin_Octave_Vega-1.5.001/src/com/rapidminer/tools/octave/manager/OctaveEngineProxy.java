package com.rapidminer.tools.octave.manager;

import com.rapidminer.operator.octave.OctaveScriptOperator;

import dk.ange.octave.type.OctaveObject;

public interface OctaveEngineProxy {

	public void eval(String script);

	public OctaveObject get(String expression);

	public void put(String expression, OctaveObject oc);
	
	public boolean isAvailable();

	public void shutdown();

	public String getName();

	public void addUser(OctaveScriptOperator octaveScriptOperator);

	public void releaseUser(OctaveScriptOperator octaveScriptOperator);

}

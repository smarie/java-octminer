/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.octave.translation;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;

import dk.ange.octave.OctaveEngine;

/**
 * This is the interface for all Translators, that can convert RapidMiner
 * objects to Octave and re-import Octave objects to RapidMiner.
 * 
 * @author Sebastian Land
 */
public interface OctaveTranslator<T extends IOObject> {

	/**
	 * This method should return the name this translation is presented to the
	 * user.
	 */
	public String getName();

	/**
	 * This method indicates if this translator can export an RM Object of the
	 * regarding class to Octave.
	 */
	public boolean supportsToOctave();

	/**
	 * This method indicates if this translator can import an RM Object of the
	 * regarding class from Octave.
	 */
	public boolean supportsFromOctave();

	/**
	 * This will return an IOObject created from the Octave object specified by
	 * the expression.
	 * 
	 * @throws OperatorException
	 */
	public T importObject(OctaveEngine octaveEngine, String expression)
			throws OperatorException;

	/**
	 * This will export an IOObject to the given Octave as an Octave object
	 * identified by the expression.
	 * 
	 * @throws OperatorException
	 */
	public void exportObject(OctaveEngine octaveEngine, String expression,
			IOObject ioObject , String getParameter) throws OperatorException;

	/**
	 * This method returns the subclass of IOObject this translator works for.
	 */
	public Class<T> getSupportedClass();
}

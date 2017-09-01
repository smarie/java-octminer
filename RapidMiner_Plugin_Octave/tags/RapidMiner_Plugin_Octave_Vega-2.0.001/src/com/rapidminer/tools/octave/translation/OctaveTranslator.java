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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;

/**
 * This is the interface for all Translators, that can convert RapidMiner
 * objects to Octave and re-import Octave objects to RapidMiner.
 * 
 * @author Sylvain Marié
 * @author Yaoyu Zhang
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
	public T importObject(OctaveEngineProxy octaveEngine, String objectName)
			throws OperatorException;

	/**
	 * This will export an IOObject to the given Octave as an Octave object
	 * identified by the expression.
	 * 
	 * @throws OperatorException
	 */
	public void exportObject(OctaveEngineProxy octaveEngine, String expression,
			IOObject ioObject , String getParameter) throws OperatorException;

	/**
	 * This method returns the subclass of IOObject this translator works for.
	 */
	public Class<T> getSupportedClass();
}

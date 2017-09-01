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
package com.rapidminer.operator.octave;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.ProcessingStep;
import com.rapidminer.tools.LoggingHandler;

/**
 * 
 * @author Sylvain Mari�
 * @author Yaoyu Zhang
 * 
 */
public class OctaveIOObject implements IOObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setSource(String sourceName) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void appendOperatorToHistory(Operator operator, OutputPort port) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ProcessingStep> getProcessingHistory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IOObject copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public LoggingHandler getLog() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLoggingHandler(LoggingHandler loggingHandler) {
		// TODO Auto-generated method stub

	}

	@Override
	public Annotations getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

}

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
package com.rapidminer.tools.octave.manager;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.operator.octave.OctaveScriptOperator;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.type.OctaveObject;

/**
 * This is a wrapper for Octave engine, so that
 * <ul>
 * <li>it logs correctly in log4j (The wrapper has its own log with the name of
 * the instance.)
 * <li>it can be called by multiple threads in parallel without compromising the
 * behavior. Since the internal engine is not thread safe this is done by
 * synchronizing all methods on the engine object.
 * <li>it maintains a list of operators that are currently using the engine.
 * This is useful to provide a "isAvailable" method, e.g. for load balancing in
 * the octave pool.
 * <ul>
 * 
 * @author Sylvain Marié
 */
public class ThreadSafeSimpleOctaveEngineProxy implements OctaveEngineProxy {

	private Log log;

	private OctaveEngine internalEngine;

	private String engineName;

	private Hashtable<String, OctaveScriptOperator> users = new Hashtable<String, OctaveScriptOperator>(
			10);

	/**
	 * Constructor
	 * 
	 * @param factory
	 * @param name
	 */
	public ThreadSafeSimpleOctaveEngineProxy(OctaveEngineFactory factory,
			String name) {

		log = LogFactory
				.getLog("com.rapidminer.operator.octave.ThreadSafeSimpleOctaveEngineProxy["
						+ engineName + "]");

		// 1. create the engine instance
		engineName = name;
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " starting...");
		internalEngine = factory.getScriptEngine();

		// 2. A writer that writes in log4j instead of sysout
		Writer writer = createLog4jWriter(engineName);
		internalEngine.setErrorWriter(writer);
		internalEngine.setWriter(writer);

		// 3. We force octave to start. This takes time but it will not be
		// needed again
		internalEngine
				.eval("disp(\"(" + engineName + ") Welcome To Octave\");");

		// 4. log the correct creation
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " is now ready");

	}

	public String getName() {
		return engineName;
	}

	// ************** OctaveEngine usual functions, thread-safe

	public void put(String expression, OctaveObject oc) {
		synchronized (internalEngine) {
			// log.info("Synchronized put - lock taken");
			internalEngine.put(expression, oc);
			// log.info("Synchronized put - releasing lock");
		}
	}

	public void eval(String script) {
		synchronized (internalEngine) {
			// log.info("Synchronized eval - lock taken");
			internalEngine.eval(script);
			// log.info("Synchronized eval - releasing lock");
		}
	}

	public OctaveObject get(String expression) {
		synchronized (internalEngine) {
			// log.info("Synchronized get - lock taken");
			OctaveObject ret = internalEngine.get(expression);
			// log.info("Synchronized get - releasing lock");
			return ret;
		}
	}

	public void shutdown() {
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " shutting down...");
		synchronized (internalEngine) {
			// does not work in multithread context.. TODO investigate
			// internalEngine.close();
			internalEngine.destroy();
		}
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " shut down successfully");

	}

	// ******** Operators management

	public void addUser(OctaveScriptOperator octaveScriptOperator) {
		users.put(octaveScriptOperator.getName(), octaveScriptOperator);
	}

	public void releaseUser(OctaveScriptOperator octaveScriptOperator) {
		users.remove(octaveScriptOperator.getName());
	}

	public boolean isAvailable() {
		return (users.size() == 0);
	}

	// ****** misc

	/**
	 * Creates A writer that writes in log4j instead of sysout
	 * 
	 * @return
	 */
	private Writer createLog4jWriter(String engineName) {

		final Logger log = Logger.getLogger("dk.ange.octave.OctaveEngine["
				+ engineName + "]");
		return new PrintWriter(System.out) {

			StringBuffer sb = new StringBuffer();

			@Override
			public void write(char[] buf, int off, int len) {
				// super.write(buf, off, len);
				sb.append(new String(buf, off, len));
			}

			@Override
			public void flush() {
				synchronized (sb) {
					log.info(sb.toString());
					sb = new StringBuffer();
				}
			}
		};
	}

}

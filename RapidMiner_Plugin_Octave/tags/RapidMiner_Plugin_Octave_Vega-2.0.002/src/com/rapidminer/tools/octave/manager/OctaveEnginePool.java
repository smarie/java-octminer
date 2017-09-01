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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.operator.UserError;
import com.rapidminer.operator.octave.OctaveScriptOperator;

import dk.ange.octave.OctaveEngineFactory;

/**
 * Represents a pool of octave engines. It creates as many thread-safe engines
 * as required, and provides a method to return an available engine when
 * requested (currently this "load-balancing" method is basic and returns the
 * first non-active engine if any, or the first engine of the pool otherwise).
 * The pool can also be resized dynamically.
 * 
 * @author Sylvain Marié
 */
public class OctaveEnginePool {

	Map<String, OctaveEngineProxy> engines;

	public static Log log = LogFactory
			.getLog("com.rapidminer.operator.octave.OctaveEnginePool");

	private OctaveEngineFactory factory;

	private int m_poolSize;

	/**
	 * Constructor for the pool
	 * 
	 * @param factory
	 * @param poolSize
	 */
	public OctaveEnginePool(OctaveEngineFactory factory, int poolSize) {
		engines = Collections
				.synchronizedMap(new HashMap<String, OctaveEngineProxy>(
						poolSize));
		this.factory = factory;

		// start the correct number of engines, in parallel :)
		ArrayList<Thread> threads = new ArrayList<Thread>(poolSize);
		for (int i = 0; i < poolSize; i++) {
			final String name = "[Octave engine " + (i + 1) + "]";
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					OctaveEngineProxy eng = createEngine(name);
					engines.put(name, eng);
				}
			}, name);
			threads.add(t);
			t.start();
		}

		// wait for all engines to be started
		Iterator<Thread> it = threads.iterator();
		while (it.hasNext()) {
			try {
				it.next().join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Started a pool of " + engines.size()
				+ " Octave engines successfully");
		m_poolSize = engines.size();
	}

	/**
	 * A reusable method to create a new multithread-safe octave engine and
	 * configure it correctly.
	 * 
	 * @param engineName
	 * @return
	 */
	private OctaveEngineProxy createEngine(String engineName) {
		return new ThreadSafeSimpleOctaveEngineProxy(factory, engineName);
	}

	/**
	 * Returns an engine to be used by the given operator. The operator will
	 * need to declare when it is done with the engine.
	 * <p>
	 * Currently this "load-balancing" method is basic and returns the first
	 * non-active engine if any, or the first engine of the pool otherwise
	 * 
	 * @param octaveScriptOperator
	 * @return
	 * @throws UserError 
	 */
	public OctaveEngineProxy getAvailableEngine(
			OctaveScriptOperator octaveScriptOperator) throws UserError {
		OctaveEngineProxy oct;

		// find an available engine if any
		Set<Entry<String, OctaveEngineProxy>> set = engines.entrySet();
		Iterator<Entry<String, OctaveEngineProxy>> it = set.iterator();
		while (it.hasNext()) {
			Entry<String, OctaveEngineProxy> e = it.next();
			oct = e.getValue();
			if (oct.isAvailable()) {
				oct.addUser(octaveScriptOperator);
				return oct;
			}
		}
		
		// check that the pool still has its size
		int size = engines.size();
		if (size < m_poolSize)
			resize(size);
		
		// else return the first one
		if(engines.values().iterator().hasNext()){
			oct = engines.values().iterator().next();
			oct.addUser(octaveScriptOperator);
			return oct;
		} else {
			// the pool is down !
			throw new UserError(null, "octave.octave_pool_error");
		}
	}

	/**
	 * Dynamically resize the pool
	 * 
	 * @param octaveNbEngines
	 */
	public void resize(int octaveNbEngines) {

		synchronized (engines) {

			// -------- resize up
			if (octaveNbEngines > engines.size()) {

				// we need to synchronize to change the size

				// start the correct number of additional engines
				int initialSize = engines.size();
				for (int i = 0; i < (octaveNbEngines - initialSize); i++) {
					String name = "[Octave engine " + (initialSize + i + 1)
							+ "]";
					OctaveEngineProxy eng = createEngine(name);
					engines.put(name, eng);
				}
				log.info("The pool has been extended to " + engines.size()
						+ " Octave engines successfully");

			}
			// -------- resize down
			else if (octaveNbEngines < engines.size()) {
				if (octaveNbEngines < 1)
					throw new IllegalArgumentException(
							"Size can not be less than one octave engine");

				// we need to synchronize to change the size

				while (octaveNbEngines < engines.size()) {
					OctaveEngineProxy p = engines.remove("[Octave engine "
							+ engines.size() + "]");
					p.shutdown();
				}

			} else {
				// nothing to do : same size
			}
			
			//update the actual pool size
			m_poolSize = engines.size();
		}
	}

	/**
	 * Shutdown all engines in the pool
	 */
	public void shutdown() {
		if (log.isInfoEnabled())
			log.info("Shutting down the Octave engine pool");

		synchronized (engines) {
			while (engines.size() > 0) {
				OctaveEngineProxy p = engines.remove("Octave engine "
						+ engines.size());
				p.shutdown();
			}
		}

		if (log.isInfoEnabled())
			log.info("Octave engine pool closed successfully");

	}

}

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

import com.rapidminer.operator.octave.OctaveScriptOperator;

import dk.ange.octave.OctaveEngineFactory;

/**
 * Represents a pool of octave engines. It creates as many thread-safe engines
 * as required, and provides a method to return an available engine when
 * requested. It can also be resized.
 * 
 */
public class OctaveEnginePool {

	Map<String, OctaveEngineProxy> engines;

	public static Log log = LogFactory
			.getLog("com.rapidminer.operator.octave.OctaveEnginePool");

	private OctaveEngineFactory factory;

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
	 * Returns an engin to be used by this operator. The operator will need to
	 * declare when it is done with the engine.
	 * 
	 * @param octaveScriptOperator
	 * @return
	 */
	public OctaveEngineProxy getAvailableEngine(
			OctaveScriptOperator octaveScriptOperator) {
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
		// else return the first one
		oct = engines.values().iterator().next();
		oct.addUser(octaveScriptOperator);
		return oct;
	}

	public void resize(int octaveNbEngines) {

		synchronized (engines) {

			// -------- resize up
			if (octaveNbEngines > engines.size()) {

				// we need to synchronize to change the size

				// start the correct number of additional engines
				int initialSize = engines.size();
				for (int i = 0; i < (octaveNbEngines - initialSize); i++) {
					String name = "[Octave engine " + (initialSize + i + 1) + "]";
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
		}
	}

	public void shutdown() {
		if (log.isInfoEnabled())
			log.info("Shutting down the Octave engine pool");
		
		synchronized(engines){
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

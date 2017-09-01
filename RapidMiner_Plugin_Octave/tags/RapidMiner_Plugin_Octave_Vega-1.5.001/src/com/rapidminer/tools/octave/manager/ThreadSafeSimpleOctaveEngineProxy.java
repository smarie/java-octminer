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
 * <li>it logs correctly in log4j
 * <li>it can be called by multiple threads in parallel without compromising the
 * behavior. Since the internal engine is not thread safe this is done by
 * synchronizing all methods on the engine object.
 * <ul>
 * 
 * The wrapper has its own log with the name of the instance.
 */
public class ThreadSafeSimpleOctaveEngineProxy implements OctaveEngineProxy {

	private Log log;

	private OctaveEngine internalEngine;

	private String engineName;

	private Hashtable<String, OctaveScriptOperator> users = new Hashtable<String, OctaveScriptOperator>(10);
	
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

	@Override
	public void put(String expression, OctaveObject oc) {
		synchronized (internalEngine) {
			// log.info("Synchronized put - lock taken");
			internalEngine.put(expression, oc);
			// log.info("Synchronized put - releasing lock");
		}
	}
	
	@Override
	public void eval(String script) {
		synchronized (internalEngine) {
			// log.info("Synchronized eval - lock taken");
			internalEngine.eval(script);
			// log.info("Synchronized eval - releasing lock");
		}
	}

	@Override
	public OctaveObject get(String expression) {
		synchronized (internalEngine) {
			// log.info("Synchronized get - lock taken");
			OctaveObject ret = internalEngine.get(expression);
			// log.info("Synchronized get - releasing lock");
			return ret;
		}
	}

	@Override
	public void addUser(OctaveScriptOperator octaveScriptOperator) {
		users.put(octaveScriptOperator.getName(), octaveScriptOperator);
	}
	
	

	@Override
	public void releaseUser(OctaveScriptOperator octaveScriptOperator) {
		users.remove(octaveScriptOperator.getName());
	}

	@Override
	public boolean isAvailable() {
		return (users.size() == 0);
	}

	@Override
	public void shutdown() {
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " shutting down...");
		synchronized(internalEngine){
			//does not work in multithread context.. TODO investigate
			//internalEngine.close();
			internalEngine.destroy();
		}
		if (log.isInfoEnabled())
			log.info("Octave engine " + engineName + " shut down successfully");

	}

	@Override
	public String getName() {
		return engineName;
	}

}

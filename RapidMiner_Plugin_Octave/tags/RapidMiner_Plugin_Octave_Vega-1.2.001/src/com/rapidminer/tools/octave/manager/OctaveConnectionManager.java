package com.rapidminer.tools.octave.manager;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.io.spi.OctaveDataReader;
import dk.ange.octave.io.spi.OctaveDataWriter;
import dk.ange.octave.type.OctaveComplex;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveString;
import dk.ange.octave.type.cast.Cast;

/**
 * This class manages the connection to Octave engines. In particular it ensures
 * that engine is reused.
 * 
 * @author HOMESAnalytics
 * 
 */
public class OctaveConnectionManager {

	public static Log log = LogFactory
			.getLog("com.rapidminer.operator.octave.OctaveConnectionManager");

	static {

		// setup the octave logger so that is has the rapidminer logger as a
		// parent
		Logger rmlogger = Logger.getLogger("com.rapidminer");
		Logger octlogger = Logger.getLogger("dk.ange.octave");
		octlogger.setParent(rmlogger);

		/*
		 * Unfortunately JavaOctave uses ServiceRegistry.lookupProviders in
		 * order to find its META-INF/services/... files. Since all threads are
		 * created by RapidMiner, they all have the RapidMiner's
		 * ContextClassloader, not the PluginClassLoader. In order to solve that
		 * problem, we create an artificial thread with the Plugin classLoader,
		 * and make sure it calls javaoctave before anything else calls it.
		 */
		Thread t = new Thread(new Runnable() {

			public Log tlog = LogFactory.getLog("com.rapidminer.operator.octave.OctaveConnectionManager#Thread");

			/**
			 * Here our context class loader will be
			 * OctaveConnectionManager.class.getClassLoader() (see below, we set
			 * the context class loader before running the thread)
			 * 
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			public void run() {

				// dk.ange.octave.io.spi.OctaveDataWriter
				OctaveDataWriter<OctaveString> a = OctaveDataWriter
						.getOctaveDataWriter(new OctaveString(""));
				if (a != null)
					tlog.info("Loaded OctaveDataWriter SPI with success");
				else
					tlog.info("Failed to load OctaveDataWriter SPI");

				// dk.ange.octave.io.spi.OctaveDataReader
				OctaveDataReader b = OctaveDataReader
						.getOctaveDataReader("cell");
				if (b != null)
					tlog.info("Loaded OctaveDataReader SPI with success");
				else
					tlog.info("Failed to load OctaveDataReader SPI");

				// dk.ange.octave.type.cast.Caster
				OctaveComplex c = Cast.cast(OctaveComplex.class,
						new OctaveDouble(new double[] { 1 }, 1, 1));
				if (c != null)
					tlog.info("Loaded Octave Caster SPI with success");
				else
					tlog.info("Failed to load Octave Caster SPI");
			}
		}, "OctaveConnectionManagerInitThread");

		// set its context class loader to the PluginCLassLoader
		t.setContextClassLoader(OctaveConnectionManager.class.getClassLoader());
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static OctaveEngineFactory factory = new OctaveEngineFactory();
	private static OctaveEngine sharedengine;

	/**
	 * Method used by the various operators in order to get an octave engine.
	 * The engine is configured the first time it is called, then kept in memory
	 * for future use. In particular we configure it so that the output is
	 * logged.
	 * 
	 * @return
	 */
	public static OctaveEngine getScriptEngine() {
		if (sharedengine == null) {
			log.info("First start of Octave engine...");
			sharedengine = factory.getScriptEngine();
			final Logger log = Logger.getLogger("dk.ange.octave.OctaveEngine");
			// A writer that writes in log4j instead of sysout
			Writer writer = new PrintWriter(System.out) {

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

			sharedengine.setErrorWriter(writer);
			sharedengine.setWriter(writer);
			
			// this takes time because octave will need to start.
			sharedengine.eval("disp(\"Welcome To Octave\");");

			log.info("Octave engine now ready");
		}
		return sharedengine;
	}

	/*
	 * Called when the object is destroyed
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {

		super.finalize();

		if (log.isInfoEnabled())
			log.info("now shutting down the Octave engine");

		sharedengine.close();

		if (log.isInfoEnabled())
			log.info("Octave engine closed successfully");
	}
}

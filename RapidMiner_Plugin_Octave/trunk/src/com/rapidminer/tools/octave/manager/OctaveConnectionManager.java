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

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.PluginInitOctaveExtension;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.octave.OctaveScriptOperator;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.octave.manager.pool.OctaveEngineJob;
import com.rapidminer.tools.octave.manager.pool.OctaveThreadPoolExecutor;
import com.rapidminer.tools.parameter.ParameterChangeListener;

import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.exception.OctaveException;
import dk.ange.octave.exec.OctaveExec;
import dk.ange.octave.io.spi.OctaveDataReader;
import dk.ange.octave.io.spi.OctaveDataWriter;
import dk.ange.octave.type.OctaveComplex;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveString;
import dk.ange.octave.type.cast.Cast;

/**
 * This class manages the connection to Octave engines. In particular it
 * <ul>
 * <li>ensures that the JavaOctave library is loaded by the proper Class Loader
 * <li>ensures that the library logs at the right place
 * <li>handles the configuration changes
 * <li>manages the pool of Octave engines so that OctaveOperators only have to
 * use {@link #getScriptEngine(OctaveScriptOperator)} to get an engine, without
 * knowing about the pool and the various thread-safe protections.
 * </ul>
 *
 * @author Sylvain Mari�
 *
 */
public class OctaveConnectionManager {

    /*
     * Write = time to send a command or data to octave.
     *
     * Read = time to execute a script or retrieve data from octave
     */
    public static final long OCTAVE_WRITE_TIMEOUT_DEFAULT_VALUE = 60;
    public static final long OCTAVE_READ_TIMEOUT_DEFAULT_VALUE = 120;

    public static Log log = LogFactory
            .getLog("com.rapidminer.operator.octave.OctaveConnectionManager");
    public static String[] CMD_ARRAY = { null, "--no-history",
            "--no-init-file", "--no-line-editing", "--no-site-file", "--silent" };

    public static OctaveEngineFactory factory = new OctaveEngineFactory();

    // in this class we have 1 instance only
    public static OctaveConnectionManager onlyInstance = new OctaveConnectionManager();

    // private OctaveEngineProxy threadSafeSharedEngine;
    private ConfigurationManager rmOctaveConfManager;

    private OctaveThreadPoolExecutor enginePool;

    /**
     * Constructor.
     * <ul>
     * <li>Sets the octave logger correctly
     * <li>Fixes the problem of loading the octave classes in the correct class
     * loader
     * <li>Registers a listener so that changes of parameters are handled
     * correctly.
     * </ul>
     *
     */
    private OctaveConnectionManager() {

        // setup the octave logger so that is has the rapidminer logger as a
        // parent
        Logger rmlogger = Logger.getLogger("com.rapidminer");
        Logger octlogger = Logger.getLogger("dk.ange.octave");

        // DOES NOT WORK ON Rapidanalytics (not allowed by JBOSS)
        // octlogger.setParent(rmlogger);
        Handler[] h = rmlogger.getHandlers();
        for (int i = 0; i < h.length; i++) {
            log.info("Adding log handler " + h[i].toString()
                    + " to octave logger " + octlogger.getName());
            octlogger.addHandler(h[i]);
        }

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

        // add the parameter listeners for the 4 parameters that matter to us
        rmOctaveConfManager = new ConfigurationManager();
        ParameterService.registerParameterChangeListener(rmOctaveConfManager);

        // TEMP to remove
        // OctaveEngine e =factory.getScriptEngine();
        // e.eval("A=0");
        // e.close();
        //
        // OctaveEngineProxy p = new ThreadSafeSimpleOctaveEngineProxy(factory,
        // "test");
        // p.shutdown();
        // System.out.println("closed engine successfully");

    }

    // /**
    // * Method used by the various operators in order to get an octave engine
    // * from the pool. The engine is created and configured the first time it
    // is
    // * called, then kept in memory for future use. In particular we configure
    // it
    // * so that the output is logged.
    // *
    // * This method is synchronized so that only ONE pool of engines can be
    // * created.
    // *
    // * Note: this method will wait until a free engine is available.
    // *
    // * @param octaveScriptOperator
    // *
    // * @return
    // * @throws UserError
    // */
    // public synchronized OctaveEngineProxy getScriptEngine(
    // OctaveScriptOperator octaveScriptOperator) throws UserError {
    // if (enginePool == null) {
    // log.info("First start of Octave engine(s)...");
    //
    // // retrieve the nb of engines to start
    // int poolSize = rmOctaveConfManager.getNbEngines();
    // enginePool = new OctaveEnginePool(factory, poolSize);
    // }
    // return enginePool.getAvailableEngine(octaveScriptOperator);
    // }

    /**
     * Synchronous (blocking) way to execute a task on the Octave engine pool.
     * Transforms the exceptions into the correct ones
     *
     * @param job
     * @throws OperatorException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void executeOctaveTaskSync(OctaveEngineJob job)
            throws OperatorException {
        checkPoolInited();
        Future<?> result = null;
        long maxTimeSeconds = rmOctaveConfManager.getOctaveReadTimeoutSeconds()
                + rmOctaveConfManager.getOctaveWriteTimeoutSeconds();
        try {
            result = enginePool.executeOctaveJob(job);
            // wait until completion
            result.get(maxTimeSeconds, TimeUnit.SECONDS);
            switch (job.getStatus()) {
            case EXECUTED_WITHOUT_ERROR:
                break;
            case EXECUTED_WITH_ERROR:
                Exception e = job.getLastException();
                if (e instanceof RuntimeException) {
                    throw ((RuntimeException) e);
                } else if (e instanceof OctaveException) {
                    throw ((OctaveException) e);
                } else if (e instanceof OperatorException) {
                    throw ((OperatorException) e);
                } else {
                    // this will never happen, by design of the
                    // OctaveEngineJob.doOctaveWork signature
                    throw new OperatorException(
                            "Error while executing octave task", e);
                }
            default:
                throw new OperatorException(
                        "Error executing Octave job : the status of the job is still "
                                + job.getStatus()
                                + " after having been executed by the pool");
            }
        } catch (InterruptedException e) {
            throw new OperatorException("Error executing Octave job", e);
        } catch (ExecutionException e) {
            throw new OperatorException("Error executing Octave job", e);
        } catch (TimeoutException e1) {
            // this will prevent the octave engine to restart > BAD !
            // if (result != null)
            // result.cancel(true);
            throw new OperatorException(
                    "Error executing Octave job, task timed out (maximum time allowed was "
                            + maxTimeSeconds + ")", e1);
        }
    }

    // /**
    // * Asynchronous (non-blocking) way to execute a task on the Octave engine
    // * pool. Use get() to wait for the task to complete.
    // *
    // * @param job
    // * @return
    // */
    // public Future<?> executeOctaveTaskAsync(OctaveEngineJob job) {
    // checkPoolInited();
    // return enginePool.executeOctaveJob(job);
    // }

    private void checkPoolInited() {
        if (enginePool == null) {
            synchronized (this) {
                // double check because the waiting thread should not recreate
                if (enginePool == null) {
                    log.info("First start of Octave engine(s)...");

                    // retrieve the nb of engines to start
                    int poolSize = rmOctaveConfManager.getNbEngines();
                    long readTO = rmOctaveConfManager
                            .getOctaveReadTimeoutSeconds();
                    long writeTO = rmOctaveConfManager
                            .getOctaveWriteTimeoutSeconds();
                    enginePool = new OctaveThreadPoolExecutor(poolSize, readTO,
                            writeTO);
                }
            }
        }
    }

    synchronized void resizePoolIfNeeded(int octaveNbEngines) {
        if (enginePool != null) {
            enginePool.resize(octaveNbEngines);
        }
    }

    synchronized void applyTimeouts(long octaveReadTimeoutSeconds,
            long octaveWriteTimeoutSeconds) {
        if (enginePool != null) {
            enginePool.applyTimeouts(octaveReadTimeoutSeconds,
                    octaveWriteTimeoutSeconds);
        }
    }

    /**
     * At creation time, will pick up the configuration value. When alerted from
     * configuration change, will apply the changes accordingly (for example
     * restart a pool of engines)
     *
     */
    public class ConfigurationManager implements ParameterChangeListener {

        private int octaveNbEngines = 2;
        private String startOptions;
        private String mFilePath;
        private long octaveReadTimeoutSeconds = OCTAVE_READ_TIMEOUT_DEFAULT_VALUE;
        private long octaveWriteTimeoutSeconds = OCTAVE_WRITE_TIMEOUT_DEFAULT_VALUE;

        /**
         * Constructor: retrieves the current parameters values
         */
        public ConfigurationManager() {

            // - parameters: options and default path
            String octaveoption = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_OPTIONS);
            String octavemfile = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_M_FILEPATH);
            String octaveNbEnginesS = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_NB_ENGINES);
            String octaveReadTimeoutS = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_READ_TIMEOUT_SECONDS);
            String octaveWriteTimeoutS = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_WRITE_TIMEOUT_SECONDS);

            configureNbEngines(octaveNbEnginesS);
            configureOctaveMFile(octavemfile);
            configureOctaveStartFlags(octaveoption);
            configureTimeouts(octaveReadTimeoutS, octaveWriteTimeoutS);
            applyOctaveStartOptionsConfig();

            // - parameter: octave exe
            String octavepath = ParameterService
                    .getParameterValue(PluginInitOctaveExtension.PROPERTY_OCTAVE_OCTAVEPATH);
            if (!octavepath.isEmpty()) {
                File octaveProgram = new File(octavepath);
                configureOctavePath(octaveProgram);
            }
        }

        /**
         * Set the Octave.ext path
         *
         */
        void configureOctavePath(File octaveProgram) {
            synchronized (factory) {
                factory.setOctaveProgram(octaveProgram);
                if (log.isInfoEnabled())
                    log.info("Octave executable path now changed to "
                            + octaveProgram
                            + ". (this applies to future engines to start)");
            }
        }

        void configureNbEngines(String octaveNbEnginesS) {
            synchronized (this) {
                // default value
                if (octaveNbEnginesS.isEmpty()) {
                    octaveNbEnginesS = "2";
                }

                // set value
                octaveNbEngines = Integer.parseInt(octaveNbEnginesS);

                if (log.isInfoEnabled())
                    log.info("Octave nb of concurrent engines changed to  "
                            + octaveNbEnginesS
                            + ". (this applies to future engines to start)");
            }
        }

        void configureTimeouts(String readTimeoutS, String writeTimeoutS) {
            synchronized (this) {
                if (readTimeoutS != null) {
                    // default value
                    if (readTimeoutS.isEmpty()) {
                        readTimeoutS = "" + OCTAVE_READ_TIMEOUT_DEFAULT_VALUE;
                    }

                    // set value
                    octaveReadTimeoutSeconds = Long.parseLong(readTimeoutS);

                    if (log.isInfoEnabled())
                        log.info("Octave read timeout (s) changed to  "
                                + readTimeoutS
                                + ". (this applies to future tasks to start)");
                }
                if (writeTimeoutS != null) {
                    // default value
                    if (writeTimeoutS.isEmpty()) {
                        writeTimeoutS = "" + OCTAVE_WRITE_TIMEOUT_DEFAULT_VALUE;
                    }

                    // set value
                    octaveWriteTimeoutSeconds = Long.parseLong(writeTimeoutS);

                    if (log.isInfoEnabled())
                        log.info("Octave write timeout (s) changed to  "
                                + writeTimeoutS
                                + ". (this applies to future tasks to start)");
                }
            }
        }

        void configureOctaveMFile(String mfilepath) {
            synchronized (this) {
                // default values
                if (mfilepath.isEmpty()) {
                    mfilepath = PluginInitOctaveExtension.pathToSupportFunctionsFolder;
                }
                // save the value
                mFilePath = mfilepath;
            }
        }

        void configureOctaveStartFlags(String octaveoption) {
            synchronized (this) {
                // save the values
                startOptions = octaveoption;
            }
        }

        public int getNbEngines() {
            return octaveNbEngines;
        }

        public long getOctaveReadTimeoutSeconds() {
            return octaveReadTimeoutSeconds;
        }

        public long getOctaveWriteTimeoutSeconds() {
            return octaveWriteTimeoutSeconds;
        }

        @Override
        public void informParameterChanged(String key, String value) {
            if (PluginInitOctaveExtension.PROPERTY_OCTAVE_OPTIONS.equals(key)) {
                configureOctaveStartFlags(value);
                applyOctaveStartOptionsConfig();
            } else if (PluginInitOctaveExtension.PROPERTY_OCTAVE_M_FILEPATH
                    .equals(key)) {
                configureOctaveMFile(value);
                applyOctaveStartOptionsConfig();
            } else if (PluginInitOctaveExtension.PROPERTY_OCTAVE_NB_ENGINES
                    .equals(key)) {
                configureNbEngines(value);
                applyNbEngineChange();
            } else if (PluginInitOctaveExtension.PROPERTY_OCTAVE_READ_TIMEOUT_SECONDS
                    .equals(key)) {
                configureTimeouts(value, null);
                applyTimeoutsChange();
            } else if (PluginInitOctaveExtension.PROPERTY_OCTAVE_WRITE_TIMEOUT_SECONDS
                    .equals(key)) {
                configureTimeouts(null, value);
                applyTimeoutsChange();
            }
        }

        @Override
        public void informParameterSaved() {
            // TODO Auto-generated method stub

        }

        /**
         * Changes the Octave CMD line with additional startup options or the
         * path of the .m files or the both of them ; Thread-safe.
         *
         */
        void applyOctaveStartOptionsConfig() {

            synchronized (OctaveExec.CMD_ARRAY) {

                /**
                 * Set the Startup option;
                 */
                if (mFilePath.isEmpty()) {
                    String[] octaveoptions = startOptions.split(",");
                    String[] cmdarray = new String[CMD_ARRAY.length
                            + octaveoptions.length];

                    System.arraycopy(CMD_ARRAY, 0, cmdarray, 0,
                            CMD_ARRAY.length);

                    for (int i = 0; i < octaveoptions.length; i++) {
                        cmdarray[i + CMD_ARRAY.length] = octaveoptions[i];
                    }
                    OctaveExec.CMD_ARRAY = cmdarray;

                } else if (startOptions.isEmpty()) {
                    /**
                     * Set the path of the .m files;
                     */
                    String[] cmdarray = new String[CMD_ARRAY.length + 2];
                    // cmdarray=CMD_ARRAY.clone();
                    System.arraycopy(CMD_ARRAY, 0, cmdarray, 0,
                            CMD_ARRAY.length);
                    // must exist in startup option
                    cmdarray[CMD_ARRAY.length] = "--path";
                    cmdarray[CMD_ARRAY.length + 1] = mFilePath;
                    OctaveExec.CMD_ARRAY = cmdarray;

                } else {
                    /**
                     * both configurations;
                     */
                    String[] octaveoptions = startOptions.split(",");
                    // String[] cmdcopy = octaveoptions;
                    String[] cmdarray = new String[CMD_ARRAY.length
                            + octaveoptions.length + 2];
                    System.arraycopy(CMD_ARRAY, 0, cmdarray, 0,
                            CMD_ARRAY.length);
                    for (int i = 0; i < octaveoptions.length; i++) {
                        cmdarray[CMD_ARRAY.length + i] = octaveoptions[i];
                    }
                    cmdarray[octaveoptions.length + octaveoptions.length] = "--path";
                    cmdarray[octaveoptions.length + octaveoptions.length + 1] = mFilePath;
                    OctaveExec.CMD_ARRAY = cmdarray;
                }

                if (log.isInfoEnabled())
                    log.info("Octave startup options now changed to "
                            + Arrays.toString(OctaveExec.CMD_ARRAY)
                            + ". (this applies to future engines to start)");
            }

        }

        void applyNbEngineChange() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    onlyInstance.resizePoolIfNeeded(octaveNbEngines);
                }
            }, "Octave pool size configuration applyer");

            t.start();
        }

        void applyTimeoutsChange() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    onlyInstance.applyTimeouts(octaveReadTimeoutSeconds,
                            octaveWriteTimeoutSeconds);
                }
            }, "Octave pool timeouts applyer");

            t.start();
        }

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

        enginePool.shutdown();
    }

    /**
     * A method to test that an engine can be created and shutdown correctly.
     *
     * @param engineName
     * @return
     */
    public void testCreateEngine(String engineName) {
        ThreadSafeSimpleOctaveEngineProxy engine = new ThreadSafeSimpleOctaveEngineProxy(
                factory, engineName);
        engine.shutdown();
    }
}

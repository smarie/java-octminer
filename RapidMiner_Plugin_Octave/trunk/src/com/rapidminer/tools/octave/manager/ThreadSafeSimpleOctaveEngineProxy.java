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
import dk.ange.octave.exception.OctaveNonrecoverableException;
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
 * <li>it restarts automatically when there was an
 * {@link OctaveNonrecoverableException} .
 * <ul>
 *
 * @author Sylvain Marié
 */
public class ThreadSafeSimpleOctaveEngineProxy implements OctaveEngineProxy {

    private Log log;

    // information to create or recreate the engine
    private String engineName;
    private OctaveEngineFactory engineFactory;

    // the engine to use
    private OctaveEngine internalEngine;

    // table listing the operators using this engine currently
    private Hashtable<String, OctaveScriptOperator> users = new Hashtable<String, OctaveScriptOperator>(
            10);

    private long readTimeout;

    private long writeTimeout;

    /**
     * Constructor
     *
     * @param factory
     * @param name
     * @param writeTimeOut
     * @param readTimeOut
     */
    public ThreadSafeSimpleOctaveEngineProxy(OctaveEngineFactory factory,
            String name) {
        this(factory, name, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructor
     *
     * @param factory
     * @param name
     * @param writeTimeOut
     * @param readTimeOut
     */
    public ThreadSafeSimpleOctaveEngineProxy(OctaveEngineFactory factory,
            String name, long readTimeOut, long writeTimeOut) {

        engineName = name;
        engineFactory = factory;

        log = LogFactory
                .getLog("com.rapidminer.operator.octave.ThreadSafeSimpleOctaveEngineProxy["
                        + engineName + "]");
        // we need to save them in case the engine breaks and we have to restart
        this.readTimeout = readTimeOut;
        this.writeTimeout = writeTimeOut;
        initInternalOctaveEngine();

    }

    /**
     * Internal utility method to init the internal engine.
     *
     * @param factory
     * @param name
     */
    private void initInternalOctaveEngine() {

        // 1. create the engine instance
        if (log.isInfoEnabled())
            log.info("Octave engine '" + engineName + "' (re)starting...");
        internalEngine = engineFactory.getScriptEngine();

        internalEngine.setOctaveTimeouts(writeTimeout, readTimeout);

        // 2. A writer that writes in log4j instead of sysout
        Writer writer = createLog4jWriter(engineName);
        internalEngine.setErrorWriter(writer);
        internalEngine.setWriter(writer);

        // 3. We force octave to start. This takes time but it will not be
        // needed again
        internalEngine
                .eval("disp(\"(" + engineName + ") Welcome To Octave\");");

        // check supported octave version
        String version = internalEngine.getVersion();
        if (log.isWarnEnabled() && !("3.6.1".equals(version)))
            log.warn("Octave version number is currently "
                    + version
                    + "! Only 3.6.1 is supported by this rapidminer extension and other versions might fail.");

        // 4. log the correct creation
        if (log.isInfoEnabled())
            log.info("Octave engine '" + engineName + "' version " + version
                    + " is now ready");
    }

    public String getName() {
        return engineName;
    }

    // ************** OctaveEngine usual functions, thread-safe

    @Override
    public void setOctaveTimeouts(long readTimeOut, long writeTimeOut) {
        internalEngine.setOctaveTimeouts(writeTimeOut, readTimeOut);
    }

    public void put(String expression, OctaveObject oc) {
        synchronized (internalEngine) {
            try {
                // log.info("Synchronized put - lock taken");
                internalEngine.put(expression, oc);
                // log.info("Synchronized put - releasing lock");
            } catch (OctaveNonrecoverableException e) {

                // shutdown - restart
                handleUnrecoverableError(e);

                // NEW : retry ONCE on the new engine
                try {
                    // log.info("Synchronized put - lock taken");
                    internalEngine.put(expression, oc);
                    // log.info("Synchronized put - releasing lock");
                } catch (OctaveNonrecoverableException e1) {

                    // this time throw e because it is very probable that the
                    // script is causing the destruction of the engine.
                    handleUnrecoverableError(e1);
                    throw e1;
                }
            }
        }
    }

    public void eval(String script) {
        synchronized (internalEngine) {
            try {
                // log.info("Synchronized eval - lock taken");
                internalEngine.eval(script);
                // log.info("Synchronized eval - releasing lock");
            } catch (OctaveNonrecoverableException e) {

                // shutdown - restart
                handleUnrecoverableError(e);

                // NEW : retry ONCE on the new engine
                try {
                    // log.info("Synchronized eval - lock taken");
                    internalEngine.eval(script);
                    // log.info("Synchronized eval - releasing lock");
                } catch (OctaveNonrecoverableException e1) {

                    // this time throw e because it is very probable that the
                    // script is causing the destruction of the engine.
                    handleUnrecoverableError(e1);
                    throw e1;
                }
            }
        }
    }

    public OctaveObject get(String expression) {
        synchronized (internalEngine) {
            try {
                // log.info("Synchronized get - lock taken");
                OctaveObject ret = internalEngine.get(expression);
                // log.info("Synchronized get - releasing lock");
                return ret;
            } catch (OctaveNonrecoverableException e) {

                // shutdown - restart
                handleUnrecoverableError(e);

                // NEW : retry ONCE on the new engine
                try {
                    // log.info("Synchronized get - lock taken");
                    OctaveObject ret = internalEngine.get(expression);
                    // log.info("Synchronized get - releasing lock");
                    return ret;
                } catch (OctaveNonrecoverableException e1) {

                    // this time throw e because it is very probable that the
                    // script is causing the destruction of the engine.
                    handleUnrecoverableError(e1);
                    throw e1;
                }
            }
        }
    }

    /**
     * Common code to handle properly non-recoverable errors of the octave
     * engine : a new engine will be restarted
     *
     * @param e
     */
    protected void handleUnrecoverableError(OctaveNonrecoverableException e)
 {
        log.error(
                "Unrecoverable error while running Octave script. I will recreate a new engine to replace this broken one",
                e);
        // shutdown engine
        shutdown();
        // recreate it
        initInternalOctaveEngine();
        // don't throw the exception
        // throw e;
    }

    public void shutdown() {
        if (log.isInfoEnabled())
            log.info("Octave engine " + engineName + " shutting down...");
        synchronized (internalEngine) {
            // does not work in multithread context.. TODO investigate
            // internalEngine.close();
            try {
                internalEngine.destroy();
            } catch (Exception e1) {
                // do nothing but log error
                log.error("Error while destroying octave engine " + engineName,
                        e1);
            }
        }
        if (log.isInfoEnabled())
            log.info("Octave engine " + engineName + " shut down successfully");
    }

    // ******** Operators management

    public synchronized void addUser(OctaveScriptOperator octaveScriptOperator) {
        users.put(octaveScriptOperator.getName(), octaveScriptOperator);
    }

    public synchronized void releaseUser(
            OctaveScriptOperator octaveScriptOperator) {
        users.remove(octaveScriptOperator.getName());
    }

    public synchronized boolean isAvailable() {
        return (users.size() == 0);
    }

    // ****** misc

    /**
     * Creates A writer that writes in log4j instead of sysout
     *
     * @return
     */
    private Writer createLog4jWriter(String engineName) {

        // apparently new versions of Rapidminer have a filter on the log
        // name - old "dk.ange.octave...." was filtered out. We now use the
        // package name (com.rapidminer....) and it works again.
        final Logger log = Logger
                .getLogger(ThreadSafeSimpleOctaveEngineProxy.class.getName()
                        + "[" + engineName + "]");
        // final Log log = LogFactory
        // .getLog(ThreadSafeSimpleOctaveEngineProxy.class.getName() + "["
        // + engineName + "]");
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

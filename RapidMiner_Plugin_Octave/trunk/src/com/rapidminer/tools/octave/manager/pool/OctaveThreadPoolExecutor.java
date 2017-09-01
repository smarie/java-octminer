package com.rapidminer.tools.octave.manager.pool;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.rapidminer.tools.octave.manager.OctaveConnectionManager;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;
import com.rapidminer.tools.octave.manager.ThreadSafeSimpleOctaveEngineProxy;

/**
 * A resizable pool of octave engines. We rely on the very good
 * {@link ThreadPoolExecutor} class from the jdk, but extend it in order to have
 * specific threads.
 *
 * @author Sylvain Marié
 *
 */
public class OctaveThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * The Octave engines are thread-locals, initialized from the factory with a
     * unique id.
     */
    public static final ThreadLocal<OctaveEngineProxy> octaveEngine = new ThreadLocal<OctaveEngineProxy>() {
        protected OctaveEngineProxy initialValue() {
            return null;
        };
    };
    private long octaveReadTimeoutSeconds;
    private long octaveWriteTimeoutSeconds;

    /**
     * Constructor with an initial poolsize and an octave engine factory
     *
     * @param factory
     * @param size
     * @param writeTO
     * @param readTO
     */
    public OctaveThreadPoolExecutor(int size, long octaveReadTimeoutSeconds,
            long octaveWriteTimeoutSeconds) {
        super(size, size, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new OctaveThreadFactory());
        this.octaveReadTimeoutSeconds = octaveReadTimeoutSeconds;
        this.octaveWriteTimeoutSeconds = octaveWriteTimeoutSeconds;
        // start all of them
        prestartAllCoreThreads();
    }

    public void resize(int octaveNbEngines) {
        setCorePoolSize(octaveNbEngines);
        setMaximumPoolSize(octaveNbEngines);
        prestartAllCoreThreads();
    }

    public void applyTimeouts(long octaveReadTimeoutSeconds,
            long octaveWriteTimeoutSeconds) {
        this.octaveReadTimeoutSeconds = octaveReadTimeoutSeconds;
        this.octaveWriteTimeoutSeconds = octaveWriteTimeoutSeconds;
    }

    @Override
    protected void beforeExecute(Thread paramThread, Runnable paramRunnable) {
        super.beforeExecute(paramThread, paramRunnable);
        OctaveEngineProxy engine = octaveEngine.get();
        if (engine != null) {
            engine.setOctaveTimeouts(octaveReadTimeoutSeconds,
                    octaveWriteTimeoutSeconds);
        }
    }

    /**
     * Static utility to create an octave engine
     *
     * @param id
     * @return
     */
    private static OctaveEngineProxy createOctaveEngine(long id) {
        String engineName = "[Octave engine " + id + " ]";
        return new ThreadSafeSimpleOctaveEngineProxy(
                OctaveConnectionManager.factory, engineName);
    }

    /**
     * Executes an Octave job in the pool.
     *
     * @param job
     * @return
     */
    public Future<?> executeOctaveJob(OctaveEngineJob job) {
        final OctaveEngineJob job2 = job;
        return this.submit(new Runnable() {
            @Override
            public void run() {
                OctaveEngineProxy engine = octaveEngine.get();
                job2.readyToExecute();
                try {
                    job2.doOctaveWork(engine);
                    job2.completedWithoutException();
                } catch (Exception e) {
                    job2.completedWithException(e);
                }

            }
        });
    }

    /* ******* thread factory ******** */

    /**
     * Same than Executors.DefaultThreadFactory except for the name prefix and
     * the thread class
     *
     * @author Sylvain Marié
     */
    static class OctaveThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        OctaveThreadFactory() {
            SecurityManager localSecurityManager = System.getSecurityManager();
            this.group = ((localSecurityManager != null) ? localSecurityManager
                    .getThreadGroup() : Thread.currentThread().getThreadGroup());

            this.namePrefix = "octave_pool-" + poolNumber.getAndIncrement()
                    + "-octave_thread-";
        }

        public Thread newThread(Runnable paramRunnable) {
            // the Runnable here is actually the Worker

            Thread localThread = new OctaveThread(this.group, paramRunnable,
                    this.namePrefix, this.threadNumber.getAndIncrement(), 0L);

            if (localThread.isDaemon())
                localThread.setDaemon(false);
            if (localThread.getPriority() != 5)
                localThread.setPriority(5);
            return localThread;
        }

        /**
         * A worker that first creates an engine, and then tries to execute jobs
         * with it.
         *
         * @author Sylvain Marié
         *
         */
        class OctaveThread extends Thread {
            private long id;

            public OctaveThread(ThreadGroup group, Runnable paramRunnable,
                    String string, long id, long m) {
                super(group, paramRunnable, string + id, m);
                this.id = id;
            }

            @Override
            public void run() {
                // store the engine in the ThreadLocal
                OctaveEngineProxy engine = createOctaveEngine(id);
                octaveEngine.set(engine);

                try {
                    super.run();
                } finally {
                    // termination : shutdown the engine
                    engine.shutdown();
                    octaveEngine.remove();
                }
            }
        }
    }
}

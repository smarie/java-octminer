package com.rapidminer.tools.octave.manager.pool;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;

import dk.ange.octave.exception.OctaveException;

/**
 * The mother class for jobs that operators will submit to the pool
 *
 * @author Sylvain Marié
 *
 */
public abstract class OctaveEngineJob {

    public static enum JOB_STATUS {
        NEW, READY_TO_EXECUTE, EXECUTED_WITHOUT_ERROR, EXECUTED_WITH_ERROR
    };

    private JOB_STATUS status;
    private Exception lastException = null;

    /**
     * Constructor. The job is created with status of "new".
     */
    public OctaveEngineJob() {
        status = JOB_STATUS.NEW;
    }

    public JOB_STATUS getStatus() {
        return status;
    }

    /**
     * This can only be a {@link RuntimeException}, an {@link OctaveException}
     * or an {@link OperatorException}
     * 
     * @return
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * The main method subclasses should implement to describe the work to be
     * executed by an octave engine.
     *
     * @param octaveEngine
     * @throws Exception
     */
    public abstract void doOctaveWork(OctaveEngineProxy octaveEngine)
            throws OctaveException, OperatorException;

    /**
     * This method is called by the executor thread to change the status just
     * before execution
     */
    void readyToExecute() {
        status = JOB_STATUS.READY_TO_EXECUTE;
    }

    /**
     * This method is called by the executor thread to change the status just
     * after catching an exception during execution
     */
    void completedWithException(Exception jobException) {
        lastException = jobException;
        status = JOB_STATUS.EXECUTED_WITH_ERROR;
    }

    /**
     * This method is called by the executor thread to change the status just
     * after successful execution
     */
    void completedWithoutException() {
        status = JOB_STATUS.EXECUTED_WITHOUT_ERROR;
    }
}
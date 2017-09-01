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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.FileProcessLocation;
import com.rapidminer.PluginInitOctaveExtension;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.octave.manager.OctaveConnectionManager;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;
import com.rapidminer.tools.octave.manager.pool.OctaveEngineJob;
import com.rapidminer.tools.octave.translation.OctaveTranslations;
import com.rapidminer.tools.octave.translation.OctaveTranslator;

import dk.ange.octave.exception.OctaveException;
import dk.ange.octave.exception.OctaveNonrecoverableException;

/**
 * This operator offers the possibility to enter arbitrary Octave code and
 * execute it. Two parameters offer the functionality to input and output data.
 *
 * @author Sylvain Marié
 * @author Yaoyu Zhang
 */
public class OctaveScriptOperator extends Operator {

    private static final ExampleSetMetaData EXAMPLE_SET_MD = new ExampleSetMetaData();
    private static final MetaData OTHER_RESULT_MD = new MetaData(
            OctaveIOObject.class);

    public static final String PARAMETER_OCTAVE_SCRIPT = "script";
    public static final String PARAMETER_VARIABLE_NAME = "name_of_variable";
    public static final String PARAMETER_INPUTS = "inputs";
    public static final String PARAMETER_RESULTS = "results";
    public static final String PARAMETER_RESULTS_TYPE = "type";
    public static final String PARAMETER_RESULTS_NAME = "name_of_variable";
    public static final String PARAMETER_USE_CELL = "use_cell";
    public static final String PARAMETER_USE_STRUCT = "use_struct";
    public static final String PARAMETER_OCTAVE_TYPE = "octave_type";
    public static final String PARAMETER_TARGET_TIME_ZONE = "target_time_zone";
    public static final String PARAMETER_DATE_ROUNDING_DIGIT_NB = "date_rounding_digit_nb";

    public static final String OCTAVE_TYPE_CELL = "cell";
    public static final String OCTAVE_TYPE_STRUCT = "struct";
    public static final String[] OCTAVE_TYPES = new String[] {
            OCTAVE_TYPE_CELL, OCTAVE_TYPE_STRUCT };
    public static final String OCTAVE_SCRIPTVARS_RMDIRECTORY = "<Rapidminer_Directory>";
    public static final String OCTAVE_SCRIPTVARS_CURRENTDIRECTORY = "<Current_Directory>";
    // public static final String OCTAVE_SCRIPTVARS_SRC_DATA = "\\RM-test-data";
    public static final String OCTAVE_SCRIPTVARS_UUID = "<UUID>";

    public static Log log = LogFactory
            .getLog("com.rapidminer.operator.octave.OctaveScriptOperator");

    /**
     * A customized rapidminer InputPortExtender that has a custom Precondition
     * which checks the number and type of inputs
     */
    private InputPortExtender inputPortExtender = new InputPortExtender(
            "input", getInputPorts()) {
        @Override
        protected Precondition makePrecondition(final InputPort port,
                final int portIndex) {
            return new Precondition() {
                @Override
                public void assumeSatisfied() {
                }

                @Override
                public void check(MetaData metaData) {
                    int numberOfInputs = 0;
                    String[] variable = null;
                    try {

                        // Changed the type of parameter to make the rapidminer
                        // can check input and out put;
                        List<String[]> variables = getParameterList(PARAMETER_INPUTS);
                        if (portIndex >= 0 && portIndex < variables.size()) {

                            variable = variables.get(portIndex);
                        } else {
                            variable = null;
                        }

                        numberOfInputs = variables.size();
                    } catch (UndefinedParameterError e) {

                    }

                    if (portIndex >= numberOfInputs && port.isConnected()) {
                        port.addError(new SimpleMetaDataError(Severity.WARNING,
                                port, "port_not_assigned_on_input"));
                    }

                    if (metaData != null) {
                        if (!OctaveTranslations.isSupportedClass(metaData
                                .getObjectClass())) {
                            port.addError(new SimpleMetaDataError(
                                    Severity.ERROR, port,
                                    "unsupported_ioobject", OctaveTranslations
                                            .getName(metaData.getObjectClass())));
                        }
                    } else if (portIndex < numberOfInputs) {
                        port.addError(new SimpleMetaDataError(Severity.ERROR,
                                port, "need_compatible_object",
                                (Object) variable));
                    }

                }

                @Override
                public String getDescription() {
                    if (portIndex < getNumberOfInputs())
                        return "<em>Expects either ExampleSet or OctaveResult</em>";
                    else
                        return "<em>No input allowed</em>";
                }

                @Override
                public MetaData getExpectedMetaData() {
                    if (portIndex < getNumberOfInputs())
                        return EXAMPLE_SET_MD;
                    return null;
                }

                @Override
                public boolean isCompatible(MetaData input,
                        CompatibilityLevel level) {
                    return EXAMPLE_SET_MD.isCompatible(input, level)
                            || OTHER_RESULT_MD.isCompatible(input, level);
                }

                private int getNumberOfInputs() {
                    try {
                        return getParameterList(PARAMETER_INPUTS).size();
                    } catch (UndefinedParameterError e) {
                        return 0;
                    }
                }
            };
        }
    };

    /**
     * The rapidminer object that handles output
     */
    private OutputPortExtender outputPortExtender = new OutputPortExtender(
            "output", getOutputPorts());

    private transient boolean isErrorOccurred = false;
    private transient String errorOccured;

    private String rapidminerDirectory = null;
    private String currentDirectory = null;

    // to dynamically cache metadata when the result is generated
    private HashMap<String, MetaData> cachedMetadata = new HashMap<String, MetaData>();

    /**
     * Constructor.
     *
     * @param description
     * @throws OperatorException
     */
    public OctaveScriptOperator(OperatorDescription description)
            throws OperatorException {

        super(description);
        inputPortExtender.start();
        outputPortExtender.start();

        // Adding rule that will meta data accordingly to parameter setting
        getTransformer().addRule(new MDTransformationRule() {
            // @Override
            public void transformMD() {
                deliverAllCorrectMetadata();
            }
        });
    }

    protected MetaData getGeneratedMetaData(String varName) {
        return cachedMetadata.get(varName);
    }

    /**
     * Finds the rapidminer working directory
     *
     * @return
     * @throws OperatorException
     */
    private String findRapidMinerDirectory() throws OperatorException {
        String rdir;
        // for 6.2 compatibility - thanks German Aquino
        // URL url = Launcher.class.getClassLoader().getResource(".");
        URL url = Operator.class.getClassLoader().getResource(".");
        if (url != null) {
            try {
                File dir = new File(new URI(url.toString()));
                if (dir.exists()) {
                    rdir = dir.getParentFile().toString();
                    return rdir;
                } else {
                    throw new OperatorException("Error finding RM directory "
                            + dir.getAbsolutePath()
                            + ", directory does not exist.");
                }
            } catch (URISyntaxException e) {
                throw new OperatorException("Error finding RM directory "
                        + url.toString(), e);
            }
        } else {
            throw new OperatorException(
                    "Error finding RM directory, null url for resource \".\" ");
        }

    }

    /**
     * Looks at the embedding process to find its location in the filesystem.
     *
     * @return
     * @throws OperatorException
     */
    private String findCurrentDirectory() throws OperatorException {

        ProcessLocation processLocation = getExecutionUnit()
                .getEnclosingOperator().getProcess().getProcessLocation();

        // now the location can be file or repository
        String path;
        String dir;
        if (processLocation instanceof FileProcessLocation) {

            FileProcessLocation fileLoc = (FileProcessLocation) processLocation;
            dir = fileLoc.getFile().getAbsolutePath();

        } else if (processLocation instanceof RepositoryProcessLocation) {
            RepositoryProcessLocation repoLoc = (RepositoryProcessLocation) processLocation;
            try {
                Repository r = repoLoc.getRepositoryLocation().getRepository();
                if (r instanceof LocalRepository) {
                    LocalRepository localR = (LocalRepository) r;
                    int lenRepository = r.toString().length();
                    int lentotal = repoLoc.toString().length();
                    String bef = localR.getFile().getPath();
                    String aft = repoLoc.toString();
                    String newpath = bef
                            + aft.substring(lenRepository + 2, lentotal);
                    // String current = temp2.replaceAll("/", "\\\\\\\\");
                    String current = backlashReplace(newpath);
                    int j = current.lastIndexOf("\\");
                    dir = current.substring(0, j);
                    // int te = current.substring(0, j).lastIndexOf("\\");
                    // dir = current.substring(0, te) +
                    // OCTAVE_SCRIPTVARS_SRC_DATA;
                } else {
                    // the repository is not a file system. So we use a folder
                    // in USER home folder
                    path = PluginInitOctaveExtension.getUserHomeFolderPath();
                    dir = path + "\\CurrentDirectoryTempFolder";
                    new File(currentDirectory).mkdirs();
                }

            } catch (RepositoryException e) {
                if (log.isErrorEnabled())
                    log.error(e);
                throw new OperatorException(
                        "Error accessing repository information", e);
            }
        } else {
            throw new OperatorException(
                    "Error accessing repository information");
        }
        return dir;
    }

    /**
     * Main method called at each execution. *
     *
     * @see com.rapidminer.operator.Operator#doWork()
     */
    @Override
    public void doWork() throws OperatorException {

        isErrorOccurred = false;
        errorOccured = null;

        final double dateRoundingDigits = getParameterAsDouble(PARAMETER_DATE_ROUNDING_DIGIT_NB);

        try {
            checkDirectories();
        } catch (Exception e) {
            log.error(e);
        }

        try {
            executeInnerDoWorkAsOctaveJob(dateRoundingDigits);
        } catch (OctaveNonrecoverableException e) {
            // try one more time with new engine automatically replaced by
            // ThreadPool.
            executeInnerDoWorkAsOctaveJob(dateRoundingDigits);
        } finally {
            errorOccured = null;
        }
    }

    /**
     * Utility method to execute the innerDoWork() method using the octave
     * engine pool
     *
     * @param dateRoundingDigits
     * @throws OctaveException
     * @throws OperatorException
     */
    protected void executeInnerDoWorkAsOctaveJob(final double dateRoundingDigits)
            throws OperatorException {

        OctaveEngineJob job = new OctaveEngineJob() {
            @Override
            public void doOctaveWork(OctaveEngineProxy octaveEngine)
                    throws OctaveException, OperatorException {
                log.info("Operator [" + getName() + "] grabbed octave engine "
                        + octaveEngine.getName());
                innerDoWork(dateRoundingDigits, octaveEngine);
            }
        };
        OctaveConnectionManager.onlyInstance.executeOctaveTaskSync(job);
    }

    /**
     * Now extracted so that
     *
     * @param dateRoundingDigits
     * @param octaveEngine
     * @throws UndefinedParameterError
     * @throws OperatorException
     * @throws UserError
     */
    protected void innerDoWork(double dateRoundingDigits,
            OctaveEngineProxy octaveEngine) throws UndefinedParameterError,
            OperatorException, UserError {

        // 01. generate uuid
        UUID octaveUuid = UUID.randomUUID();
        String octaveUUIDString = octaveUuid.toString().replaceAll("\\-", "")
                .substring(0, 10);
        if (log.isInfoEnabled())
            log.info("Operator [" + this.getName()
                    + "] Generated a random UUID String : " + octaveUUIDString);

        /*
         * ---------------------------------------------------------------- 1.
         * make RapidMiner Input available in Octave
         * ----------------------------------------------------------------
         */
        List<IOObject> inputs = inputPortExtender.getData(false);

        /* get the input parameter */
        List<String[]> inputVariables = getParameterList(PARAMETER_INPUTS);
        TimeZone param_timeZone = Tools
                .getTimeZone(getParameterAsInt(PARAMETER_TARGET_TIME_ZONE));

        /* for each object in the input */
        int i = 0;
        for (IOObject input : inputs) {
            /* get the translator for that type of input */
            OctaveTranslator<? extends IOObject> translator = OctaveTranslations
                    .getTranslators(input.getClass());

            if (inputVariables.get(i)[0].contains("<UUID>")) {
                inputVariables.get(i)[0] = inputVariables.get(i)[0].replaceAll(
                        "<UUID>", octaveUUIDString);
                // if (log.isInfoEnabled())
                // log.info("Operator [" + this.getName() +
                // "] Replaced Input variable name with UUID: "
                // + inputVariables.get(i)[0]);
            }

            if (translator != null) {
                // configure time zone
                translator.setDateTimeZone(param_timeZone);

                // export into an octave object - struct or cell according
                // to user's choice
                log.info("Operator [" + this.getName()
                        + "] Transforming input " + i + " into "
                        + inputVariables.get(i)[1] + " octave variable "
                        + inputVariables.get(i)[0]);
                translator.exportObject(octaveEngine, inputVariables.get(i)[0],
                        input, inputVariables.get(i)[1], dateRoundingDigits);
            } else {
                throw new UserError(this, "octave.in.no_translator_available",
                        input.getClass().getSimpleName());
            }
            i++;
        }

        /*
         * ---------------------------------------------------------------- 2.
         * executing script itself
         * ----------------------------------------------------------------
         */
        if (isParameterSet(PARAMETER_OCTAVE_SCRIPT)) {
            String script = getParameterAsString(PARAMETER_OCTAVE_SCRIPT);

            if (script.contains(OCTAVE_SCRIPTVARS_RMDIRECTORY)) {
                if (rapidminerDirectory != null) {
                    script = script.replaceAll(OCTAVE_SCRIPTVARS_RMDIRECTORY,
                            rapidminerDirectory);
                    if (log.isInfoEnabled())
                        log.info("Replaced in script keyword "
                                + OCTAVE_SCRIPTVARS_RMDIRECTORY + " with "
                                + rapidminerDirectory);
                } else {
                    if (log.isWarnEnabled())
                        log.warn("Could *NOT* Replace in script the keyword "
                                + OCTAVE_SCRIPTVARS_RMDIRECTORY
                                + " because I have a null value for this string");
                }
            }
            if (script.contains(OCTAVE_SCRIPTVARS_CURRENTDIRECTORY)) {
                if (currentDirectory != null) {
                    script = script.replaceAll(
                            OCTAVE_SCRIPTVARS_CURRENTDIRECTORY,
                            currentDirectory);
                    if (log.isInfoEnabled())
                        log.info("Replaced in script keyword "
                                + OCTAVE_SCRIPTVARS_CURRENTDIRECTORY + " with "
                                + currentDirectory);
                } else {
                    if (log.isWarnEnabled())
                        log.warn("Could *NOT* Replace in script the keyword "
                                + OCTAVE_SCRIPTVARS_CURRENTDIRECTORY
                                + " because I have a null value for this string");
                }
            }
            if (script.contains(OCTAVE_SCRIPTVARS_UUID)) {
                script = script.replaceAll(OCTAVE_SCRIPTVARS_UUID,
                        octaveUUIDString);
                if (log.isInfoEnabled())
                    log.info("Replaced in script keyword "
                            + OCTAVE_SCRIPTVARS_UUID + " with "
                            + octaveUUIDString);
            }

            // is this thread-safe ?
            log.info("Operator [" + this.getName()
                    + "] Evaluation of Octave script ... ");
            octaveEngine.eval(script);
            log.info("Operator [" + this.getName()
                    + "] Evaluation of Octave script ... DONE");
        }

        // checking for errors during execution occurred on log
        if (isErrorOccurred)
            throw new UserError(this, new Throwable(errorOccured),
                    "octave.octave_error");

        /*
         * 3. retrieving results defined in the parameters
         */
        List<IOObject> outputs = new LinkedList<IOObject>();
        List<String[]> resultVariables = ParameterTypeList
                .transformString2List(getParameterAsString(PARAMETER_RESULTS));
        for (String[] nameTypePair : resultVariables) {
            Class<? extends IOObject> resultClass = OctaveTranslations
                    .getClass(nameTypePair[1]);
            // ask for the translator able to translate
            OctaveTranslator<? extends IOObject> translator = OctaveTranslations
                    .getTranslators(resultClass);
            if (translator != null) {
                // use the translator

                translator.setDateTimeZone(param_timeZone);

                /**
                 * Add uuid for octave output;
                 */
                if (nameTypePair[0].contains(OCTAVE_SCRIPTVARS_UUID)) {
                    nameTypePair[0] = nameTypePair[0].replaceAll(
                            OCTAVE_SCRIPTVARS_UUID, octaveUUIDString);
                    if (log.isInfoEnabled())
                        log.info("Replaced Output variable name with UUID: "
                                + nameTypePair[0]);
                }
                log.info("Operator [" + this.getName()
                        + "] Transforming octave variable " + nameTypePair[0]
                        + " into Rapidminer ExampleSet output");
                IOObject out = translator.importObject(octaveEngine,
                        nameTypePair[0], dateRoundingDigits);
                if (out instanceof ExampleSet) {
                    ExampleSet eOut = (ExampleSet) out;
                    cachedMetadata.put(nameTypePair[0],
                            getMetadataFromExampleSet(eOut));
                }
                outputs.add(out);
            } else {
                throw new UserError(this, "octave.out.no_translator_available",
                        resultClass.getSimpleName());
            }
        }

        // TODO maybe here we should try to compute the metadata
        // intelligently?
        // ANSWER: NO, we should rather advise users to check
        // "Process > Sync MetaData with real data"
        // NEW ANSWER : yes we should because this option is not available
        // anymore
        outputPortExtender.deliver(outputs);
        // outputPortExtender.deliverMetaData(inputMD);
        // outputPortExtender.deliverMetaData(inputMD)

        deliverAllCorrectMetadata();
    }

    /**
     * This method allows to dynamically deliver the correct metadata from the
     * outputs of the octave script, cached in the map.
     */
    protected void deliverAllCorrectMetadata() {
        // iterating over parameter to decide what will be delivered
        try {
            List<String[]> parameterList = getParameterList(PARAMETER_RESULTS);
            List<OutputPort> managedPorts = outputPortExtender
                    .getManagedPorts();
            Iterator<OutputPort> iterator = managedPorts.iterator();
            for (String[] pair : parameterList) {
                if (iterator.hasNext()) {
                    OutputPort port = iterator.next();
                    Class<? extends IOObject> class1 = OctaveTranslations
                            .getClass(pair[1]);
                    if (class1 != null) {
                        MetaData cachedMetaData = OctaveScriptOperator.this
                                .getGeneratedMetaData(pair[0]);
                        // this is where we update the metadata of the
                        // result
                        if (cachedMetaData != null)
                            port.deliverMD(cachedMetaData);
                        else
                            port.deliverMD(new MetaData(class1));
                    }
                } else {
                    break;
                }
            }
        } catch (UndefinedParameterError e) {
        }
    }

    /**
     * Creates a new ExampleSetMetadata from the given example set
     *
     * @param eOut
     * @return
     */
    private MetaData getMetadataFromExampleSet(ExampleSet eOut) {
        return new ExampleSetMetaData(eOut, false);
    }

    private void checkDirectories() throws OperatorException {
        if (currentDirectory == null) {
            currentDirectory = findCurrentDirectory();
            currentDirectory = currentDirectory.replaceAll("\\\\",
                    "\\\\\\\\\\\\\\\\");
        }
        if (rapidminerDirectory == null) {
            rapidminerDirectory = findRapidMinerDirectory();
            rapidminerDirectory = rapidminerDirectory.replaceAll("\\\\",
                    "\\\\\\\\\\\\\\\\");
        }
    }

    /**
     * replace the "/" with "\\"
     *
     * @param myStr
     *            in put string
     * @return
     */
    public static String backlashReplace(String myStr) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(
                myStr);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {

            if (character == '/') {
                result.append("\\");
            } else {
                result.append(character);
            }

            character = iterator.next();
        }
        return result.toString();
    }

    /**
     * This script operator has three inputs:
     * <ul>
     * <li>the octave script
     * <li>the list of input ExampleSets to transform to octave objects
     * <li>the list of octave objects to output as ExampleSets
     * </ul>
     *
     * @see com.rapidminer.operator.Operator#getParameterTypes()
     */
    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        /* 1. the script parameter */
        ParameterType type = new ParameterTypeText(
                PARAMETER_OCTAVE_SCRIPT,
                "This script will be executed on one of the available octave servers. "
                        + "In the script you may use &lt;UUID&gt; in variable "
                        + "names so that they become unique, and you may also use the "
                        + "following special tags : &lt;Current_Directory&gt; "
                        + "and &lt;Rapidminer_Directory&gt;. Use addpath() ",
                TextType.PLAIN, getDefaultScript());
        type.setExpert(false);
        types.add(type);

        /* 2. the inputs parameter */
        // type = new ParameterTypeEnumeration(
        // PARAMETER_INPUTS,
        // "This assigns each input port a variable name. If the type of input object is supported by the Octave translation, it will be accessible under this variable name.",
        // new ParameterTypeString(PARAMETER_VARIABLE_NAME,
        // "This is the name of the variable containing input port's data."));
        type = new ParameterTypeList(
                PARAMETER_INPUTS,
                "This assigns each input port a variable name. If the type of input "
                        + "object is supported by the Octave translation, it will be accessible "
                        + "under this variable name, and translated according to the selected "
                        + "mode. You can ensure that the variable name will be unique in Octave "
                        + "by appending the special tag &lt;UUID&gt; at the end of "
                        + "variable name (note: you will need to append it in each occurence of "
                        + "the variable name in the script too)",
                new ParameterTypeString(PARAMETER_VARIABLE_NAME,
                        "This is the name of the variable containing input port's data."),
                new ParameterTypeCategory(
                        PARAMETER_OCTAVE_TYPE,
                        "This indicates the type the variable should be translated to in Octave . Please mention, that a wrong type will cause errors.",
                        OCTAVE_TYPES, 0));
        type.setExpert(false);
        types.add(type);

        /* 3. the output parameter */
        type = new ParameterTypeList(
                PARAMETER_RESULTS,
                "This maps a variable name in Octave to an RapidMiner Object supported by "
                        + "the Octave Translation. You can ensure that the variable name will be unique "
                        + "in Octave by appending the special tag &lt;UUID&gt; at the end of "
                        + "variable name (note: you will need to append it in each occurence of "
                        + "the variable name in the script too)",
                new ParameterTypeString(
                        PARAMETER_RESULTS_NAME,
                        "This is the variable in Octave, filled by the script that should be translated to RapidMiner"),
                new ParameterTypeCategory(
                        PARAMETER_RESULTS_TYPE,
                        "This indicates the type the variable in Octave should be translated to. Please mention, that a wrong type will cause errors.",
                        OctaveTranslations.getSupportedClassNames(), 0));
        type.setExpert(false);
        types.add(type);

        /* 4. the output parameter */
        type = new ParameterTypeCategory(PARAMETER_TARGET_TIME_ZONE,
                "The time zone used to convert the datetime attributes",
                Tools.getAllTimeZones(), Tools.getPreferredTimeZoneIndex());
        type.setExpert(false);
        types.add(type);

        /* 5. the nb of rounding digits */
        types.add(new ParameterTypeDouble(
                PARAMETER_DATE_ROUNDING_DIGIT_NB,
                "To compensate truncation of the date in MATLAB, we round to the upper n-th digit after decimal",
                1, 50, 9, true));

        return types;
    }

    private String getDefaultScript() {
        StringBuffer sb = new StringBuffer();
        sb.append("%% ================= New octave script ===================\n");
        sb.append("%\n");
        sb.append("% Useful properties: <UUID>, <Current_Directory>, <Rapidminer_Directory> \n");
        sb.append("% Useful commands: \n");
        sb.append("% * save(\"-v7\",\"<Current_Directory>\\\\test.mat\",\"VariableName\");\n");
        sb.append("% * addpath(\"<Current_Directory>\\\\myFunction.m\");\n");
        sb.append("% \n");
        sb.append("% * [encodedData name role levels]=openExampleSet(Input);\n");
        sb.append("% * StructOutput= createStructExampleSet(encodedData,name,role,levels);\n");
        sb.append("% * CellOutput= createCellExampleSet(encodedData,name,role,levels);\n");
        sb.append("% \n");

        // sb.append("% Inputs of this script when created : ");
        //
        // try {
        // List<String[]> inputVariables = getParameterList(PARAMETER_INPUTS);
        // int i = 0;
        // for (String[] input : inputVariables) {
        // sb.append(input[0]);
        // sb.append(" (");
        // sb.append(input[1]);
        // sb.append(")");
        // sb.append(",");
        // }
        // sb.deleteCharAt(sb.length());
        // sb.append("\n");
        //
        // sb.append("% Outputs of this script when created : ");
        // List<String[]> outputVariables =
        // ParameterTypeList.transformString2List(getParameterAsString(PARAMETER_RESULTS));
        // for (String[] output : outputVariables) {
        // sb.append(output[0]);
        // sb.append(",");
        // }
        // sb.deleteCharAt(sb.length());
        // sb.append("\n");
        // } catch (UndefinedParameterError e) {
        // sb.append("error getting i/o information\n");
        // if (log.isErrorEnabled())
        // log.error(e);
        // }
        sb.append("%% ================ (enter your code below) ============ \n");
        return sb.toString();
    }

}

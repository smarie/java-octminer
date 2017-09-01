/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.octave;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapid_i.Launcher;
import com.rapidminer.FileProcessLocation;
import com.rapidminer.PluginInitOctaveExtension;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
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
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.octave.manager.OctaveConnectionManager;
import com.rapidminer.tools.octave.manager.OctaveEngineProxy;
import com.rapidminer.tools.octave.translation.OctaveTranslations;
import com.rapidminer.tools.octave.translation.OctaveTranslator;

/**
 * This operator offers the possibility to enter arbitrary Octave code and
 * execute it. Two parameters offer the functionality to input and output data.
 * 
 * 
 * 
 * 
 * @author Sebastian Land
 */
public class OctaveScriptOperator extends Operator {

	private static final ExampleSetMetaData EXAMPLE_SET_MD = new ExampleSetMetaData();
	private static final MetaData RRESULT_MD = new MetaData(
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

	public static final String OCTAVE_TYPE_CELL = "cell";
	public static final String OCTAVE_TYPE_STRUCT = "struct";
	public static final String[] OCTAVE_TYPES = new String[] {
			OCTAVE_TYPE_CELL, OCTAVE_TYPE_STRUCT };
	public static final String OCTAVE_SCRIPTVARS_RMDIRECTORY = "<Rapidminer_Directory>";
	public static final String OCTAVE_SCRIPTVARS_CURRENTDIRECTORY = "<Current_Directory>";
	public static final String OCTAVE_SCRIPTVARS_SRC_DATA = "\\RM-test-data";
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

						// Changed the type of parameter to make the rapidminer can check input and out put;
						 List<String[]> variables = getParameterList(PARAMETER_INPUTS);
						if (portIndex >= 0 && portIndex < variables.size()){

							variable = variables.get(portIndex);}
						else{
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
								port, "need_compatible_object", (Object) variable));
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
							|| RRESULT_MD.isCompatible(input, level);
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

	public OctaveScriptOperator(OperatorDescription description)
			throws OperatorException {

		super(description);
		inputPortExtender.start();
		outputPortExtender.start();

		// Adding rule that will meta data accordingly to parameter setting
		getTransformer().addRule(new MDTransformationRule() {
			// @Override
			public void transformMD() {
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
							if (class1 != null)
								port.deliverMD(new MetaData(class1));
						} else {
							break;
						}
					}
				} catch (UndefinedParameterError e) {
				}
			}
		});
	}

	/**
	 * Finds the rapidminer working directory
	 * 
	 * @return
	 * @throws OperatorException
	 */
	private String findRapidMinerDirectory() throws OperatorException {
		String rdir;
		URL url = Launcher.class.getClassLoader().getResource(".");
		if (url != null) {
			try {
				File dir = new File(new URI(url.toString()));
				if (dir.exists()) {
					rdir = dir.getParentFile().toString();
					return rdir;
				} else {
					throw new OperatorException("Error finding RM directory " + dir.getAbsolutePath() + ", directory does not exist.");
				}
			} catch (URISyntaxException e) {
				throw new OperatorException("Error finding RM directory " + url.toString(), e);
			}
		} else {
			throw new OperatorException("Error finding RM directory, null url for resource \".\" ");
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
					String aft = localR.getFile().getAbsolutePath();
					String bef = repoLoc.toString();
					String newpath = aft
							+ bef.substring(lenRepository + 2, lentotal);
					// String current = temp2.replaceAll("/", "\\\\\\\\");
					String current = backlashReplace(newpath);
					int j = current.lastIndexOf("\\");
					int te = current.substring(0, j).lastIndexOf("\\");
					dir = current.substring(0, te) + OCTAVE_SCRIPTVARS_SRC_DATA;
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
	 * 
	 * 
	 * 
	 * @see com.rapidminer.operator.Operator#doWork()
	 */
	@Override
	public void doWork() throws OperatorException {
		isErrorOccurred = false;
		errorOccured = null;
		
		try{
		checkDirectories();
		} catch (Exception e){
			log.error(e);
		}
		// 0. get the engine
		OctaveEngineProxy octaveEngine = OctaveConnectionManager.onlyInstance
				.getScriptEngine(this);
		if (octaveEngine != null)
			log.info("Operator [" + this.getName() + "] grabbed octave engine "
					+ octaveEngine.getName());

		try {
			// 01. generate uuid
			UUID octaveUuid = UUID.randomUUID();
			String octaveUUIDString = octaveUuid.toString()
					.replaceAll("\\-", "").substring(0, 10);
			if (log.isInfoEnabled())
				log.info("Generated a random UUID String for operator "
						+ this.getName() + " : " + octaveUUIDString);

			/*
			 * ----------------------------------------------------------------
			 * 1. make RapidMiner Input available in Octave
			 * ----------------------------------------------------------------
			 */
			List<IOObject> inputs = inputPortExtender.getData(false);

			/* get the input parameter */
			List<String[]> inputVariables = getParameterList(PARAMETER_INPUTS);

			/* for each object in the input */
			int i = 0;
			for (IOObject input : inputs) {
				/* get the translator for that type of input */
				OctaveTranslator<? extends IOObject> translator = OctaveTranslations
						.getTranslators(input.getClass());

				if (inputVariables.get(i)[0].contains("<UUID>")) {
					inputVariables.get(i)[0] = inputVariables.get(i)[0]
							.replaceAll("<UUID>", octaveUUIDString);
					if (log.isInfoEnabled())
						log.info("Replaced Input variable name with UUID: "
								+ inputVariables.get(i)[0]);
				}

				if (translator != null) {
					// export into an octave object - struct or cell according
					// to user's choice
					translator.exportObject(octaveEngine,
							inputVariables.get(i)[0], input,
							inputVariables.get(i)[1]);
				} else {
					throw new UserError(this, "octave.in.no_translator_available",
							input.getClass().getSimpleName());
				}
				i++;
			}

			/*
			 * ----------------------------------------------------------------
			 * 2. executing script itself
			 * ----------------------------------------------------------------
			 */
			if (isParameterSet(PARAMETER_OCTAVE_SCRIPT)) {
				String script = getParameterAsString(PARAMETER_OCTAVE_SCRIPT);

				if (script.contains(OCTAVE_SCRIPTVARS_RMDIRECTORY)) {
					script = script.replaceAll(OCTAVE_SCRIPTVARS_RMDIRECTORY,
							rapidminerDirectory);
					if (log.isInfoEnabled())
						log.info("Replaced in script keyword "
								+ OCTAVE_SCRIPTVARS_RMDIRECTORY + " with "
								+ rapidminerDirectory);
				}
				if (script.contains(OCTAVE_SCRIPTVARS_CURRENTDIRECTORY)) {
					script = script.replaceAll(
							OCTAVE_SCRIPTVARS_CURRENTDIRECTORY,
							currentDirectory);
					if (log.isInfoEnabled())
						log.info("Replaced in script keyword "
								+ OCTAVE_SCRIPTVARS_CURRENTDIRECTORY + " with "
								+ currentDirectory);
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
				octaveEngine.eval(script);
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
					outputs.add(translator.importObject(octaveEngine,
							nameTypePair[0]));
				} else {
					throw new UserError(this, "octave.out.no_translator_available",
							resultClass.getSimpleName());
				}
			}
			outputPortExtender.deliver(outputs);

		} finally {
			errorOccured = null;
			// make sure session will be released again
			if (octaveEngine != null) {
				octaveEngine.releaseUser(this);
				log.info("Operator [" + this.getName()
						+ "] released octave engine " + octaveEngine.getName());
			}
		}
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
	 * This script operqtor hqs three inputs:
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
						+ "and &lt;Rapidminer_Directory&gt;, ",
				TextType.PLAIN, true);
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
		// type = new ParameterTypeStringCategory(PARAMETER_OCTAVE_TYPE,
		// "Specifies the ExempleSet Type.", OCTAVE_TYPES, OCTAVE_TYPE_STRUCT,
		// false);
		// type.setExpert(false);
		// types.add(type);

		return types;
	}

}

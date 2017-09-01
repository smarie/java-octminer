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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import com.rapidminer.tools.octave.manager.OctaveConnectionManager;
import com.rapidminer.tools.octave.translation.OctaveTranslations;
import com.rapidminer.tools.octave.translation.OctaveTranslator;

import dk.ange.octave.OctaveEngine;

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
	public static final String[] OCTAVE_TYPES = new String[] { OCTAVE_TYPE_CELL, OCTAVE_TYPE_STRUCT };

	// private static final String[] OCTAVE_TYPES = {"cell", "struct"};
	//
	// /** Indicates a CELL. */
	// public static final int OCTAVE_CELL = 0;
	//
	// /** Indicates a STRUCT. */
	// public static final int OCTAVE_STRUCT = 1;

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
					String variable = null;
					try {
						String[] variables = ParameterTypeEnumeration
								.transformString2Enumeration(getParameterAsString(PARAMETER_INPUTS));
						if (portIndex >= 0 && portIndex < variables.length)
							variable = variables[portIndex];
						else
							variable = "";
						numberOfInputs = variables.length;
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
								port, "need_compatible_object", variable));
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
						return ParameterTypeEnumeration
								.transformString2Enumeration(getParameterAsString(PARAMETER_INPUTS)).length;
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

	public OctaveScriptOperator(OperatorDescription description) {
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
	 * 
	 * 
	 * 
	 * @see com.rapidminer.operator.Operator#doWork()
	 */
	@Override
	public void doWork() throws OperatorException {
		isErrorOccurred = false;
		errorOccured = null;
		
		OctaveEngine octaveEngine = OctaveConnectionManager.getScriptEngine();
		if (octaveEngine != null)
			log.info("Grabbed octave engine");

		try {
			/*
			 * 1. make RapidMiner Input available in Octave
			 */
			List<IOObject> inputs = inputPortExtender.getData(false);
			
			/* get the input parameter */
			//String[] inputVariableNames = ParameterTypeEnumeration
			//		.transformString2Enumeration(getParameterAsString(PARAMETER_INPUTS));
			List<String[]> inputVariables = ParameterTypeList
					.transformString2List(getParameterAsString(PARAMETER_INPUTS));
			
			/* for each object in the input */
			int i = 0;
			for (IOObject input : inputs) {
				/* get the translator for that type of input */
				OctaveTranslator<? extends IOObject> translator = OctaveTranslations
						.getTranslators(input.getClass());
				
				if (translator != null) {
					/* export into an octave object - struct or cell according to user's choice */
					translator.exportObject(octaveEngine,
							inputVariables.get(i)[0], input, inputVariables.get(i)[1]);					
				} else {
					throw new UserError(this, "Octave.no_translator_available",
							input.getClass().getSimpleName());
				}
				i++;
			}

			/*
			 * 2. executing script itself
			 */
			if (isParameterSet(PARAMETER_OCTAVE_SCRIPT))
				octaveEngine
						.eval(getParameterAsString(PARAMETER_OCTAVE_SCRIPT));
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
					outputs.add(translator.importObject(octaveEngine,
							nameTypePair[0]));
				} else {
					throw new UserError(this, "Octave.no_translator_available",
							resultClass.getSimpleName());
				}
			}
			outputPortExtender.deliver(outputs);

		} finally {
			errorOccured = null;
			// make sure session will be released again

		}
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
				"This script will be executed on one of the available octave servers.",
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
				"This assigns each input port a variable name. If the type of input object is supported by the Octave translation, it will be accessible under this variable name, and translated according to the selected mode.",
				new ParameterTypeString(
						PARAMETER_VARIABLE_NAME,
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
				"This maps a variable name in Octave to an RapidMiner Object supported by the Octave Translation.",
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
//		type = new ParameterTypeStringCategory(PARAMETER_OCTAVE_TYPE,
//				"Specifies the ExempleSet Type.", OCTAVE_TYPES, OCTAVE_TYPE_STRUCT,
//				false);
//		type.setExpert(false);
//		types.add(type);

		return types;
	}

}

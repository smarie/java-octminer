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
package com.rapidminer.tools.octave;

import java.util.jar.JarFile;

import com.rapidminer.tools.GenericOperatorFactory;
import com.rapidminer.tools.plugin.Plugin;

/**
 * Registers all Octave core operators.
 * 
 * @author Sylvain Marié
 * @author Yaoyu Zhang
 */
public class OctaveOperatorFactory implements GenericOperatorFactory {

//	public static String[] WEKA_ASSOCIATORS = new String[0];
//	public static String[] WEKA_ATTRIBUTE_EVALUATORS = new String[0];
//    public static String[] WEKA_META_CLASSIFIERS = new String[0]; 
//	public static String[] WEKA_CLASSIFIERS = new String[0];
//	public static String[] WEKA_CLUSTERERS = new String[0];
//	
//	private static final String[] SKIPPED_META_CLASSIFIERS = new String[] { "weka.classifiers.meta.AttributeSelectedClassifier", "weka.classifiers.meta.CVParameterSelection", "weka.classifiers.meta.ClassificationViaRegression", "weka.classifiers.meta.FilteredClassifier", "weka.classifiers.meta.MultiScheme", "weka.classifiers.meta.Vote",
//			"weka.classifiers.meta.Grading", "weka.classifiers.meta.Stacking", "weka.classifiers.meta.StackingC", "weka.classifiers.meta.RotationForest$ClassifierWrapper" };
//
//	private static final String[] SKIPPED_CLASSIFIERS = new String[] { ".meta.", ".pmml.", "weka.classifiers.functions.LibSVM", "MISVM", "UserClassifier", "LMTNode", "PreConstructedLinearModel", "RuleNode", "FTInnerNode", "FTLeavesNode", "FTNode", "weka.classifiers.functions.LibLINEAR" };
//
//	private static final String[] SKIPPED_CLUSTERERS = new String[] { "weka.clusterers.FilteredClusterer", "weka.clusterers.OPTICS", "weka.clusterers.DBScan", "weka.clusterers.MakeDensityBasedClusterer" };
//
//	private static final String[] SKIPPED_ASSOCIATORS = new String[] { "FilteredAssociator" };
//
//	private static final String[] ENSEMBLE_CLASSIFIERS = new String[] { "weka.classifiers.meta.MultiScheme", "weka.classifiers.meta.Vote", "weka.classifiers.meta.Grading", "weka.classifiers.meta.Stacking", "weka.classifiers.meta.StackingC" };
//
//	private static final Map<String, String> DEPRECATED_CLASSIFIER_INFOS = new HashMap<String, String>();
//
//	static {
//		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayesSimple", "Deprecated: please use NaiveBayes instead.");
//		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayesUpdateable", "Deprecated: please use NaiveBayes instead.");
//		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayes", "Deprecated: please use NaiveBayes instead.");
//	}

	@Override
	public void registerOperators(ClassLoader classLoader, Plugin plugin) {
		JarFile jarFile = plugin.getArchive();
//		
//		WEKA_ATTRIBUTE_EVALUATORS = WekaTools.getWekaClasses(jarFile, AttributeEvaluator.class);
//	    WEKA_META_CLASSIFIERS = WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ".meta.", true);
//		WEKA_CLASSIFIERS = WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ".meta.", false);
//		WEKA_CLUSTERERS = WekaTools.getWekaClasses(jarFile, Clusterer.class);
//
//		
		// octave
//		try {
//			WekaTools.registerOctaveOperators(classLoader, WekaTools
//					.getWekaClasses(jarFile, weka.classifiers.Classifier.class,
//							null, SKIPPED_CLASSIFIERS),
//					DEPRECATED_CLASSIFIER_INFOS,
//					"com.rapidminer.operator.learner.weka.GenericWekaLearner",
//					"The weka learner",
//					"octave.", null,
//					plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING,
//					"Cannot register Octave operator: " + e, e);
//		}
		
//		// learning schemes
//		try {
//			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, null, SKIPPED_CLASSIFIERS), DEPRECATED_CLASSIFIER_INFOS, "com.rapidminer.operator.learner.weka.GenericWekaLearner", "The weka learner", "modeling.classification_and_regression.weka.", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka learners: " + e, e);
//		}
//
//		// meta learning schemes
//		try {
//			
//			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, new String[] { ".meta." }, SKIPPED_META_CLASSIFIERS), "com.rapidminer.operator.learner.weka.GenericWekaMetaLearner", "The weka meta learner", "modeling.classification_and_regression.weka.", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka meta learners: " + e, e);
//		}
//
//		// ensemble learning schemes
//		try {
//			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ENSEMBLE_CLASSIFIERS, null), "com.rapidminer.operator.learner.weka.GenericWekaEnsembleLearner", "The weka ensemble learner", "modeling.classification_and_regression.weka.", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka ensemble learners: " + e, e);
//		}
//
//		// association rule learners
//		try {
//			WEKA_ASSOCIATORS = WekaTools.getWekaClasses(jarFile, weka.associations.Associator.class, null, SKIPPED_ASSOCIATORS);
//			WekaTools.registerWekaOperators(classLoader, WEKA_ASSOCIATORS, "com.rapidminer.operator.learner.weka.GenericWekaAssociationLearner", "The weka associator", "modeling.associations.weka", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka association rule learners: " + e, e);
//		}
//
//		// feature weighting
//		try {
//			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.attributeSelection.AttributeEvaluator.class), "com.rapidminer.operator.features.weighting.GenericWekaAttributeWeighting", "The weka attribute evaluator", "modeling.weighting.weka", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka feature weighting schemes: " + e, e);
//		}
//
//		// clusterers
//		try {
//			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.clusterers.Clusterer.class, SKIPPED_CLUSTERERS, false), "com.rapidminer.operator.clustering.clusterer.GenericWekaClustererAdaptor", "The weka clusterer", "modeling.clustering.weka", null, plugin);
//		} catch (Exception e) {
//			LogService.getRoot().log(Level.WARNING, "Cannot register Weka clusterers: " + e, e);
//		}
	}
}

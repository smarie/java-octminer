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
package com.rapidminer.tools.octave.translation;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.IOObject;

/**
 * @author Sylvain Marié
 * @author Yaoyu Zhang
 * 
 */
public class OctaveTranslations {
	private static Map<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>> classToTranslatorMap = new LinkedHashMap<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>>();

	private static Log log = LogFactory
			.getLog("com.rapidminer.tools.octave.translation.OctaveTranslations");

	static {
		registerTranslator(new ExampleSetTranslator());
		// registerTranslator(new RExpressionTranslator());
	}

	/**
	 * This method will add the given translator the the available ones.
	 */
	public static void registerTranslator(
			OctaveTranslator<? extends IOObject> translator) {
		Collection<OctaveTranslator<? extends IOObject>> translators = classToTranslatorMap
				.get(translator.getSupportedClass());
		if (translators == null) {
			translators = new LinkedList<OctaveTranslator<? extends IOObject>>();
			classToTranslatorMap.put(translator.getSupportedClass(),
					translators);
		}
		translators.add(translator);
	}

	/**
	 * This method will return the first suitable translator. If you need to get
	 * a specific translator, you must use the method where to specify it's name
	 * additionally.
	 */
	public static OctaveTranslator<? extends IOObject> getTranslators(
			Class<? extends IOObject> forClass) {
		Collection<OctaveTranslator<? extends IOObject>> collection = classToTranslatorMap
				.get(forClass);
		if (collection != null && !collection.isEmpty()) {
			return collection.iterator().next();
		} else {
			// having to slowly iterate over all entries
			for (Map.Entry<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>> entry : classToTranslatorMap
					.entrySet()) {
				if (entry.getKey().isAssignableFrom(forClass)) {
					collection = entry.getValue();
					if (collection != null && !collection.isEmpty()) {
						return collection.iterator().next();
					}
				}
			}
			return null;
		}
	}

	/**
	 * This method will return a sorted list of the RapidMiner names of all
	 * supported RapidMiner class types. If called before the RendererService is
	 * initialized, emtpy Strings might be returned.
	 */
	public static String[] getSupportedClassNames() {
		String[] names = new String[classToTranslatorMap.size()];
		int i = 0;
		for (Class<? extends IOObject> supportedClass : classToTranslatorMap
				.keySet()) {
			String name = null;
			try {
				name = RendererService.getName(supportedClass);
			} catch (java.lang.IndexOutOfBoundsException e) {
				// this is normal at startup
				if (log.isWarnEnabled())
					log.warn("Caught IndexOutOfBoundException from Rapidminer Renderer service when asked " +
							"getName(ExampleSet class). This seems normal at startup");
			} finally {
				if (name != null)
					names[i] = name;
				else
					names[i] = "";
				i++;
			}
		}
		Arrays.sort(names);
		return names;
	}

	/**
	 * Returns whether this io object class is supported to be exported.
	 */
	public static boolean isSupportedClass(Class<? extends IOObject> clazz) {
		return getTranslators(clazz) != null;
	}

	/**
	 * This method will return the class associated with that name. Mention,
	 * that this method is currently equivalent to calling
	 * RendererService.getClass(), but should be used instead, since underlying
	 * implementation might change.
	 */
	public static Class<? extends IOObject> getClass(String objectName) {
		return RendererService.getClass(objectName);
	}

	/**
	 * Returns the name of this class
	 */
	public static String getName(Class<? extends IOObject> objectClass) {
		return RendererService.getName(objectClass);
	}
}

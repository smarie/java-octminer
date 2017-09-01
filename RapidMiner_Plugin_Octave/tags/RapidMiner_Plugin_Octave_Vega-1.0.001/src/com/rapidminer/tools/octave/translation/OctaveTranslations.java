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
 * @author Sebastian Land
 * 
 */
public class OctaveTranslations {
	private static Map<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>> classToTranslatorMap = new LinkedHashMap<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>>();

	private static Log log = LogFactory.getLog("com.rapidminer.tools.octave.translation.OctaveTranslations");
	
	static {
		registerTranslator(new ExampleSetTranslator());
		//registerTranslator(new RExpressionTranslator());
	}

	/**
	 * This method will add the given translator the the available ones.
	 */
	public static void registerTranslator(OctaveTranslator<? extends IOObject> translator) {
		Collection<OctaveTranslator<? extends IOObject>> translators = classToTranslatorMap.get(translator.getSupportedClass());
		if (translators == null) {
			translators = new LinkedList<OctaveTranslator<? extends IOObject>>();
			classToTranslatorMap.put(translator.getSupportedClass(), translators);
		}
		translators.add(translator);
	}

	/**
	 * This method will return the first suitable translator. If you need to get a specific translator, you must use the
	 * method where to specify it's name additionally.
	 */
	public static OctaveTranslator<? extends IOObject> getTranslators(Class<? extends IOObject> forClass) {
		Collection<OctaveTranslator<? extends IOObject>> collection = classToTranslatorMap.get(forClass);
		if (collection != null && !collection.isEmpty()) {
			return collection.iterator().next();
		} else {
			// having to slowly iterate over all entries
			for (Map.Entry<Class<? extends IOObject>, Collection<OctaveTranslator<? extends IOObject>>> entry: classToTranslatorMap.entrySet()) {
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
	 * This method will return a sorted list of the RapidMiner names of all supported RapidMiner class types.
	 * If called before the RendererService is initialized, emtpy Strings might be returned.
	 */
	public static String[] getSupportedClassNames() {
		String[] names = new String[classToTranslatorMap.size()];
		int i = 0;
		for (Class<? extends IOObject> supportedClass : classToTranslatorMap.keySet()) {
			String name = null;
			//try{
				name = RendererService.getName(supportedClass);
			//} 
//			catch(java.lang.IndexOutOfBoundsException e){
//				//this is normal at startup
//				if (log.isWarnEnabled())
//					log.warn("Caught IndexOutOfBoundException from Rapidminer Renderer service. This is normal at startup");
//			}
//			finally {
				if (name != null)
					names[i] = name;
				else
					names[i] = "";
				i++;
//			}
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
	 * This method will return the class associated with that name. Mention, that
	 * this method is currently equivalent to calling RendererService.getClass(),
	 * but should be used instead, since underlying implementation might change.
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

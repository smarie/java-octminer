/*
 *  RapidMiner Octave Extension
 *
 *  Copyright (C) 2001-2012 by Rapid-I and the contributors
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
package com.rapidminer.tools.octave;

import java.lang.reflect.InvocationTargetException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.plugin.Plugin;

/**
 * This particular subclass of {@link OperatorDescription} handles the
 * dynamic parameter creation of Octave operators.
 * 
 * @author Sebastian Land
 */
public class OctaveOperatorDescription extends OperatorDescription {
    public OctaveOperatorDescription(ClassLoader classLoader, String name, String name2, String operatorClass, String shortDescription, String longDescription, String operatorGroup, String icon, String deprecationInfo, Plugin plugin) throws ClassNotFoundException {
        super(classLoader, name, name, operatorClass, shortDescription, longDescription, operatorGroup, icon, deprecationInfo, plugin);
    }

    @Override
    protected Operator createOperatorInstanceByDescription(OperatorDescription description) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
        Operator octaveOperator = super.createOperatorInstanceByDescription(description);
        octaveOperator.getParameterTypes();
        return octaveOperator;
    }

}

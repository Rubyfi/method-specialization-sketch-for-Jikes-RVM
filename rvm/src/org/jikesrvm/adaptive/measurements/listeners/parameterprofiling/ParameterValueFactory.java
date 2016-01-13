/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.adaptive.measurements.listeners.parameterprofiling;

import org.jikesrvm.adaptive.parameterprofiling.BooleanParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.ByteParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.CharParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.DoubleParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.FloatParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.IntParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.LongParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.NullParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.ShortParameterValue;
import org.jikesrvm.adaptive.parameterprofiling.TypeValueForObjectParameter;
import org.jikesrvm.classloader.RVMType;

public interface ParameterValueFactory {

  BooleanParameterValue createBooleanParameter(boolean b);

  ByteParameterValue createByteParameter(byte byteValue);

  CharParameterValue createCharParameter(char charValue);

  DoubleParameterValue createDoubleParameter(double doubleValue);

  FloatParameterValue createFloatParameter(float floatValue);

  IntParameterValue createIntParameter(int intValue);

  LongParameterValue createLongParameter(long longValue);

  ShortParameterValue createShortParameter(short shortValue);

  TypeValueForObjectParameter createTypeValueForObjectParameter(RVMType type);

  NullParameterValue createNullParameter();

}

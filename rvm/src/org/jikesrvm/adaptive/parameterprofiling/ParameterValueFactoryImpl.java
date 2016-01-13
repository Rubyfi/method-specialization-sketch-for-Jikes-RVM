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
package org.jikesrvm.adaptive.parameterprofiling;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterValueFactory;
import org.jikesrvm.classloader.RVMType;

public class ParameterValueFactoryImpl implements ParameterValueFactory {

  private BooleanParameterValue booleanForFalse = new BooleanParameterValue(false);
  private BooleanParameterValue booleanForTrue = new BooleanParameterValue(true);

  @Override
  public BooleanParameterValue createBooleanParameter(boolean b) {
    BooleanParameterValue booleanParameter = (b) ? booleanForTrue : booleanForFalse;
    return booleanParameter;
  }

  @Override
  public ByteParameterValue createByteParameter(byte byteValue) {
    ByteParameterValue byteParameter = new ByteParameterValue(byteValue);
    return byteParameter;
  }

  @Override
  public CharParameterValue createCharParameter(char charValue) {
    CharParameterValue charParameter = new CharParameterValue(charValue);
    return charParameter;
  }

  @Override
  public DoubleParameterValue createDoubleParameter(double doubleValue) {
    DoubleParameterValue doubleParameter = new DoubleParameterValue(doubleValue);
    return doubleParameter;
  }

  @Override
  public FloatParameterValue createFloatParameter(float floatValue) {
    FloatParameterValue floatParameter = new FloatParameterValue(floatValue);
    return floatParameter;
  }

  @Override
  public LongParameterValue createLongParameter(long longValue) {
    LongParameterValue longParameter = new LongParameterValue(longValue);
    return longParameter;
  }

  @Override
  public IntParameterValue createIntParameter(int intValue) {
    IntParameterValue intParameter = new IntParameterValue(intValue);
    return intParameter;
  }

  @Override
  public ShortParameterValue createShortParameter(short shortValue) {
    ShortParameterValue shortParameter = new ShortParameterValue(shortValue);
    return shortParameter;
  }

  @Override
  public TypeValueForObjectParameter createTypeValueForObjectParameter(RVMType type) {
    TypeValueForObjectParameter typeParameter = new TypeValueForObjectParameter(type);
    return typeParameter;
  }

  @Override
  public NullParameterValue createNullParameter() {
    return NullParameterValue.NULL;
  }

}

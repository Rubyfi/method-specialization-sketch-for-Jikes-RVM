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

import java.util.Arrays;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.MethodDataProvider;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterValueFactory;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;

/**
 * Directly represents a profile that was taken for a given method execution.
 */
public class ParameterProfile implements ParameterProfileInformation {

  private int numberOfParameters;

  private AbstractParameterInfo[] parameterValues;

  private MethodDataProvider mdp;

  private int multiplicity = 1;

  private static final ParameterValueFactory paramValueFactory = new ParameterValueFactoryImpl();

  protected ParameterProfile(){
    // for NullParameterProfile
  }

  /**
   * NB for test cases only.
   * @param mdp source for profile data
   */
  public ParameterProfile(MethodDataProvider mdp) {
    int valueSize = mdp.getParameterTypes().length;
    parameterValues = new AbstractParameterInfo[valueSize];
    this.mdp = mdp;
  }

  public ParameterProfile(RVMMethod method) {
    int valueSize = method.getParameterTypes().length;
    // implicit this
    valueSize = (method.isStatic()) ? valueSize : valueSize + 1;
    parameterValues = new AbstractParameterInfo[valueSize];
    mdp = new MethodDataProviderImpl(method);
  }

  @Override
  public void addNewIntValue(int intValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createIntParameter(intValue);
  }

  @Override
  public void addNewByteValue(byte byteValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createByteParameter(byteValue);
  }

  @Override
  public void addNewCharValue(char charValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createCharParameter(charValue);
  }

  @Override
  public void addNewShortValue(short shortValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createShortParameter(shortValue);
  }

  @Override
  public void addNewLongValue(long longValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createLongParameter(longValue);
  }

  @Override
  public void addNewBooleanValue(boolean booleanValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createBooleanParameter(booleanValue);
  }

  @Override
  public void addNewDoubleValue(double doubleValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createDoubleParameter(doubleValue);
  }

  @Override
  public void addNewFloatValue(float floatValue) {
    parameterValues[numberOfParameters++] = paramValueFactory.createFloatParameter(floatValue);
  }

  @Override
  public void addNewType(RVMType objectType) {
    if (objectType == null) {
      parameterValues[numberOfParameters++] = NullParameterValue.NULL;
    } else {
      parameterValues[numberOfParameters++] = paramValueFactory.createTypeValueForObjectParameter(objectType);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (multiplicity != 1) {
      sb.append(Integer.toString(multiplicity));
      sb.append(" x ");
    }

    sb.append(mdp.getDeclaringClassDescriptor());
    sb.append(" ");
    sb.append(mdp.getMethodName());
    sb.append(" ");
    sb.append(mdp.getMethodDescriptor());
    sb.append(" ");

    int index = 0;
    if (mdp.isInstanceMethod()) {
      sb.append("{");
      sb.append(parameterValues[0].toString());
      sb.append("} ");
      index++;
    }

    for (; index < parameterValues.length; index++) {
      String s = parameterValues[index].toString();
      if (s == null) {
        sb.append("NULL");
      } else {
        sb.append(s);
      }

      sb.append(" ");
    }

    sb.setCharAt(sb.length() - 1, '\n');
    return sb.toString();
  }

  private boolean canBeMergedWith(ParameterProfile secondProfile) {
    return Arrays.deepEquals(parameterValues, secondProfile.parameterValues);
  }

  public boolean mergeWith(ParameterProfile secondProfile) {
    if (this != secondProfile && canBeMergedWith(secondProfile)) {
      multiplicity++;
      return true;
    }
    return false;
  }

  /* Methods currently used only in testcases */

  String getValueOfParameter(int i) {
    return parameterValues[i].toString();
  }

  boolean isComplete() {
    return numberOfParameters == mdp.getParameterTypes().length;
  }

  int multiplicity() {
    return multiplicity;
  }

}

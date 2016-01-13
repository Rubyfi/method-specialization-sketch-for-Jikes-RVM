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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.MethodDataProvider;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.util.Pair;

public class MethodProfile implements ParameterProfileInformation {

  private static final PairComparator pairComp = new PairComparator();

  private Map<Integer, Map<AbstractParameterInfo, Integer>> parameters;

  private Integer currentParam;

  private final MethodDataProvider methodData;

  private final CandidateType candidateType;

  public enum CandidateType {
    VALUES_ONLY, TYPES_ONLY, ALL,
  }

  public MethodProfile(MethodDataProvider methodData) {
    this(methodData, CandidateType.ALL);
  }

  public MethodProfile(MethodDataProvider methodData, CandidateType candidateType) {
    this.methodData = methodData;
    parameters = new HashMap<Integer, Map<AbstractParameterInfo, Integer>>();
    for (int i = 0; i < methodData.getParameterCountIncludingThis(); i++) {
      parameters.put(Integer.valueOf(i), new HashMap<AbstractParameterInfo, Integer>());
    }
    currentParam = Integer.valueOf(0);

    this.candidateType = candidateType;
  }

  @Override
  public void addNewByteValue(byte byteValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    ByteParameterValue bpv = new ByteParameterValue(byteValue);
    increaseCountForParameterValue(currentParamMap, bpv);

    switchToNextParameter();
  }

  @Override
  public void addNewCharValue(char charValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    CharParameterValue cpv = new CharParameterValue(charValue);
    increaseCountForParameterValue(currentParamMap, cpv);

    switchToNextParameter();
  }

  @Override
  public void addNewIntValue(int i) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    IntParameterValue ipv = new IntParameterValue(i);
    increaseCountForParameterValue(currentParamMap, ipv);

    switchToNextParameter();
  }

  @Override
  public void addNewLongValue(long l) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    LongParameterValue lpv = new LongParameterValue(l);
    increaseCountForParameterValue(currentParamMap, lpv);

    switchToNextParameter();
  }

  @Override
  public void addNewShortValue(short shortValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    ShortParameterValue spv = new ShortParameterValue(shortValue);
    increaseCountForParameterValue(currentParamMap, spv);

    switchToNextParameter();
  }

  @Override
  public void addNewBooleanValue(boolean booleanValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    BooleanParameterValue bpv = new BooleanParameterValue(booleanValue);
    increaseCountForParameterValue(currentParamMap, bpv);

    switchToNextParameter();
  }

  @Override
  public void addNewDoubleValue(double doubleValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    DoubleParameterValue dpv = new DoubleParameterValue(doubleValue);
    increaseCountForParameterValue(currentParamMap, dpv);

    switchToNextParameter();
  }

  @Override
  public void addNewFloatValue(float floatValue) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    FloatParameterValue fpv = new FloatParameterValue(floatValue);
    increaseCountForParameterValue(currentParamMap, fpv);

    switchToNextParameter();
  }

  @Override
  public void addNewType(RVMType objectType) {
    Map<AbstractParameterInfo, Integer> currentParamMap = parameters.get(currentParam);

    AbstractParameterInfo abstractParamInfo = null;
    if (objectType == null) {
      abstractParamInfo = NullParameterValue.NULL;
    } else {
      abstractParamInfo = new TypeValueForObjectParameter(objectType);
    }

    increaseCountForParameterValue(currentParamMap, abstractParamInfo);

    switchToNextParameter();
  }

  protected void increaseCountForParameterValue(Map<AbstractParameterInfo, Integer> currentParamMap, AbstractParameterInfo apv) {
    Integer count = currentParamMap.get(apv);
    if (count == null) {
      count = Integer.valueOf(1);
    } else {
      count = Integer.valueOf(++count);
    }
    currentParamMap.put(apv, count);
  }

  private void switchToNextParameter() {
    int nextValue = currentParam.intValue() + 1;
    if (nextValue >= methodData.getParameterCountIncludingThis()) {
      nextValue = 0;
    }
    currentParam = Integer.valueOf(nextValue);
  }

  public List<Pair<AbstractParameterInfo, Integer>> getDataForParameter(int i) {
    Map<AbstractParameterInfo, Integer> parameterMap = parameters.get(i);

    List<Pair<AbstractParameterInfo, Integer>> list = new LinkedList<Pair<AbstractParameterInfo, Integer>>();
    for (Entry<AbstractParameterInfo, Integer> e : parameterMap.entrySet()) {
      Pair<AbstractParameterInfo, Integer> pair = new Pair<AbstractParameterInfo, Integer>(e.getKey(), e.getValue());
      list.add(pair);
    }
    Collections.sort(list, pairComp);

    return list;
  }

  public List<Pair<AbstractParameterInfo, Integer>> getCandidatesForParameter(int i) {
    List<Pair<AbstractParameterInfo, Integer>> data = getDataForParameter(i);

    Iterator<Pair<AbstractParameterInfo, Integer>> iterator = data.iterator();

    Pair<AbstractParameterInfo, Integer> pair = null;

    while (iterator.hasNext()) {
      pair = iterator.next();

      boolean hasTypeInfo = pair.first.hasTypeInformation();
      if (!hasTypeInfo && candidateType == CandidateType.TYPES_ONLY) {
        return Collections.emptyList();
      } else  if (hasTypeInfo && candidateType == CandidateType.VALUES_ONLY) {
        return Collections.emptyList();
      }

      removeTypesThatAreNotMorePreciseThanSignature(i, iterator, pair);
    }

    return data;
  }

  protected void removeTypesThatAreNotMorePreciseThanSignature(int i, Iterator<Pair<AbstractParameterInfo, Integer>> iterator, Pair<AbstractParameterInfo, Integer> pair) {
    if (pair.first.hasTypeInformation()) {
      TypeValueForObjectParameter tvfop = (TypeValueForObjectParameter) pair.first;
      int offset = methodData.isInstanceMethod() ? -1 : 0;
      int matchingTypeParam = i + offset;
      TypeReference typeRefOfSpecializedParameter = tvfop.getObjectType().getTypeRef();
      TypeReference typeRefFromSignature = methodData.getParameterTypes()[matchingTypeParam];
      if (typeRefOfSpecializedParameter == typeRefFromSignature) {
        iterator.remove();
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder toString = new StringBuilder();
    toString.append("------ START PROFILE of ");
    toString.append(methodData.getMethodName());
    toString.append(" , ");
    toString.append(methodData.getMethodDescriptor());
    toString.append("\n");
    int paramCount = methodData.getParameterCountIncludingThis();
    for (int param = 0; param < paramCount; param++) {
      toString.append("Parameter ");
      toString.append(param);
      if (param == 0 && (methodData.isInstanceMethod())) {
        toString.append(" (implicit this) ");
      }
      toString.append(": ");
      List<Pair<AbstractParameterInfo, Integer>> paramInfo = getDataForParameter(param);
      for (Pair<AbstractParameterInfo, Integer> infoForParamValue : paramInfo) {
        toString.append(infoForParamValue.second);
        toString.append(" x ");
        toString.append(infoForParamValue.first);
        toString.append(" | ");
      }
      toString.append("\n");
    }
    toString.append("------ END PROFILE\n");
    return toString.toString();
  }

}

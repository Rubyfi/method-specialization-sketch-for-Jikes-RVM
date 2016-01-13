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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.MethodDataProvider;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.junit.runners.RequiresBootstrapVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.jikesrvm.util.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(VMRequirements.class)
@Category(RequiresBootstrapVM.class)
public class MethodProfileTest {

  private static final int ONE = 1;
  private static final int TEN = 10;
  private MethodDataProvider methodData;
  private MethodProfile mp;
  private List<Pair<AbstractParameterInfo, Integer>> listForCurrentParameter;

  @Before
  public void setup() {
    methodData = mock(MethodDataProvider.class);
    TypeReference[] singleParameter = new TypeReference[1];
    when(methodData.getParameterTypes()).thenReturn(singleParameter);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(1);

    mp = new MethodProfile(methodData);
  }

  @Test
  public void oneCandidateIsSaved() {
    mp.addNewIntValue(ONE);
    getAndVerifyList(0, 1);
    assertFirstEntryIsOneWithCount(1);
  }

  @Test
  public void multipleCandidatesAreCounted() throws Exception {
    addOneTwoTimes();
    getAndVerifyList(0, 1);
    assertFirstEntryIsOneWithCount(2);
  }

  protected Pair<AbstractParameterInfo, Integer> getEntry(int entryNumber) {
    Pair<AbstractParameterInfo, Integer> firstEntry = listForCurrentParameter.get(entryNumber);
    return firstEntry;
  }

  protected void assertFirstEntryIsOneWithCount(int count) {
    Pair<AbstractParameterInfo, Integer> firstEntry = getEntry(0);
    assertEquals(new IntParameterValue(ONE), firstEntry.first);
    assertThat(firstEntry.second, is(count));
  }


  @Test
  public void candidatesAreSorted() throws Exception {
    mp.addNewIntValue(TEN);
    addOneTwoTimes();

    getAndVerifyList(0, 2);

    assertFirstEntryIsOneWithCount(2);
    Pair<AbstractParameterInfo, Integer> secondEntry = getEntry(1);
    assertEquals(new IntParameterValue(TEN), secondEntry.first);
    assertThat(secondEntry.second, is(1));
  }

  protected void addOneTwoTimes() {
    mp.addNewIntValue(ONE);
    mp.addNewIntValue(ONE);
  }

  @Test
  public void multipleParametersAreSupported() throws Exception {
    TypeReference[] multipleParameters = new TypeReference[2];
    when(methodData.getParameterTypes()).thenReturn(multipleParameters);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(2);
    mp = new MethodProfile(methodData);

    mp.addNewIntValue(ONE);
    mp.addNewIntValue(ONE);

    getAndVerifyList(0, 1);
    assertFirstEntryIsOneWithCount(1);

    getAndVerifyList(1, 1);
    // c&p
    Pair<AbstractParameterInfo, Integer> firstEntry = getEntry(0);
    assertEquals(new IntParameterValue(ONE), firstEntry.first);
    assertThat(firstEntry.second, is(1));
  }

  protected void getAndVerifyList(int parameterNumber, int expectedListSize) {
    listForCurrentParameter = mp.getDataForParameter(parameterNumber);
    assertThat(listForCurrentParameter.size(), is(expectedListSize));
  }

  @Test
  public void multipleParametersWithDifferentTypesAreSupported() throws Exception {
    TypeReference[] twoParameters = new TypeReference[2];
    when(methodData.getParameterTypes()).thenReturn(twoParameters);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(2);
    mp = new MethodProfile(methodData);

    long l = 12345678910L;
    mp.addNewIntValue(ONE);
    mp.addNewLongValue(l);
    mp.addNewIntValue(ONE);
    mp.addNewLongValue(l);
    mp.addNewIntValue(TEN);
    mp.addNewLongValue(l);

    getAndVerifyList(0, 2);
    assertFirstEntryIsOneWithCount(2);

    getAndVerifyList(1, 1);
    Pair<AbstractParameterInfo, Integer> firstEntry = getEntry(0);
    assertEquals(new LongParameterValue(l), firstEntry.first);
    assertThat(firstEntry.second, is(3));
  }

  @Test
  public void allStandardPrimitiveTypesAreSupported() throws Exception {
    TypeReference[] sevenParameters = new TypeReference[6];
    when(methodData.getParameterTypes()).thenReturn(sevenParameters);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(6);
    mp = new MethodProfile(methodData);

    mp.addNewBooleanValue(true);
    byte byteValue = 127;
    mp.addNewByteValue(byteValue);
    char charValue = '-';
    mp.addNewCharValue(charValue);
    double doubleValue = -4.529843309240932123928409324E120d;
    mp.addNewDoubleValue(doubleValue);
    float floatValue = 1.24367E-10f;
    mp.addNewFloatValue(floatValue);
    short shortValue = -10;
    mp.addNewShortValue(shortValue);

    mp.addNewBooleanValue(true);
    mp.addNewByteValue(byteValue);
    mp.addNewCharValue(charValue);
    mp.addNewDoubleValue(doubleValue);
    mp.addNewFloatValue(floatValue);
    mp.addNewShortValue(shortValue);

    mp.addNewBooleanValue(false);
    byte otherByteValue = (byte) -127;
    mp.addNewByteValue(otherByteValue);
    char otherCharValue = '+';
    mp.addNewCharValue(otherCharValue);
    double otherDoubleValue = 124.810347835E-100d;
    mp.addNewDoubleValue(otherDoubleValue);
    float otherFloatValue = 0.00002349045E10f;
    mp.addNewFloatValue(otherFloatValue);
    short otherShortValue = (short) 30000;
    mp.addNewShortValue(otherShortValue);

    getAndVerifyList(0, 2);
    Pair<AbstractParameterInfo, Integer> firstEntry = getEntry(0);
    assertEquals(new BooleanParameterValue(true), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    Pair<AbstractParameterInfo, Integer> secondEntry = getEntry(1);
    assertEquals(new BooleanParameterValue(false), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    getAndVerifyList(1, 2);
    firstEntry = getEntry(0);
    assertEquals(new ByteParameterValue(byteValue), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    secondEntry = getEntry(1);
    assertEquals(new ByteParameterValue(otherByteValue), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    getAndVerifyList(2, 2);
    firstEntry = getEntry(0);
    assertEquals(new CharParameterValue(charValue), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    secondEntry = getEntry(1);
    assertEquals(new CharParameterValue(otherCharValue), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    getAndVerifyList(3, 2);
    firstEntry = getEntry(0);
    assertEquals(new DoubleParameterValue(doubleValue), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    secondEntry = getEntry(1);
    assertEquals(new DoubleParameterValue(otherDoubleValue), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    getAndVerifyList(4, 2);
    firstEntry = getEntry(0);
    assertEquals(new FloatParameterValue(floatValue), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    secondEntry = getEntry(1);
    assertEquals(new FloatParameterValue(otherFloatValue), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    getAndVerifyList(5, 2);
    firstEntry = getEntry(0);
    assertEquals(new ShortParameterValue(shortValue), firstEntry.first);
    assertThat(firstEntry.second, is(2));

    secondEntry = getEntry(1);
    assertEquals(new ShortParameterValue(otherShortValue), secondEntry.first);
    assertThat(secondEntry.second, is(1));

    System.out.println(mp.toString());
  }

  @Test
  public void testProfileThatIgnoresPrimitiveCandidates() throws Exception {
    methodData = mock(MethodDataProvider.class);
    TypeReference[] singleParameter = new TypeReference[1];
    when(methodData.getParameterTypes()).thenReturn(singleParameter);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(1);
    MethodProfile mp = new MethodProfile(methodData, MethodProfile.CandidateType.TYPES_ONLY);
    mp.addNewIntValue(0);
    List<Pair<AbstractParameterInfo, Integer>> candidatesForParameter = mp.getCandidatesForParameter(0);
    assertThat(candidatesForParameter.size(), is(0));
  }

  @Ignore("does not work on Jikes RVM because of mocking and does not work on other VMs because of Jikes RVM internals")
  @Test
  public void testProfileThatIgnoresReferenceCandidates() throws Exception {
    methodData = mock(MethodDataProvider.class);
    TypeReference[] singleParameter = new TypeReference[1];
    when(methodData.getParameterTypes()).thenReturn(singleParameter);
    when(methodData.isInstanceMethod()).thenReturn(false);
    when(methodData.getParameterCountIncludingThis()).thenReturn(1);
    MethodProfile mp = new MethodProfile(methodData, MethodProfile.CandidateType.VALUES_ONLY);

    RVMType type = mock(RVMType.class);

    mp.addNewType(type);
    List<Pair<AbstractParameterInfo, Integer>> candidatesForParameter = mp.getCandidatesForParameter(0);
    assertThat(candidatesForParameter.size(), is(0));
  }

}

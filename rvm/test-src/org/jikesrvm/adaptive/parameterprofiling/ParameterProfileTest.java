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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.MethodDataProvider;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.junit.runners.RequiresBootstrapVM;
import org.jikesrvm.junit.runners.RequiresBuiltJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(VMRequirements.class)
@Category(RequiresBootstrapVM.class)
public class ParameterProfileTest {

  private static final String atomString = "TEST";
  private static final int numberOfPrimitiveTypes = 8;
  private static final TypeReference[] ALL_PRIMITIVE_PARAMETER_CASES = new TypeReference[numberOfPrimitiveTypes];

  private MethodDataProvider mockProvider;

  private static final TypeReference[] TWO_PARAMETERS = new TypeReference[2];
  private static final TypeReference[] ONE_PARAMETER = new TypeReference[1];

  @Before
  public void setup() {
    mockProvider = mock(MethodDataProvider.class);
  }

  @Test
  public void testMethodWithSingleParameter() {
    when(mockProvider.getParameterTypes()).thenReturn(ONE_PARAMETER);

    ParameterProfile paraProfile = createNewProfile();
    paraProfile.addNewIntValue(20);
    assertTrue(paraProfile.isComplete());
  }

  protected ParameterProfile createNewProfile() {
    return new ParameterProfile(mockProvider);
  }

  @Test
  public void testMethodWithMultipleParameters() {
    when(mockProvider.getParameterTypes()).thenReturn(TWO_PARAMETERS);

    ParameterProfile paraProfile = createNewProfile();
    paraProfile.addNewIntValue(20);
    assertFalse(paraProfile.isComplete());
  }

  @Test
  public void testValueIsSavedCorrectly() {
    when(mockProvider.getParameterTypes()).thenReturn(ONE_PARAMETER);

    ParameterProfile paraProfile = createNewProfile();
    int intValue = 200;
    paraProfile.addNewIntValue(intValue);
    assertEquals(Integer.toString(intValue), paraProfile.getValueOfParameter(0));
  }

  @Test
  public void testMultipleValuesAreSavedCorrectly() {
    when(mockProvider.getParameterTypes()).thenReturn(TWO_PARAMETERS);

    ParameterProfile paraProfile = createNewProfile();
    int intValue = 200;
    int secondIntValue = Integer.MIN_VALUE;
    paraProfile.addNewIntValue(intValue);
    paraProfile.addNewIntValue(secondIntValue);
    assertEquals(Integer.toString(intValue), paraProfile.getValueOfParameter(0));
    assertEquals(Integer.toString(secondIntValue), paraProfile.getValueOfParameter(1));
  }

  @Test
  public void testValuesOfAllTypesAreSavedCorrectly() {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);

    ParameterProfile paraProfile = createNewProfile();
    int intValue = Integer.MAX_VALUE;
    paraProfile.addNewIntValue(intValue);
    assertEquals(Integer.toString(intValue), paraProfile.getValueOfParameter(0));

    byte byteValue = Byte.MAX_VALUE;
    paraProfile.addNewByteValue(byteValue);
    assertEquals(Byte.toString(byteValue), paraProfile.getValueOfParameter(1));

    char charValue = Character.MAX_VALUE;
    paraProfile.addNewCharValue(charValue);
    assertEquals(Character.toString(charValue), paraProfile.getValueOfParameter(2));

    short shortValue = Short.MIN_VALUE;
    paraProfile.addNewShortValue(shortValue);
    assertEquals(Short.toString(shortValue), paraProfile.getValueOfParameter(3));

    long longValue = Long.MIN_VALUE;
    paraProfile.addNewLongValue(longValue);
    assertEquals(Long.toString(longValue), paraProfile.getValueOfParameter(4));

    boolean booleanValue = true;
    paraProfile.addNewBooleanValue(booleanValue);
    assertEquals(Boolean.toString(booleanValue), paraProfile.getValueOfParameter(5));

    double doubleValue = Double.MAX_VALUE;
    paraProfile.addNewDoubleValue(doubleValue);
    assertEquals(Double.toString(doubleValue), paraProfile.getValueOfParameter(6));

    float floatValue = Float.MIN_VALUE;
    paraProfile.addNewFloatValue(floatValue);
    assertEquals(Float.toString(floatValue), paraProfile.getValueOfParameter(7));

    assertTrue(paraProfile.isComplete());
  }

  @Test
  public void testToString() {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    String className = "Lnet/sourceforge/someapp/SomeClass;";
    when(mockProvider.getDeclaringClassDescriptor()).thenReturn(className);
    String methodName = "strangeMethod";
    when(mockProvider.getMethodName()).thenReturn(methodName);
    String methodDescriptor = "(IBCSZJDF)V";
    when(mockProvider.getMethodDescriptor()).thenReturn(methodDescriptor);

    ParameterProfile paraProfile = createNewProfile();
    int intValue = Integer.MAX_VALUE;
    byte byteValue = Byte.MAX_VALUE;
    char charValue = Character.MAX_VALUE;
    short shortValue = Short.MIN_VALUE;
    long longValue = Long.MIN_VALUE;
    boolean booleanValue = true;
    double doubleValue = Double.MAX_VALUE;
    float floatValue = Float.MIN_VALUE;
    addValuesToProfile(paraProfile, intValue, byteValue, charValue, shortValue, longValue, booleanValue, doubleValue, floatValue);

    StringBuilder expectedResultOfToString = new StringBuilder();
    buildExpectedPartForToString(className, methodName, methodDescriptor, intValue, byteValue, charValue, shortValue, longValue,
        booleanValue, doubleValue, floatValue, expectedResultOfToString);

    assertEquals(expectedResultOfToString.toString(), paraProfile.toString());
  }

  @Test
  public void testToStringWithMultiplicityOfTwo() throws Exception {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    String className = "Lnet/sourceforge/someapp/SomeClass;";
    when(mockProvider.getDeclaringClassDescriptor()).thenReturn(className);
    String methodName = "strangeMethod";
    when(mockProvider.getMethodName()).thenReturn(methodName);
    String methodDescriptor = "(IBCSZJDF)V";
    when(mockProvider.getMethodDescriptor()).thenReturn(methodDescriptor);

    ParameterProfile paraProfile = createNewProfile();
    int intValue = Integer.MAX_VALUE;
    byte byteValue = Byte.MAX_VALUE;
    char charValue = Character.MAX_VALUE;
    short shortValue = Short.MIN_VALUE;
    long longValue = Long.MIN_VALUE;
    boolean booleanValue = true;
    double doubleValue = Double.MAX_VALUE;
    float floatValue = Float.MIN_VALUE;
    addValuesToProfile(paraProfile, intValue, byteValue, charValue, shortValue, longValue, booleanValue, doubleValue, floatValue);

    ParameterProfile secondProfile = createNewProfile();
    addValuesToProfile(secondProfile, intValue, byteValue, charValue, shortValue, longValue, booleanValue, doubleValue, floatValue);

    paraProfile.mergeWith(secondProfile);

    StringBuilder expectedResultOfToString = new StringBuilder();
    expectedResultOfToString.append("2 x ");
    buildExpectedPartForToString(className, methodName, methodDescriptor, intValue, byteValue, charValue, shortValue, longValue,
        booleanValue, doubleValue, floatValue, expectedResultOfToString);

    assertEquals(expectedResultOfToString.toString(), paraProfile.toString());
  }

  private void buildExpectedPartForToString(String className, String methodName, String methodDescriptor, int intValue,
      byte byteValue, char charValue, short shortValue, long longValue, boolean booleanValue, double doubleValue, float floatValue,
      StringBuilder expectedResultOfToString) {
    expectedResultOfToString.append(className);
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(methodName);
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(methodDescriptor);
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Integer.toString(intValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Byte.toString(byteValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Character.toString(charValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Short.toString(shortValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Long.toString(longValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Boolean.toString(booleanValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Double.toString(doubleValue));
    expectedResultOfToString.append(" ");
    expectedResultOfToString.append(Float.toString(floatValue));
    expectedResultOfToString.append("\n");
  }

  private void addValuesToProfile(ParameterProfile paraProfile, int intValue, byte byteValue, char charValue, short shortValue,
      long longValue, boolean booleanValue, double doubleValue, float floatValue) {
    paraProfile.addNewIntValue(intValue);
    paraProfile.addNewByteValue(byteValue);
    paraProfile.addNewCharValue(charValue);
    paraProfile.addNewShortValue(shortValue);
    paraProfile.addNewLongValue(longValue);
    paraProfile.addNewBooleanValue(booleanValue);
    paraProfile.addNewDoubleValue(doubleValue);
    paraProfile.addNewFloatValue(floatValue);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void testAddingObjectType() throws Exception {
    when(mockProvider.getParameterTypes()).thenReturn(ONE_PARAMETER);
    ParameterProfile paraProfile = createNewProfile();

    RVMType objectType = mock(RVMType.class);
    Atom a = Atom.findOrCreateAsciiAtom(atomString);
    when(objectType.getDescriptor()).thenReturn(a);

    paraProfile.addNewType(objectType);
    assertEquals(objectType.getDescriptor().toString(), paraProfile.getValueOfParameter(8));
  }

  @Test
  public void testAddingNull() throws Exception {
    when(mockProvider.getParameterTypes()).thenReturn(ONE_PARAMETER);
    ParameterProfile paraProfile = createNewProfile();

    paraProfile.addNewType(null);
    assertEquals("NULL", paraProfile.getValueOfParameter(0));
  }

  @Test
  public void testMultiplicityOfProfile() {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    ParameterProfile firstProfile = createNewProfile();
    assertEquals(1, firstProfile.multiplicity());
  }

  @Test
  public void testMergingOfEqualProfiles() {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    ParameterProfile firstProfile = createNewProfile();
    assertThat(firstProfile.multiplicity(), is(1));

    ParameterProfile secondProfile = createNewProfile();
    assertTrue(firstProfile.mergeWith(secondProfile));
    assertThat(firstProfile.multiplicity(), is(2));
  }

  @Test
  public void testDifferentProfilesAreNotMerged() throws Exception {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    ParameterProfile firstProfile = createNewProfile();

    ParameterProfile secondProfile = createNewProfile();
    secondProfile.addNewBooleanValue(false);
    assertFalse(firstProfile.mergeWith(secondProfile));
    assertThat(firstProfile.multiplicity(), is(1));
  }

  @Test
  public void testProfilesAreNotMergedWithSelf() throws Exception {
    when(mockProvider.getParameterTypes()).thenReturn(ALL_PRIMITIVE_PARAMETER_CASES);
    ParameterProfile firstProfile = createNewProfile();
    firstProfile.mergeWith(firstProfile);
    assertThat(firstProfile.multiplicity(), is(1));
  }

}

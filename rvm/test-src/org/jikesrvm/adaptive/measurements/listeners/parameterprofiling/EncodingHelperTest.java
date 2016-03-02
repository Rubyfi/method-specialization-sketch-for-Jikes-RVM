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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterStorage.ErrorFlag;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.junit.runners.RequiresBuiltJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.jikesrvm.runtime.JavaSizeConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(VMRequirements.class)
public class EncodingHelperTest {

  private static final int INITIAL_CAPACITY = 200;;
  private EncodingHelper encodingHelper;

  @Test
  public void encodeAndDecodeByte() {
    byte byteValue = (byte) -123;
    int startIndex = 0;
    encodeByte(byteValue, startIndex);
    prepareForDecoding();
    assertEquals(byteValue, decodeByte(startIndex));
    assertEquals(EncodingHelper.ErrorFlag.NO_ERROR, retrieveErrorFlag());
  }

  @Test
  public void encodeByteDoNotSwitchModeAndTryToDecode() {
    byte byteValue = (byte) 12;
    int startIndex = 0;
    encodeByte(byteValue, startIndex);
    byte decodedByte = decodeByte(startIndex);
    assertThatErrorFlagForStillInEncodingModeIsSet();
    assertEquals(Byte.MIN_VALUE, decodedByte);
  }

  @Test
  public void tryToEncodeByteInDecodingMode() throws Exception {
    byte byteValue = (byte) 12;
    int startIndex = 0;
    encodingHelper.switchToDecodeMode();
    encodeByte(byteValue, startIndex);
    assertEquals(EncodingHelper.ErrorFlag.STILL_IN_DECODING_MODE, retrieveErrorFlag());
  }

  protected void encodeByte(byte byteValue, int startIndex) {
    encodingHelper.encodeByte(startIndex, byteValue);
  }

  @Test
  public void encodeAndDecodeChar() {
    char c = (char) 200;
    int startIndex = 0;
    encodeChar(startIndex, c);
    prepareForDecoding();
    char decodedChar = decodeChar(startIndex);
    assertEquals(c, decodedChar);
  }

  @Test
  public void encodeAndDecodeShort() {
    short s = -30123;
    int startIndex = 0;
    encodeShort(startIndex, s);
    prepareForDecoding();
    short decodedShort = decodeShort(startIndex);
    assertEquals(s, decodedShort);
  }

  @Test
  public void encodeAndDecodePositiveShort() {
    short s = 32000;
    int startIndex = 0;
    encodeShort(startIndex, s);
    prepareForDecoding();
    short decodedShort = decodeShort(startIndex);
    assertEquals(s, decodedShort);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeFloat() throws Exception {
    float f = 0.1234f;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    prepareForDecoding();
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(f, decodedFloat, 0.0);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeFloatNan() throws Exception {
    float f = Float.NaN;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    prepareForDecoding();
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(f, decodedFloat, 0.0);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeFloatNegativeInf() throws Exception {
    float f = Float.NEGATIVE_INFINITY;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    prepareForDecoding();
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(f, decodedFloat, 0.0);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeFloatPositiveInf() throws Exception {
    float f = Float.POSITIVE_INFINITY;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    prepareForDecoding();
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(f, decodedFloat, 0.0);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeNegativeFloatValue() throws Exception {
    float f = -0.456f;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    prepareForDecoding();
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(f, decodedFloat, 0.0);
  }

  @Test
  public void encodeAndDecodeInt() throws Exception {
    int i = -12009350;
    int startIndex = 0;
    encodeInt(startIndex, i);
    prepareForDecoding();
    int decodededInt = decodeInt(startIndex);
    assertEquals(i, decodededInt);
  }

  @Test
  public void encodeAndDecodeTrue() {
    boolean trueValue = true;
    int startIndex = 0;
    encodingHelper.encodeBoolean(startIndex, trueValue);
    prepareForDecoding();
    boolean decodedBoolean = encodingHelper.decodeBoolean(startIndex);
    assertEquals(trueValue, decodedBoolean);
  }

  @Test
  public void encodeAndDecodeFalse() {
    boolean trueValue = false;
    int startIndex = 0;
    encodingHelper.encodeBoolean(startIndex, trueValue);
    boolean decodedBoolean = encodingHelper.decodeBoolean(startIndex);
    assertEquals(trueValue, decodedBoolean);
  }

  protected void prepareForDecoding() {
    encodingHelper.switchToDecodeMode();
  }

  /**
   * @param startIndex
   * @return
   */
  protected byte decodeByte(int startIndex) {
    return encodingHelper.decodeByte(startIndex);
  }

  @Test
  public void encodeCharDoNotSwitchModeAndTryToDecode() {
    char c = (char) 200;
    int startIndex = 0;
    encodeChar(startIndex, c);
    char decodedChar = decodeChar(startIndex);
    assertEquals(Character.MIN_VALUE, decodedChar);
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Test
  public void encodeShortDoNotSwitchModeAndTryToDecode() {
    short s = 32000;
    int startIndex = 0;
    encodeShort(startIndex, s);
    short decodedShort = decodeShort(startIndex);
    assertEquals(Short.MIN_VALUE, decodedShort);
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeFloatDoNotSwitchModeAndTryToDecode() {
    float f = -12.45f;
    int startIndex = 0;
    encodeFloat(startIndex, f);
    float decodedFloat = decodeFloat(startIndex);
    assertEquals(Float.MIN_VALUE, decodedFloat, 0.0);
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Test
  public void encodeIntDoNotSwitchModeAndTryToDecode() {
    int i = Integer.MAX_VALUE - 2000;
    int startIndex = 0;
    encodeInt(startIndex, i);
    int decodededInt = decodeInt(startIndex);
    assertEquals(Integer.MIN_VALUE, decodededInt);
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Test
  public void encodeTrueDoNotSwitchModeAndTryToDecode() {
    boolean trueValue = true;
    int startIndex = 0;
    encodingHelper.encodeBoolean(startIndex, trueValue);
    boolean decodedBoolean = encodingHelper.decodeBoolean(startIndex);
    assertEquals(false, decodedBoolean);
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  protected void assertThatErrorFlagForStillInEncodingModeIsSet() {
    assertEquals(EncodingHelper.ErrorFlag.STILL_IN_ENCODING_MODE, retrieveErrorFlag());
  }

  protected ErrorFlag retrieveErrorFlag() {
    return encodingHelper.getErrorFlag();
  }

  protected void encodeChar(int startIndex, char c) {
    encodingHelper.encodeChar(startIndex, c);
  }

  protected char decodeChar(int startIndex) {
    return encodingHelper.decodeChar(startIndex);
  }

  protected void encodeShort(int startIndex, short s) {
    encodingHelper.encodeShort(startIndex, s);
  }

  protected short decodeShort(int startIndex) {
    short decodedShort = encodingHelper.decodeShort(startIndex);
    return decodedShort;
  }

  protected void encodeFloat(int startIndex, float f) {
    encodingHelper.encodeFloat(startIndex, f);
  }

  protected float decodeFloat(int startIndex) {
    float decodedFloat = encodingHelper.decodeFloat(startIndex);
    return decodedFloat;
  }

  protected void encodeInt(int startIndex, int i) {
    encodingHelper.encodeInt(startIndex, i);
  }

  protected int decodeInt(int startIndex) {
    int decodededInt = encodingHelper.decodeInt(startIndex);
    return decodededInt;
  }

  @Test
  public void encodeAndDecodeLong() throws Exception {
    long l = Long.MAX_VALUE - Integer.MAX_VALUE * 5 - 123;
    int startIndex = 0;
    encodingHelper.encodeLong(startIndex, l);
    prepareForDecoding();
    long decodedLong = encodingHelper.decodeLong(startIndex);
    assertThat(decodedLong, is(l));
  }

  @Test
  public void encodeLongAndDoNotSwitchMode() throws Exception {
    long l = 29835987439636L;
    int startIndex = 0;
    encodingHelper.encodeLong(startIndex, l);
    long decodedLong = encodingHelper.decodeLong(startIndex);
    assertThat(decodedLong, is(Long.MIN_VALUE));
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeDouble() throws Exception {
    double d = 1234.34E-120d;
    int startIndex = 0;
    encodingHelper.encodeDouble(startIndex, d);
    prepareForDecoding();
    double decodedDouble = encodingHelper.decodeDouble(startIndex);
    assertEquals(d, decodedDouble, 0.0);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeDoubleAndDoNotSwitchMode() throws Exception {
    double d = -2.000003E200d;
    int startIndex = 0;
    encodingHelper.encodeDouble(startIndex, d);
    double decodedDouble = encodingHelper.decodeDouble(startIndex);
    assertThat(decodedDouble, is(Double.MIN_VALUE));
    assertThatErrorFlagForStillInEncodingModeIsSet();
  }

  @Test
  public void testEncodingHelperReset() {
    long l = 12345678910111213L;
    int startIndex = 0;
    encodingHelper.encodeLong(startIndex, l);
    encodingHelper.decodeLong(startIndex);
    encodingHelper.reset();
    assertEquals(EncodingHelper.ErrorFlag.NO_ERROR, retrieveErrorFlag());
    assertThatEncodingWorks(l);
  }

  protected void assertThatEncodingWorks(long l) {
    int startIndex = 0;
    encodingHelper.encodeLong(startIndex, l);
    prepareForDecoding();
    long decodedLong = encodingHelper.decodeLong(startIndex);
    assertEquals(l, decodedLong);
  }

  @Test
  public void testConsistencyChecks() {
    long l = Integer.MIN_VALUE - 1;
    int indexForLong = 0;
    encodingHelper.encodeLong(indexForLong, l);
    int indexForInt = 8;
    encodingHelper.encodeInt(indexForInt, 120);
    prepareForDecoding();
    encodingHelper.decodeLong(indexForLong);
    encodingHelper.decodeInt(indexForInt);
    assertTrue(encodingHelper.numberOfEncodingsAndDecodingsMatches());
  }


  @Test
  public void testConsistencyChecksForNonMatchingCalls() {
    long l = Integer.MIN_VALUE - 1;
    int indexForLong = 0;
    encodingHelper.encodeLong(indexForLong, l);
    encodingHelper.encodeInt(indexForLong + 8, 120);
    prepareForDecoding();
    encodingHelper.decodeLong(indexForLong);
    assertFalse(encodingHelper.numberOfEncodingsAndDecodingsMatches());
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeAndDecodeRVMType() throws Exception {
    RVMType type = getAResolvedType();
    int index = 0;
    encodingHelper.encodeType(index, type);
    prepareForDecoding();
    RVMType decodedType = encodingHelper.decodeType(index);
    assertEquals(type, decodedType);
  }

  private RVMType getAResolvedType() {
    RVMType type = TypeReference.findOrCreate("Lorg/jikesrvm/classloader/Atom;").peekType();
    assertNotNull(type);
    return type;
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void encodeRVMTypeAndDoNotSwitchForDecoding() throws Exception {
    RVMType type = getAResolvedType();
    int index = 0;
    encodingHelper.encodeType(index, type);
    RVMType decodedType = encodingHelper.decodeType(index);
    assertNull(decodedType);
  }

  @Before
  public void setUpForNewTest() {
    encodingHelper = new EncodingHelper(INITIAL_CAPACITY);
  }

  @Category(RequiresBuiltJikesRVM.class)
  @Test
  public void testBytesForTypeReference() {
    TypeReference booleanTypeRef = TypeReference.Boolean;
    assertEquals(JavaSizeConstants.BYTES_IN_BOOLEAN, EncodingHelper.getBytesForTypeReference(booleanTypeRef));

    TypeReference byteTypeRef = TypeReference.Byte;
    assertEquals(JavaSizeConstants.BYTES_IN_BYTE, EncodingHelper.getBytesForTypeReference(byteTypeRef));

    TypeReference charTypeRef = TypeReference.Char;
    assertEquals(JavaSizeConstants.BYTES_IN_CHAR, EncodingHelper.getBytesForTypeReference(charTypeRef));

    TypeReference doubleTypeRef = TypeReference.Double;
    assertEquals(JavaSizeConstants.BYTES_IN_DOUBLE, EncodingHelper.getBytesForTypeReference(doubleTypeRef));

    TypeReference floatTypeRef = TypeReference.Float;
    assertEquals(JavaSizeConstants.BYTES_IN_FLOAT, EncodingHelper.getBytesForTypeReference(floatTypeRef));

    TypeReference intTypeRef = TypeReference.Int;
    assertEquals(JavaSizeConstants.BYTES_IN_INT, EncodingHelper.getBytesForTypeReference(intTypeRef));

    TypeReference shortTypeRef = TypeReference.Short;
    assertEquals(JavaSizeConstants.BYTES_IN_SHORT, EncodingHelper.getBytesForTypeReference(shortTypeRef));

    TypeReference longTypeRef = TypeReference.Long;
    assertEquals(JavaSizeConstants.BYTES_IN_LONG, EncodingHelper.getBytesForTypeReference(longTypeRef));

    TypeReference objectTypeRef = TypeReference.JavaLangClass;
    assertEquals(JavaSizeConstants.BYTES_IN_INT, EncodingHelper.getBytesForTypeReference(objectTypeRef));
  }

}

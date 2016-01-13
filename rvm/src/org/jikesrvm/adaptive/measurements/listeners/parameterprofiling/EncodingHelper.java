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

import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_BOOLEAN;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_BYTE;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_CHAR;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_DOUBLE;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_FLOAT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_INT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_LONG;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_SHORT;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.runtime.Magic;
import org.vmmagic.pragma.Uninterruptible;

/**
 * Encodes and decodes primitive values and types of objects. The calling class
 * is responsible for remembering which parameters were encoded in what order.
 * <p>
 *
 * Access to this class <b>MUST</b> be synchronized externally.
 * <p>
 *
 * By convention, all return values in error cases are the minimum values
 * allowed for the data type (<code>false</code> in the case of booleans).
 *
 * TODO rename this class
 */
@Uninterruptible
public final class EncodingHelper implements ParameterDecoder, ParameterEncoder {

  private Mode currentMode;

  private ErrorFlag currentError;

  /**
   * negative means more decodings, positive means more encodings
   */
  protected int encodeDecodeBalance;

  private byte[] encodedParameters;

  public EncodingHelper(int length) {
    encodedParameters = new byte[length];
    currentMode = Mode.ENCODING;
    currentError = ErrorFlag.NO_ERROR;
  }

  @Override
  public void switchToDecodeMode() {
    currentMode = Mode.DECODING;
  }

  @Override
  public ErrorFlag getErrorFlag() {
    return currentError;
  }

  @Override
  public void encodeByte(int startIndex, byte value) {
    checkThatEncodingModeIsActive();

    encodeDecodeBalance++;
    encodedParameters[startIndex] = value;
  }

  private void checkThatEncodingModeIsActive() {
    if (notInEncodingMode()) {
      setErrorFlagForStillInDecodingMode();
    }
  }

  @Override
  public byte decodeByte(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Byte.MIN_VALUE;
    }

    encodeDecodeBalance--;

    return encodedParameters[startIndex];
  }

  protected void setErrorFlagForStillInEncodingMode() {
    currentError = ErrorFlag.STILL_IN_ENCODING_MODE;
  }

  protected void setErrorFlagForStillInDecodingMode() {
    currentError = ErrorFlag.STILL_IN_DECODING_MODE;
  }

  /**
   * NOTE: Must match {@link ParameterListener#dumpParameters(org.vmmagic.unboxed.Address, org.jikesrvm.compilers.baseline.BaselineCompiledMethod, int)}
   * TODO move to type reference once I can run JUnit on the RVM
   * @param ref
   * @return
   */
  public static int getBytesForTypeReference(TypeReference ref) {
    if (ref.isBooleanType()) {
      return BYTES_IN_BOOLEAN;
    } else if (ref.isByteType()) {
      return BYTES_IN_BYTE;
    } else if (ref.isCharType()) {
      return BYTES_IN_CHAR;
    } else if (ref.isDoubleType()) {
      return BYTES_IN_DOUBLE;
    } else if (ref.isFloatType()) {
      return BYTES_IN_FLOAT;
    } else if (ref.isIntType()) {
      return BYTES_IN_INT;
    } else if (ref.isLongType()) {
      return BYTES_IN_LONG;
    } else if (ref.isShortType()) {
      return BYTES_IN_SHORT;
    } else if (ref.isReferenceType()) {
      // saved as type id, which is an int
      return BYTES_IN_INT;
    } else if (ref.isWordLikeType()) {
      //TODO currently not dumped
      return 0;
    } else if (ref.isCodeType()) {
      //TODO currently not dumped
      return 0;
    }

    if (VM.VerifyAssertions) {
      VM.sysWriteln(ref.getName());
      VM.sysFail("Could not determine size of type reference");
    }

    return 0;
  }

  /**
   * @param c
   */
  @Override
  public void encodeChar(int startIndex, char c) {
    checkThatEncodingModeIsActive();

    int temp = c;
    byte upperHalf = (byte) (temp >>> 8);
    byte lowerHalf = (byte) (temp & 0x00FF);

    encodeDecodeBalance++;

    encodedParameters[startIndex] = upperHalf;
    encodedParameters[startIndex + 1] = lowerHalf;
  }

  /**
   * @param startIndex
   * @return
   */
  @Override
  public char decodeChar(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Character.MIN_VALUE;
    }

    encodeDecodeBalance--;

    byte upperHalf = encodedParameters[startIndex];
    byte lowerHalf = encodedParameters[startIndex + 1];
    int temp = (upperHalf << 8) | (lowerHalf & 0x00FF);
    char c = (char) temp;
    return c;
  }

  /**
   * @param bool
   */
  @Override
  public void encodeBoolean(int startIndex, boolean bool) {
    checkThatEncodingModeIsActive();

    byte b = bool ? (byte) 0x00FF : (byte) 0x0000;
    encodeDecodeBalance++;
    encodedParameters[startIndex] = b;
  }

  @Override
  public boolean decodeBoolean(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return false;
    }

    encodeDecodeBalance--;

    byte temp = encodedParameters[startIndex];
    boolean b = ((temp & 0x00FF) == 0x00FF) ? true : false;
    return b;
  }

  @Override
  public void encodeShort(int startIndex, short s) {
    encodeChar(startIndex, (char) s);
  }

  @Override
  public short decodeShort(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Short.MIN_VALUE;
    }

    return (short) decodeChar(startIndex);
  }

  @Override
  public void encodeFloat(int startIndex, float f) {
    // Use magic because Float.floatToIntBits(...) is not uninterruptible
    int rawBits = Magic.floatAsIntBits(f);
    encode32Bits(startIndex, rawBits);
  }

  @Override
  public float decodeFloat(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Float.MIN_VALUE;
    }

    // Use magic because Float.intBitsToFloat(...) is not uninterruptible
    int rawBits = decode32Bits(startIndex);
    return Magic.intBitsAsFloat(rawBits);
  }

  @Override
  public void encodeInt(int startIndex, int i) {
    int rawBits = i;
    encode32Bits(startIndex, rawBits);
  }

  protected void encode32Bits(int startIndex, int rawBits) {
    checkThatEncodingModeIsActive();

    byte upperMostQuarter = (byte) (rawBits >> 24);
    byte secondUpperQuarter = (byte) (rawBits >> 16);
    byte secondLowestQuarter = (byte) (rawBits >> 8);
    byte lowestQuarter = (byte) (rawBits & 0x000000FF);

    encodeDecodeBalance++;

    encodedParameters[startIndex] = upperMostQuarter;
    encodedParameters[startIndex + 1] = secondUpperQuarter;
    encodedParameters[startIndex + 2] = secondLowestQuarter;
    encodedParameters[startIndex + 3] = lowestQuarter;
  }

  @Override
  public int decodeInt(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Integer.MIN_VALUE;
    }

    int rawBits = decode32Bits(startIndex);
    return rawBits;
  }

  protected int decode32Bits(int startIndex) {
    int upperMostQuarter = (encodedParameters[startIndex] << 24) & 0xFF000000;
    int secondUpperQuarter = (encodedParameters[startIndex + 1] << 16) & 0x00FF0000;
    int secondLowestQuarter = (encodedParameters[startIndex + 2] << 8) & 0x0000FF00;
    int lowestQuarter = (encodedParameters[startIndex + 3]) & 0x000000FF;

    encodeDecodeBalance--;

    int rawBits = upperMostQuarter | secondUpperQuarter | secondLowestQuarter | lowestQuarter;
    return rawBits;
  }

  @Override
  public void encodeLong(int startIndex, long l) {
    encode64Bits(startIndex, l);
  }

  protected void encode64Bits(int startIndex, long bits) {
    checkThatEncodingModeIsActive();

    byte eigthByte = (byte) (bits >> 56);
    byte seventhByte = (byte) (bits >> 48);
    byte sixthByte = (byte) (bits >> 40);
    byte fifthByte = (byte) (bits >> 32);
    byte fourthByte = (byte) (bits >> 24);
    byte thirdByte = (byte) (bits >> 16);
    byte secondByte = (byte) (bits >> 8);
    byte firstByte = (byte) (bits & 0x00000000000000FF);

    encodeDecodeBalance++;

    encodedParameters[startIndex] = eigthByte;
    encodedParameters[startIndex + 1] = seventhByte;
    encodedParameters[startIndex + 2] = sixthByte;
    encodedParameters[startIndex + 3] = fifthByte;
    encodedParameters[startIndex + 4] = fourthByte;
    encodedParameters[startIndex + 5] = thirdByte;
    encodedParameters[startIndex + 6] = secondByte;
    encodedParameters[startIndex + 7] = firstByte;
  }

  @Override
  public long decodeLong(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Long.MIN_VALUE;
    }

    return decode64Bits(startIndex);
  }

  protected boolean notInDecodingMode() {
    return currentMode != Mode.DECODING;
  }

  protected boolean notInEncodingMode() {
    return currentMode != Mode.ENCODING;
  }



  protected long decode64Bits(int startIndex) {
    long eightByte = ((long) encodedParameters[startIndex] << 56)  & 0xFF00000000000000L;;
    long seventhByte = ((long) encodedParameters[startIndex + 1] << 48) & 0x00FF000000000000L;
    long sixthByte =  ((long) encodedParameters[startIndex + 2] << 40) & 0x0000FF0000000000L;
    long fifthByte = ((long) encodedParameters[startIndex + 3] << 32) & 0x000000FF00000000L;
    long fourthByte = ((long) encodedParameters[startIndex + 4] << 24) & 0x00000000FF000000L;
    long thirdByte = ((long) encodedParameters[startIndex + 5] << 16) & 0x0000000000FF0000L;
    long secondByte = ((long) encodedParameters[startIndex + 6] << 8) & 0x000000000000FF00L;
    long firstByte = encodedParameters[startIndex + 7] & 0x00000000000000FFL;

    encodeDecodeBalance--;

    long rawBits = eightByte | seventhByte | sixthByte | fifthByte | fourthByte | thirdByte | secondByte | firstByte;
    return rawBits;
  }

  @Override
  public void encodeDouble(int startIndex, double d) {
    // Use magic because Double.doubleToLongBits(...) is not uninterruptible
    long bits = Magic.doubleAsLongBits(d);
    encode64Bits(startIndex, bits);
  }

  @Override
  public double decodeDouble(int startIndex) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return Double.MIN_VALUE;
    }

    // Use magic because Double.longBitsToDouble(...) is not uninterruptible
    return Magic.longBitsAsDouble(decode64Bits(startIndex));
  }

  @Override
  public void reset() {
    currentError = ErrorFlag.NO_ERROR;
    currentMode = Mode.ENCODING;
    encodeDecodeBalance = 0;
  }

  public Mode getCurrentMode() {
    return currentMode;
  }

  public ErrorFlag getCurrentError() {
    return currentError;
  }

  @Override
  public int getEncodeDecodeBalance() {
    return encodeDecodeBalance;
  }

  public byte[] getEncodedParameters() {
    return encodedParameters;
  }

  @Override
  public boolean numberOfEncodingsAndDecodingsMatches() {
    return encodeDecodeBalance == 0;
  }

  @Override
  public void encodeType(int index, RVMType type) {
    storeType(index, type);
  }

  protected void storeType(int index, RVMType type) {
    // Use Integer.MIN_VALUE to represent the value null
    // (type ids start at 1, see TypeReference.nextId)

    int typeId;
    if (type != null) {
      typeId = type.getTypeRef().getId();
    } else {
      typeId = Integer.MIN_VALUE;
    }

    encodeInt(index, typeId);
  }

  @Override
  public RVMType decodeType(int index) {
    if (notInDecodingMode()) {
      setErrorFlagForStillInEncodingMode();
      return null;
    }

    int typeId = decodeInt(index);
    if (typeId == Integer.MIN_VALUE) {
      return null;
    }

    TypeReference typeRef = TypeReference.getTypeRef(typeId);
    if (VM.VerifyAssertions) {
      VM._assert(typeId >= 1, "Type id was not >= 1!");
      VM._assert(typeRef != null, "Type ref was null!");
    }

    RVMType type = typeRef.peekType();
    if (type == null) {
      VM.sysFail("Saved type reference for an id was not resolved yet!");
    }
    return type;
  }

}

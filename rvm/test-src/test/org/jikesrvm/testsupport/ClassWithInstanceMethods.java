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
package test.org.jikesrvm.testsupport;



public class ClassWithInstanceMethods {

  public static final boolean BOOLEAN_RETURN_VALUE = true;
  public static final byte BYTE_RETURN_VALUE = (byte) -42;
  public static final char CHAR_RETURN_VALUE = '€';
  public static final double DOUBLE_RETURN_VALUE = -23.42E-120;
  public static final float FLOAT_RETURN_VALUE = -1.4567892E-20f;
  public static final int INT_RETURN_VALUE = -120000;
  public static final long LONG_RETURN_VALUE = -12345678987654321L;
  public static final short SHORT_RETURN_VALUE = -30102;
  public static final String STRING_RETURN_VALUE = "Success ...";
  private StringBuilder stringBuilder;

  public ClassWithInstanceMethods() {
    stringBuilder = new StringBuilder();
  }

  public void writeToStringBuilder(String[] args) {
    for (String s : args) {
      stringBuilder.append(s);
    }
  }

  public String getOutputForInstanceMethod() {
    return stringBuilder.toString();
  }


  public void manyParametersReturnsVoid(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
  }

  public boolean manyParametersReturnsBoolean(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return BOOLEAN_RETURN_VALUE;
  }

  public byte manyParametersReturnsByte(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Byte.valueOf(BYTE_RETURN_VALUE);
  }

  public char manyParametersReturnsChar(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Character.valueOf(CHAR_RETURN_VALUE);
  }

  public double manyParametersReturnsDouble(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Double.valueOf(DOUBLE_RETURN_VALUE);
  }

  public float manyParametersReturnsFloat(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Float.valueOf(FLOAT_RETURN_VALUE);
  }

  public int manyParametersReturnsInt(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Integer.valueOf(INT_RETURN_VALUE);
  }

  public long manyParametersReturnsLong(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Long.valueOf(LONG_RETURN_VALUE);
  }

  public short manyParametersReturnsShort(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return Short.valueOf(SHORT_RETURN_VALUE);
  }

  public String manyParametersReturnsString(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    printParameters(b, l, d, i, c, f, o, s, bt);
    return STRING_RETURN_VALUE;
  }

  protected void printParameters(boolean b, long l, double d, int i, char c, float f, Object o, short s, byte bt) {
    stringBuilder.append("Boolean was " + b);
    stringBuilder.append("Long was " + l);
    stringBuilder.append("Double was " + d);
    stringBuilder.append("Integer was " + i);
    stringBuilder.append("Character was " + c);
    stringBuilder.append("Float was " + f);
    stringBuilder.append("Object was " + o);
    stringBuilder.append("Short was " + s);
    stringBuilder.append("Byte was " + bt);
  }

}

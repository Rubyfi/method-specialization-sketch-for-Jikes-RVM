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
import org.jikesrvm.runtime.Magic;

/**
 * Provides methods that are used for testing in the tests for specialization.
 */
public class SpecializationTestClass {

  public static void doubleIsConstantMultipleParameters(int a, double d, int b) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + a);

    System.out.println("Parameter 1 is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter 1 is : " + d);

    // Param d is a double and 2 words wide -> next local is 3
    System.out.println("Parameter 2 is constant: " + Magic.isConstantLocal(3));
    System.out.println("Parameter 2 is : " + b);

  }

  public static void booleanIsConstant(boolean bool) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + bool);
  }

  public static void byteIsConstant(byte b) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + b);
  }

  public static void charIsConstant(char c) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + c);
  }

  public static void floatIsConstant(float f) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + f);
  }

  public static void intIsConstant(int i) {
    System.out.println("Parameter is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter is one : " + isOne(i));
    System.out.println("Parameter is constant and one: " + (Magic.isConstantLocal(0) && isOne(i)));
  }

  public static void intIsConstantMultipleParameters(int a, int b, int c) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + a);

    System.out.println("Parameter 1 is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter 1 is : " + b);

    System.out.println("Parameter 2 is constant: " + Magic.isConstantLocal(2));
    System.out.println("Parameter 2 is : " + c);
  }

  public void intIsConstantInstanceMethod(int i) {
    System.out.println("Parameter 1 (implicit this is 0) is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter is one : " + isOne(i));
    System.out.println("Parameter is constant and one: " + (Magic.isConstantLocal(1) && isOne(i)));
  }

  public synchronized void intIsConstantInstanceMethodSynchronized(int i) {
    System.out.println("Parameter 1 (implicit this is 0) is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter is one : " + isOne(i));
    System.out.println("Parameter is constant and one: " + (Magic.isConstantLocal(1) && isOne(i)));
  }

  public static void typeParamIsOfSpecificType(A klazz) {
    System.out.println("Parameter 0 is of a specialized type: " + Magic.isLocalWithSpecializedType(0));
  }

  public static void typeParamIsNull(A obj) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + obj);
  }

  public void typeParamIsOfSpecificTypeInstanceMethod(A klazz) {
    System.out.println("Parameter 1 (implicit this is 0) is of a specialized type: " + Magic.isLocalWithSpecializedType(1));
  }

  public static void shortIsConstant(short s) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + s);
  }

  public static int tailRecursiveFunction(int i) {
    if (i >= 10) {
      System.out.println("Done");
      return i;
    }
    System.out.println(i);
    return tailRecursiveFunction(i + 1);
  }

  public static int tailRecursiveFunctionWithTypeParameter(A a, int i) {
    if (i >= 10) {
      System.out.println("Done");
      return i;
    }
    if (i % 2 == 0) {
      System.out.println(i);
      return tailRecursiveFunctionWithTypeParameter(new B(), i + 1);
    } else {
      System.out.println(i);
      return tailRecursiveFunctionWithTypeParameter(new C(), i + 1);
    }
  }


  private static boolean isOne(int i) {
    return i == 1;
  }

  public static void longIsConstant(int a, long l, int b) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + a);

    System.out.println("Parameter 1 is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter 1 is : " + l);

    // Parameter 2 is a long and 2 words wide -> next local is 3
    System.out.println("Parameter 2 is constant: " + Magic.isConstantLocal(3));
    System.out.println("Parameter 2 is : " + b);
  }

  public static void longIsConstantWithParameterWriting(int a, long l, int b) {
    System.out.println("Parameter 0 is constant: " + Magic.isConstantLocal(0));
    System.out.println("Parameter 0 is : " + a);

    l = l + 1;

    System.out.println("Parameter 1 is constant: " + Magic.isConstantLocal(1));
    System.out.println("Parameter 1 is : " + l);

    // Parameter 2 is a long and 2 words wide -> next local is 3
    System.out.println("Parameter 2 is constant: " + Magic.isConstantLocal(3));
    System.out.println("Parameter 2 is : " + b);
  }

  public static void emptyMethodWithObjectParameter(Object obj) {

  }

  public void methodWithDoWhile(int begin, int end) {
    do {
      System.out.println(begin);
    } while (begin++ != end);
  }

}

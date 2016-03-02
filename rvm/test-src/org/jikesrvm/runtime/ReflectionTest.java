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
package org.jikesrvm.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.lang.reflect.JikesRVMSupport;
import java.lang.reflect.Method;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.jikesrvm.junit.runners.RequiresBuiltJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import test.org.jikesrvm.testsupport.ClassWithInstanceMethods;

@RunWith(VMRequirements.class)
@Category(RequiresBuiltJikesRVM.class)
// accesses Jikes RVM internals; loading classes from protected packages like java.lang.reflect
// is not allowed on normal JVMs
public class ReflectionTest {

  // TODO tests for other types of primitives

  private static final String REFLECTION_TEST_CLASS = "ReflectionTestClass";
  private static final String MAIN = "main";
  private static final String GET_OUTPUT_FOR_MAIN = "getOutputForMain";

  private static final String CLASS_WITH_INSTANCE_METHODS = "test.org.jikesrvm.testsupport.ClassWithInstanceMethods";
  private static final String WRITE_TO_STRING_BUILDER = "writeToStringBuilder";
  private static final String GET_OUTPUT_FOR_INSTANCE_METHOD = "getOutputForInstanceMethod";

  @Before
  public void restetReflectionSettings() {
    Reflection.bytecodeReflection = false;
    Reflection.cacheInvokerInJavaLangReflect = true;
  }

  @After
  public void resetTestClass() throws ClassNotFoundException, NoSuchMethodException {
    resetReflectionTestClass();
  }

  @Test
  public void needsCheckArgs() throws Exception {
    ReflectionBase invoker = null;
    assertThatCheckArgsIsUnneeded(invoker);

    Reflection.bytecodeReflection = true;
    assertThatCheckArgsIsUnneeded(invoker);

    Reflection.cacheInvokerInJavaLangReflect = false;
    assertThatCheckArgsIsUnneeded(invoker);

    Reflection.bytecodeReflection = false;
    assertThat(Reflection.needsCheckArgs(invoker), is(true));
  }

  protected void assertThatCheckArgsIsUnneeded(ReflectionBase invoker) {
    assertThat(Reflection.needsCheckArgs(invoker), is(false));
  }

  @Test
  public void testOutOfLineInvokeOfStaticMethod() throws Exception {
    assumeStandardConfig();

    String[] emptyArray = new String[0];
    Object[] otherArgs = { emptyArray };
    RVMMethod mainMethod = getMethodFromClass(REFLECTION_TEST_CLASS, MAIN, String[].class);
    Object result = Reflection.invoke(mainMethod, null, null, otherArgs, true);
    assertNull(result);

    String[] someStrings = { "a", "b", "cake", "pie" };
    Object[] nonEmptyArgs = { someStrings };
    Reflection.invoke(mainMethod, null, null, nonEmptyArgs, true);

    String resultString = getOutputForMain(null, nonEmptyArgs);
    assertThatResultStringMatches(someStrings, resultString);
  }

  protected void assertThatResultStringMatches(String[] someStrings, String resultString) {
    assertNotNull(resultString);

    StringBuilder expected = new StringBuilder();
    for (String s : someStrings) {
      expected.append(s);
    }
    assertEquals(expected.toString(), resultString);
  }

  @Test
  public void testInvocationOfInstanceMethod() throws Exception {
    assumeStandardConfig();

    boolean isNonvirtual = false;
    Object thisArg = new ClassWithInstanceMethods();
    String[] emptyArray = new String[0];
    Object[] otherArgs = { emptyArray };
    RVMMethod instanceMethod = getMethodFromClass(CLASS_WITH_INSTANCE_METHODS, WRITE_TO_STRING_BUILDER, String[].class);
    ReflectionBase invokerForInstanceMethod = instanceMethod.getInvoker();

    Object result = Reflection.invoke(instanceMethod, invokerForInstanceMethod, thisArg, otherArgs, isNonvirtual);
    assertNull(result);

    String[] someStrings = { "a", "b", "cake", "pie" };
    Object[] nonEmptyArgs = { someStrings };
    Reflection.invoke(instanceMethod, invokerForInstanceMethod, thisArg, nonEmptyArgs, isNonvirtual);

    RVMMethod getOutputForInstanceMethod = getMethodFromClass(CLASS_WITH_INSTANCE_METHODS, GET_OUTPUT_FOR_INSTANCE_METHOD);
    ReflectionBase invokerForGetOutput = getOutputForInstanceMethod.getInvoker();

    Object[] emptyArgs = {};
    String resultString = (String) Reflection.invoke(getOutputForInstanceMethod, invokerForGetOutput, thisArg, emptyArgs,
        isNonvirtual);

    assertThatResultStringMatches(someStrings, resultString);
  }

  protected void assumeVMBuildForGnuClasspath() {
    assumeThat(VM.BuildForGnuClasspath, is(true));
  }

  protected void assumeStandardReflectionSettings() {
    assumeThat(Reflection.bytecodeReflection, is(false));
    assumeThat(Reflection.cacheInvokerInJavaLangReflect, is(true));
  }

  protected Method getMainMethod(Class<?> klazz) throws NoSuchMethodException {
    Method reflectMainMethod = klazz.getDeclaredMethod(MAIN, String[].class);
    return reflectMainMethod;
  }

  @Test
  public void wrappingAndUnwrappingOfBooleans() throws Exception {
    int trueValue = 1;
    Boolean wrappedTrueValue = wrapBoolean(trueValue);
    assertThat(wrappedTrueValue, is(Boolean.TRUE));
    boolean unwrappedTrueValue = unwrapBoolean(wrappedTrueValue);
    assertThat(unwrappedTrueValue, is(true));
    int unwrappedTrueValueAsInt = unwrapBooleanAsInt(wrappedTrueValue);
    assertThat(unwrappedTrueValueAsInt, is(trueValue));

    assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(0);
    assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(Integer.MAX_VALUE);
    assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(Integer.MIN_VALUE);
    assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(-1);
    assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(2);
  }

  protected RVMMethod getMethodFromClass(String className, String methodName, Class<?>... methodTypes)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException {
    Class<?> klazz = Class.forName(className);
    Method method = klazz.getDeclaredMethod(methodName, methodTypes);
    RVMMethod rvmMethod = JikesRVMSupport.getMethodOf(method);
    return rvmMethod;
  }

  protected void assertThatIntForFalseIsWrappedAndUnwrappedCorrectly(int integerValue) {
    Boolean wrappedFalseValue = wrapBoolean(integerValue);
    assertThat(wrappedFalseValue, is(Boolean.FALSE));
    boolean unwrappedFalseValue = unwrapBoolean(wrappedFalseValue);
    assertThat(unwrappedFalseValue, is(false));
    int unwrappedFalseValueAsInt = unwrapBooleanAsInt(wrappedFalseValue);
    assertThat(unwrappedFalseValueAsInt, is(0));
  }

  protected Boolean wrapBoolean(int trueValue) {
    return (Boolean) Reflection.wrapBoolean(trueValue);
  }

  protected boolean unwrapBoolean(Boolean wrappedBoolean) {
    return Reflection.unwrapBoolean(wrappedBoolean);
  }

  protected int unwrapBooleanAsInt(Boolean wrappedBoolean) {
    return Reflection.unwrapBooleanAsInt(wrappedBoolean);
  }

  @Test
  public void testDirectInvocationOfCompiledMethod() throws Exception {
    assumeStandardConfig();

    Object thisArg = null;
    String[] emptyArray = new String[0];
    Object[] otherArgs = { emptyArray };

    RVMMethod mainMethod = getMethodFromClass(REFLECTION_TEST_CLASS, MAIN, String[].class);
    CompiledMethod cm = getCompiledMethod(mainMethod);

    Object result = Reflection.invokeCompiledMethodUnchecked(cm, thisArg, otherArgs);
    assertNull(result);

    String[] someStrings = { "a", "b", "cake", "pie" };
    Object[] nonEmptyArgs = { someStrings };
    Reflection.invokeCompiledMethodUnchecked(cm, thisArg, nonEmptyArgs);

    String resultString = getOutputForMain(thisArg, nonEmptyArgs);
    assertThatResultStringMatches(someStrings, resultString);
  }

  protected CompiledMethod getCompiledMethod(RVMMethod mainMethod) {
    CompiledMethod cm = mainMethod.getCurrentCompiledMethod();
    while (cm == null) {
      mainMethod.compile();
      cm = mainMethod.getCurrentCompiledMethod();
    }
    return cm;
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningVoid() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsVoid", thisArg, otherArgs);
    assertNull(result);

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningBoolean() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsBoolean", thisArg, otherArgs);
    assertThat((Boolean) result, is(ClassWithInstanceMethods.BOOLEAN_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningByte() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsByte", thisArg, otherArgs);
    assertThat((Byte) result, is(ClassWithInstanceMethods.BYTE_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningChar() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsChar", thisArg, otherArgs);
    assertThat((Character) result, is(ClassWithInstanceMethods.CHAR_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningDouble() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsDouble", thisArg, otherArgs);
    assertThat((Double) result, is(ClassWithInstanceMethods.DOUBLE_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningFloat() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsFloat", thisArg, otherArgs);
    assertThat((Float) result, is(ClassWithInstanceMethods.FLOAT_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningInt() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsInt", thisArg, otherArgs);
    assertThat((Integer) result, is(ClassWithInstanceMethods.INT_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningLong() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsLong", thisArg, otherArgs);
    assertThat((Long) result, is(ClassWithInstanceMethods.LONG_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningShort() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsShort", thisArg, otherArgs);
    assertThat((Short) result, is(ClassWithInstanceMethods.SHORT_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }

  @Test
  public void testDirectInvocationOfCompiledInstanceMethodReturningString() throws Exception {
    assumeStandardConfig();

    Object thisArg = new ClassWithInstanceMethods();
    Object[] otherArgs = getStandardArgsForMethodWithManyParameters();
    String expected = buildExpectedStringForMethodWithManyParameters(otherArgs);

    Object result = invokeMethodWithManyParameters("manyParametersReturnsString", thisArg, otherArgs);
    assertThat((String) result, is(ClassWithInstanceMethods.STRING_RETURN_VALUE));

    String resultString = getActualOutput(thisArg);
    assertThat(resultString, is(expected));
  }


  private void assumeStandardConfig() {
    assumeStandardReflectionSettings();
    assumeVMBuildForGnuClasspath();
  }

  private String getActualOutput(Object thisArg) throws ClassNotFoundException, NoSuchMethodException {
    RVMMethod getOutputForInstanceMethod = getMethodFromClass(CLASS_WITH_INSTANCE_METHODS, GET_OUTPUT_FOR_INSTANCE_METHOD);
    ReflectionBase invokerForGetOutput = getOutputForInstanceMethod.getInvoker();
    Object[] emptyArgs = {};
    String resultString = (String) Reflection.invoke(getOutputForInstanceMethod, invokerForGetOutput, thisArg, emptyArgs,
        false);
    return resultString;
  }

  private Object invokeMethodWithManyParameters(String methodName, Object thisArg, Object[] otherArgs) throws ClassNotFoundException,
      NoSuchMethodException {
    RVMMethod instanceMethod = getMethodWithManyParameters(methodName);
    CompiledMethod cm = getCompiledMethod(instanceMethod);
    Object result = Reflection.invokeCompiledMethodUnchecked(cm, thisArg, otherArgs);
    return result;
  }

  private String buildExpectedStringForMethodWithManyParameters(Object[] otherArgs) {
    StringBuilder sb = new StringBuilder();
    for (Object o : otherArgs) {
      sb.append(o.getClass().getSimpleName());
      sb.append(" was ");
      sb.append(o);
    }
    return sb.toString();
  }

  private Object[] getStandardArgsForMethodWithManyParameters() {
    Boolean b = Boolean.TRUE;
    Long l = Long.valueOf(123456789101112L);
    Double d =  Double.valueOf(123456E-10);
    Integer i = Integer.valueOf(-3450);
    Character c = Character.valueOf('€');
    Float f = Float.valueOf(-1234.56f);
    Object o = new Object();
    Short s = Short.valueOf((short) (Short.MIN_VALUE + (short) 200));
    Byte bt = Byte.valueOf((byte) -127);
    Object[] otherArgs = { b, l, d, i, c, f, o, s, bt};
    return otherArgs;
  }

  private RVMMethod getMethodWithManyParameters(String methodName) throws ClassNotFoundException, NoSuchMethodException {
    RVMMethod instanceMethod = getMethodFromClass(CLASS_WITH_INSTANCE_METHODS,
        methodName, boolean.class, long.class, double.class, int.class,
        char.class, float.class, Object.class, short.class, byte.class);
    return instanceMethod;
  }


  protected String getOutputForMain(Object invoker, Object[] nonEmptyArgs) throws ClassNotFoundException, NoSuchMethodException {
    RVMMethod getOutputForMain = getMethodFromClass(REFLECTION_TEST_CLASS, GET_OUTPUT_FOR_MAIN);
    String resultString = (String) Reflection.invoke(getOutputForMain, null, invoker, nonEmptyArgs, true);
    return resultString;
  }

  protected void resetReflectionTestClass() throws ClassNotFoundException, NoSuchMethodException {
    RVMMethod getOutputForMain = getMethodFromClass(REFLECTION_TEST_CLASS, "reset");
    Object[] args =  {};
    Reflection.invoke(getOutputForMain, null, null, args, true);
  }

}

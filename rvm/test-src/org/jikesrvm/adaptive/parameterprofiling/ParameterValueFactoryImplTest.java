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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterValueFactory;
import org.jikesrvm.classloader.ApplicationClassLoader;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.junit.runners.RequiresJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(VMRequirements.class)
public class ParameterValueFactoryImplTest {

  private ParameterValueFactory parameterValueFactory;

  @Before
  public void setUp() {
    parameterValueFactory = new ParameterValueFactoryImpl();
  }

  @Test
  public void creationOfBooleanValues() throws Exception {
    boolean b = true;
    BooleanParameterValue booleanParameter = parameterValueFactory.createBooleanParameter(b);
    assertThat(booleanParameter.getBooleanValue(), is(b));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void booleanValuesAreCanonicalized() throws Exception {
    boolean booleanForTrue = true;
    AbstractParameterValue firstBooleanParameterForTrue = parameterValueFactory.createBooleanParameter(booleanForTrue);
    AbstractParameterValue secondBooleanParameterForTrue = parameterValueFactory.createBooleanParameter(booleanForTrue);
    assertSame(firstBooleanParameterForTrue, secondBooleanParameterForTrue);

    boolean booleanForFalse = false;
    AbstractParameterValue firstBooleanParameterForFalse = parameterValueFactory.createBooleanParameter(booleanForFalse);
    AbstractParameterValue secondBooleanParameterForFalse = parameterValueFactory.createBooleanParameter(booleanForFalse);
    assertSame(firstBooleanParameterForFalse, secondBooleanParameterForFalse);
  }

  @Test
  public void creationOfByteValues() throws Exception {
    byte byteValue = 123;
    ByteParameterValue byteParameter = parameterValueFactory.createByteParameter(byteValue);
    assertThat(byteParameter.getByteValue(), is(byteValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void byteValuesAreCanonicalized() throws Exception {
    byte byteValue = (byte) 222;
    ByteParameterValue firstByteParameter = parameterValueFactory.createByteParameter(byteValue);
    ByteParameterValue secondByteParameter = parameterValueFactory.createByteParameter(byteValue);
    assertSame(firstByteParameter, secondByteParameter);
  }

  @Test
  public void creationOfCharValues() throws Exception {
    char charValue = '?';
    CharParameterValue charParameter = parameterValueFactory.createCharParameter(charValue);
    assertThat(charParameter.getCharValue(), is(charValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void charValuesAreCanonicalized() throws Exception {
    char charValue = '*';
    CharParameterValue firstCharParameter = parameterValueFactory.createCharParameter(charValue);
    CharParameterValue secondCharParameter = parameterValueFactory.createCharParameter(charValue);
    assertSame(firstCharParameter, secondCharParameter);
  }

  @Test
  public void creationOfDoubleValues() {
    double doubleValue = 1000.1234d;
    DoubleParameterValue doubleParameter = parameterValueFactory.createDoubleParameter(doubleValue);
    assertThat(doubleParameter.getDoubleValue(), is(doubleValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void doubleValuesAreCanonicalized() throws Exception {
    double doubleValue = 0.1234E04d;
    DoubleParameterValue firstDoubleParameter = parameterValueFactory.createDoubleParameter(doubleValue);
    DoubleParameterValue secondDoubleParameter = parameterValueFactory.createDoubleParameter(doubleValue);
    assertSame(firstDoubleParameter, secondDoubleParameter);
  }

  @Test
  public void creationOfFloatValues() throws Exception {
    float floatValue = -1.023435367f;
    FloatParameterValue floatParameter = parameterValueFactory.createFloatParameter(floatValue);
    assertThat(floatParameter.getFloatValue(), is(floatValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void floatValuesAreCanonicalized() throws Exception {
    float floatValue = -1.023435367E-22f;
    FloatParameterValue firstFloatParameter = parameterValueFactory.createFloatParameter(floatValue);
    FloatParameterValue secondFloatParameter = parameterValueFactory.createFloatParameter(floatValue);
    assertSame(firstFloatParameter, secondFloatParameter);
  }

  @Test
  public void creationOfLongValues() throws Exception {
    long longValue = 124567891011121314L;
    LongParameterValue longParameter = parameterValueFactory.createLongParameter(longValue);
    assertThat(longParameter.getLongValue(), is(longValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void longValuesAreCanonicalized() throws Exception {
    long longValue = 1011912303258903465L;
    LongParameterValue firstLongParameter = parameterValueFactory.createLongParameter(longValue);
    LongParameterValue secondLongParameter = parameterValueFactory.createLongParameter(longValue);
    assertSame(firstLongParameter, secondLongParameter);
  }

  @Test
  public void creationOfIntValues() {
    int intValue = 1000;
    IntParameterValue intParameter = parameterValueFactory.createIntParameter(intValue);
    assertThat(intParameter.getIntValue(), is(intValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void intValuesAreCanonicalized() throws Exception {
    int intValue = -1000;
    IntParameterValue firstIntParameter = parameterValueFactory.createIntParameter(intValue);
    IntParameterValue secondIntParameter = parameterValueFactory.createIntParameter(intValue);
    assertSame(firstIntParameter, secondIntParameter);
  }

  @Test
  public void creationOfShortValues() throws Exception {
    short shortValue = -20000;
    ShortParameterValue shortParameter = parameterValueFactory.createShortParameter(shortValue);
    assertThat(shortParameter.getShortValue(), is(shortValue));
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Test
  public void shortValuesAreCanonicalized() throws Exception {
    short shortValue = -30000;
    ShortParameterValue firstShortParameter = parameterValueFactory.createShortParameter(shortValue);
    ShortParameterValue secondShortParameter = parameterValueFactory.createShortParameter(shortValue);
    assertSame(firstShortParameter, secondShortParameter);
  }

  @Test
  public void creationOfNullValues() throws Exception {
    NullParameterValue nullValue = parameterValueFactory.createNullParameter();
    assertSame(nullValue, NullParameterValue.NULL);
  }

  @Category(RequiresJikesRVM.class)
  @Test
  public void creationOfObjectValues() throws Exception {
    String classThatIsGuaranteedToBeLoaded = this.getClass().getName();
    RVMType type = getRVMTypeForClassWithAppCL(classThatIsGuaranteedToBeLoaded);
    TypeValueForObjectParameter typeParameter = parameterValueFactory.createTypeValueForObjectParameter(type);
    assertThat(typeParameter.getObjectType(), is(type));
  }

  protected final RVMType getRVMTypeForClassWithAppCL(String className) {
    Atom descriptor = Atom.findOrCreateAsciiAtom(className.replace('.', '/')).descriptorFromClassName();
    TypeReference tRef = TypeReference.findOrCreate(ApplicationClassLoader.getSystemClassLoader(), descriptor);
    return tRef.peekType();
  }

  @Ignore("currently disabled as this functionality has been removed because of bugs")
  @Category(RequiresJikesRVM.class)
  @Test
  public void creationOfObjectValuesIsCanonicalized() throws Exception {
    String classThatIsGuaranteedToBeLoaded = this.getClass().getName();
    RVMType type = getRVMTypeForClassWithAppCL(classThatIsGuaranteedToBeLoaded);
    TypeValueForObjectParameter firstTypeParameter = parameterValueFactory.createTypeValueForObjectParameter(type);
    TypeValueForObjectParameter secondTypeParameter = parameterValueFactory.createTypeValueForObjectParameter(type);
    assertSame(firstTypeParameter, secondTypeParameter);
  }

}

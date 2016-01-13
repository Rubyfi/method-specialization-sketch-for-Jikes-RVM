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

import static org.junit.Assert.assertFalse;

import org.jikesrvm.junit.runners.RequiresJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.vmmagic.pragma.NoOptCompile;

@RunWith(VMRequirements.class)
@Category(RequiresJikesRVM.class)
public class MagicTest {

  //TODO Magic is currently tested only for baseline compiler.
  // It would be nice to test it for opt but I can't think of
  // a good way right now.

  @Test
  public void testParameterIsConstant() {
    verifyParameterIsConstantWorksInBaseline();
  }

  private void verifyParameterIsConstantWorksInBaseline() {
    assertFalse(doSomethingBase(1));
  }

  @NoOptCompile
  private boolean doSomethingBase(int i) {
    if (i == 1) {
      System.out.println(i);
    }
    return Magic.isConstantParameter(1);
  }

  @Test
  public void testIsSpecializedTypeParameter() throws ClassNotFoundException, ExceptionInInitializerError, SecurityException, IllegalAccessException, InstantiationException {
    verifyIsSpecializedTypeParameterWorksInBaseline();
  }

  private void verifyIsSpecializedTypeParameterWorksInBaseline() throws ClassNotFoundException, ExceptionInInitializerError, SecurityException, IllegalAccessException, InstantiationException {
    Class<?> klazz = Class.forName("B");
    assertFalse(doSomethingElseBase(klazz.newInstance()));
  }

  @NoOptCompile
  private boolean doSomethingElseBase(Object obj) {
    return Magic.isSpecializedTypeParameter(1);
  }

  @Test
  public void testisConstantLocal() {
    verifyIsConstantLocalWorksInBaseline();
  }

  private void verifyIsConstantLocalWorksInBaseline() {
    assertFalse(doSomethingWithLocals(1));
  }

  @NoOptCompile
  private boolean doSomethingWithLocals(int i) {
    if (i == 1) {
      System.out.println(i);
    }
    return Magic.isConstantLocal(1);
  }

  @Test
  public void testIsLocalWithSpecializedType() throws ExceptionInInitializerError, SecurityException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    verifyIsLocalWithSpecializedType();
  }

  private void verifyIsLocalWithSpecializedType() throws ClassNotFoundException, ExceptionInInitializerError, SecurityException, IllegalAccessException, InstantiationException {
    Class<?> klazz = Class.forName("B");
    assertFalse(doSomethingWithLocalsAndType(klazz.newInstance()));
  }

  @NoOptCompile
  private boolean doSomethingWithLocalsAndType(Object obj) {
    return Magic.isLocalWithSpecializedType(1);
  }

}

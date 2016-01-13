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

import org.jikesrvm.compilers.opt.ir.operand.ConstantOperand;
import org.jikesrvm.compilers.opt.ir.operand.IntConstantOperand;

public final class BooleanParameterValue extends AbstractParameterValue {

  private final boolean booleanValue;

  BooleanParameterValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public boolean getBooleanValue() {
    return booleanValue;
  }

  @Override
  public ConstantOperand buildOperand() {
    int value = booleanValue ? 1 : 0;
    return new IntConstantOperand(value);
  }

  @Override
  public String toString() {
    return Boolean.toString(booleanValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (booleanValue ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BooleanParameterValue other = (BooleanParameterValue) obj;
    if (booleanValue != other.booleanValue)
      return false;
    return true;
  }

}

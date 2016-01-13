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


public final class IntParameterValue extends AbstractParameterValue {

  private final int intValue;

  IntParameterValue(int i) {
    this.intValue = i;
  }

  public int getIntValue() {
    return intValue;
  }

  /**
   * @return a new {@link IntConstantOperand} representing this object
   */
  @Override
  public ConstantOperand buildOperand() {
    return new IntConstantOperand(intValue);
  }

  @Override
  public String toString() {
    return Integer.toString(intValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + intValue;
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
    IntParameterValue other = (IntParameterValue) obj;
    if (intValue != other.intValue)
      return false;
    return true;
  }

}

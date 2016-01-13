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
import org.jikesrvm.compilers.opt.ir.operand.FloatConstantOperand;


public final class FloatParameterValue extends AbstractParameterValue {

  private final float floatValue;

  FloatParameterValue(float floatValue) {
    this.floatValue = floatValue;
  }

  public float getFloatValue() {
    return floatValue;
  }

  /**
   * @return a new {@link FloatConstantOperand} representing this object
   */
  @Override
  public ConstantOperand buildOperand() {
    return new FloatConstantOperand(floatValue);
  }

  @Override
  public String toString() {
    return Float.toString(floatValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(floatValue);
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
    FloatParameterValue other = (FloatParameterValue) obj;
    if (Float.floatToIntBits(floatValue) != Float.floatToIntBits(other.floatValue))
      return false;
    return true;
  }

}

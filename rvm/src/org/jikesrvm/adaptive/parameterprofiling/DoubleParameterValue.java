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
import org.jikesrvm.compilers.opt.ir.operand.DoubleConstantOperand;


public final class DoubleParameterValue extends AbstractParameterValue {

  private final double doubleValue;

  DoubleParameterValue(double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public double getDoubleValue() {
    return doubleValue;
  }

  /**
   * @return a new {@link DoubleConstantOperand} representing this object
   */
  @Override
  public ConstantOperand buildOperand() {
    return new DoubleConstantOperand(doubleValue);
  }

  @Override
  public String toString() {
    return Double.toString(doubleValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(doubleValue);
    result = prime * result + (int) (temp ^ (temp >>> 32));
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
    DoubleParameterValue other = (DoubleParameterValue) obj;
    if (Double.doubleToLongBits(doubleValue) != Double.doubleToLongBits(other.doubleValue))
      return false;
    return true;
  }

}

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
import org.jikesrvm.compilers.opt.ir.operand.LongConstantOperand;


public final class LongParameterValue extends AbstractParameterValue {

  private final long longValue;

  LongParameterValue(long longValue) {
    this.longValue = longValue;
  }

  public long getLongValue() {
    return longValue;
  }

  /**
   * @return a new {@link LongConstantOperand} representing this object
   */
  @Override
  public ConstantOperand buildOperand() {
    return new LongConstantOperand(longValue);
  }

  @Override
  public String toString() {
    return Long.toString(longValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (longValue ^ (longValue >>> 32));
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
    LongParameterValue other = (LongParameterValue) obj;
    if (longValue != other.longValue)
      return false;
    return true;
  }

}

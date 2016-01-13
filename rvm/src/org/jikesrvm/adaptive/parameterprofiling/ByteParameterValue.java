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


public final class ByteParameterValue extends AbstractParameterValue {

  private final byte byteValue;

  ByteParameterValue(byte byteValue) {
    this.byteValue = byteValue;
  }

  public byte getByteValue() {
    return byteValue;
  }

  @Override
  public ConstantOperand buildOperand() {
    return new IntConstantOperand(byteValue);
  }

  @Override
  public String toString() {
    return Byte.toString(byteValue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + byteValue;
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
    ByteParameterValue other = (ByteParameterValue) obj;
    if (byteValue != other.byteValue)
      return false;
    return true;
  }

}

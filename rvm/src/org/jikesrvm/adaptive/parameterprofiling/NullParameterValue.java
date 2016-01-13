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
import org.jikesrvm.compilers.opt.ir.operand.NullConstantOperand;


public final class NullParameterValue extends AbstractParameterValue {

  public static final NullParameterValue NULL = new NullParameterValue();

  private NullParameterValue() {
    // only one instance needed
  }

  /**
   * @return a new {@link NullConstantOperand}
   */
  @Override
  public ConstantOperand buildOperand() {
    return new NullConstantOperand();
  }

  @Override
  public String toString() {
    return "NULL";
  }

}

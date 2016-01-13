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

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.compilers.opt.ir.operand.ConstantOperand;

public abstract class AbstractParameterValue extends AbstractParameterInfo {

  public AbstractParameterValue() {
    super();
  }

  /**
   * Builds an operand.
   *
   * @return an operand that represents the information of this object. The
   *         operand is not necessarily newly created.
   */
  public abstract ConstantOperand buildOperand();

  /**
   * {@inheritDoc}
   *
   * @return <code>false</code> because this class and its subclasses represent
   *  values of parameters (and not types)
   */
  @Override
  public boolean hasTypeInformation() {
    return false;
  }

}

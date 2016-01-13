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
package org.jikesrvm.adaptive.measurements.listeners.parameterprofiling;

public abstract class AbstractParameterInfo {

  /**
   * Does this instance have information about the parameter's type?
   *
   * @return <code>true</code> if and only if the class is a
   *    {@link TypeValueForObjectParameter}
   */
  public abstract boolean hasTypeInformation();

}

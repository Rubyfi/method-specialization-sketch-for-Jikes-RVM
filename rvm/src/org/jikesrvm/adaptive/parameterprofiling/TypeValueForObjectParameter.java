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
import org.jikesrvm.classloader.RVMType;

public final class TypeValueForObjectParameter extends AbstractParameterInfo {

  private final RVMType objectType;

  TypeValueForObjectParameter(RVMType objectType) {
    this.objectType = objectType;
  }

  public RVMType getObjectType() {
    return objectType;
  }

  @Override
  public boolean hasTypeInformation() {
    return true;
  }

  @Override
  public String toString() {
    return objectType.getDescriptor().toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
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
    TypeValueForObjectParameter other = (TypeValueForObjectParameter) obj;
    if (objectType == null) {
      if (other.objectType != null)
        return false;
    } else if (!objectType.equals(other.objectType))
      return false;
    return true;
  }

}

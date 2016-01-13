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

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.MethodDataProvider;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.TypeReference;

public class MethodDataProviderImpl implements MethodDataProvider {

  private RVMMethod method;

  public MethodDataProviderImpl(RVMMethod method) {
    this.method = method;
  }

  @Override
  public TypeReference[] getParameterTypes() {
    return method.getParameterTypes();
  }

  @Override
  public String getDeclaringClassDescriptor() {
    return method.getDeclaringClass().getDescriptor().toString();
  }

  @Override
  public String getMethodName() {
    return method.getName().toString();
  }

  @Override
  public String getMethodDescriptor() {
    return method.getDescriptor().toString();
  }

  @Override
  public boolean isInstanceMethod() {
    return !method.isStatic();
  }

  @Override
  public int getParameterCountIncludingThis() {
    int paramCount = method.getParameterTypes().length;
    if (isInstanceMethod()) {
      paramCount++;
    }
    return paramCount;
  }

}

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

import org.jikesrvm.classloader.RVMType;

public interface ParameterProfileInformation {

  void addNewIntValue(int intValue);

  void addNewByteValue(byte byteValue);

  void addNewCharValue(char charValue);

  void addNewShortValue(short shortValue);

  void addNewLongValue(long longValue);

  void addNewBooleanValue(boolean booleanValue);

  void addNewDoubleValue(double doubleValue);

  void addNewFloatValue(float floatValue);

  void addNewType(RVMType objectType);

}

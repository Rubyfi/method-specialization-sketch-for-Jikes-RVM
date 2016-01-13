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

import org.jikesrvm.classloader.RVMType;

public interface ParameterDecoder extends ParameterStorage {

  byte decodeByte(int startIndex);

  char decodeChar(int startIndex);

  boolean decodeBoolean(int startIndex);

  short decodeShort(int startIndex);

  float decodeFloat(int startIndex);

  int decodeInt(int startIndex);

  long decodeLong(int startIndex);

  double decodeDouble(int startIndex);

  RVMType decodeType(int index);

  void switchToDecodeMode();

}

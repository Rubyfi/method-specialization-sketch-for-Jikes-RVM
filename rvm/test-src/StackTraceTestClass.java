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
/**
 * This class is used for testing of stacktraces and therefore fragile.
 *
 * Note: If you do anything that changes the ine numbers in this class, you
 * must change the linenumbers in the respective test cases.
 */
public class StackTraceTestClass {

  public static void throwException(int type) {
    if (type == 0) {
      throw new UnsupportedOperationException();
    } else if (type == 1) {
      throw new IllegalArgumentException();
    } else {
      throw new RuntimeException();
    }
  }

}

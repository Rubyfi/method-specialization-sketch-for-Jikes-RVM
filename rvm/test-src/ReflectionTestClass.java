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
public class ReflectionTestClass {

  private static StringBuilder outputForMain = new StringBuilder();

  public static void main(String[] args) {
    for (String s : args) {
      outputForMain.append(s);
    }
  }

  public static String getOutputForMain() {
    return outputForMain.toString();
  }

  public static void reset() {
    outputForMain = new StringBuilder();
  }

}

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

import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_ADDRESS;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_EXTENT;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_OFFSET;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Word;

@Uninterruptible
public final class ParameterUtilities {

  private ParameterUtilities() {
    // prevent instantiation of this utility class
  }

  /**
   * Logs debugging information about a method.
   *
   * @param compiledMethod
   *          the compiled method
   */
  public static void logInformationAboutMethod(CompiledMethod compiledMethod) {
    RVMMethod method = compiledMethod.getMethod();

    int parameterWords = ParameterListener.getTotalNumberOfParametersWords(method);
    int numberOfExplicitParams = method.getParameterTypes().length;

    if (method.isStatic()) {
      ParameterUtilities.writeForDebugging("Static ");
    } else {
      ParameterUtilities.writeForDebugging("Instance ");
    }

    ParameterUtilities.writeForDebugging("method ");
    ParameterUtilities.writeForDebugging(" with name ");
    ParameterUtilities.writeForDebugging(method.getName());
    ParameterUtilities.writeForDebugging(" and descriptor ");
    ParameterUtilities.writeForDebugging(method.getDescriptor());
    ParameterUtilities.writeForDebugging(" declared by ");
    ParameterUtilities.writeForDebugging(method.getDeclaringClass().getDescriptor());
    ParameterUtilities.writeForDebugging(" has ");
    ParameterUtilities.writeForDebugging(numberOfExplicitParams);
    ParameterUtilities.writeForDebugging(" parameters (excluding implicit this parameter, if any) which occupy ");
    ParameterUtilities.writeForDebugging(parameterWords);
    ParameterUtilities.writeForDebugging(" words of space");
    ParameterUtilities.writeForDebuggingLn();
  }

  /**
   * Log a word for debugging.
   *
   * @param w
   *          the word
   */
  protected static void writeForDebugging(Word w) {
    VM.sysWrite(w);
    // Log.write(w);
  }

  /**
   * Log a number for debugging.
   *
   * @param numberOfParams
   *          the number to log
   */
  protected static void writeForDebugging(int numberOfParams) {
    VM.sysWrite(numberOfParams);
    // Log.write(numberOfParams);
  }

  /**
   * Log a char for debugging.
   *
   * @param c
   *          the character to log
   */
  protected static void writeForDebugging(char c) {
    VM.sysWrite(c);
    // Log.write(c);
  }

  /**
   * Log a double for debugging.
   *
   * @param d
   *          the double to log
   */
  protected static void writeForDebugging(double d) {
    VM.sysWrite(d);
    // Log.write(d);
  }

  /**
   * Log a float for debugging.
   *
   * @param d
   *          the float to log
   */
  protected static void writeForDebugging(float f) {
    VM.sysWrite(f);
    // Log.write(f);
  }

  /**
   * Logs a boolean for debugging.
   *
   * @param b
   *          the boolean
   */
  protected static void writeForDebugging(boolean b) {
    VM.sysWrite(b);
    // Log.write(b);
  }

  /**
   * Log a long for debugging.
   *
   * @param d
   *          the long to log
   */
  protected static void writeForDebugging(long l) {
    VM.sysWrite(l);
    // Log.write(l);
  }

  /**
   * Log a short for debugging.
   *
   * @param d
   *          the double to log
   */
  protected static void writeForDebugging(short s) {
    VM.sysWrite(s);
    // Log.write(s);
  }

  /**
   * Log a byte for debugging.
   *
   * @param b
   *          the byte to log
   */
  protected static void writeForDebugging(byte b) {
    VM.sysWrite(b);
    // Log.write(b);
  }

  /**
   * Log a string for debugging.
   *
   * @param s
   *          the string to log
   */
  protected static void writeForDebugging(String s) {
    VM.sysWrite(s);
    // Log.write(s);
  }

  /**
   * Log an atom for debugging.
   *
   * @param a
   *          the Atom to log
   */
  protected static void writeForDebugging(Atom a) {
    VM.sysWrite(a);
    // Log.write(a.toByteArray());
  }

  /**
   * Log an atom for debugging.
   *
   * @param a
   *          the Atom to log
   */
  @Inline
  protected static void writeForDebuggingLn(String s) {
    VM.sysWriteln(s);
    // Log.writeln(s);
  }

  /**
   * Log an atom for debugging.
   *
   * @param a
   *          the Atom to log
   */
  protected static void writeForDebuggingLn() {
    VM.sysWriteln();
    // Log.writeln();
  }

  /**
   * Logs an Address for Debugging.
   *
   * @param addr
   *          the address
   */
  protected static void writeForDebugging(Address addr) {
    // Log.write(objPointer);
    VM.sysWrite(addr);
  }

  /**
   * Logs an Address for Debugging, writes a newline and flushes the buffer.
   *
   * @param addr
   *          the address
   */
  protected static void writeForDebuggingLn(Address addr) {
    writeForDebugging(addr);
    writeForDebuggingLn();
  }

  /**
   * Dumps all addresses from -4 Words to +4 Words relative to the given
   * address.
   *
   * @param addr
   *          start address
   */
  protected static void dumpAddresses(Address addr) {
    Address addrMinus4 = addr.minus(4 * BYTES_IN_WORD);
    Address addrMinus3 = addr.minus(3 * BYTES_IN_WORD);
    Address addrMinus2 = addr.minus(2 * BYTES_IN_WORD);
    Address addrMinus1 = addr.minus(1 * BYTES_IN_WORD);
    Address addrPlus1 = addr.plus(1 * BYTES_IN_WORD);
    Address addrPlus2 = addr.plus(2 * BYTES_IN_WORD);
    Address addrPlus3 = addr.plus(3 * BYTES_IN_WORD);
    Address addrPlus4 = addr.plus(4 * BYTES_IN_WORD);
    writeForDebugging("Addresses of addrPointer -4 to +4 ");
    writeForDebugging(addrMinus4);
    writeForDebugging(" ");
    writeForDebugging(addrMinus3);
    writeForDebugging(" ");
    writeForDebugging(addrMinus2);
    writeForDebugging(" ");
    writeForDebugging(addrMinus1);
    writeForDebugging(" [");
    writeForDebugging(addr);
    writeForDebugging("] ");
    writeForDebugging(addrPlus1);
    writeForDebugging(" ");
    writeForDebugging(addrPlus2);
    writeForDebugging(" ");
    writeForDebugging(addrPlus3);
    writeForDebugging(" ");
    writeForDebugging(addrPlus4);
    writeForDebuggingLn();
  }

  /**
   * Checks two addresses and exits the VM if they are not equal.
   *
   * @param firstAddress
   *          first Address
   * @param secondAddress
   *          second Address
   */
  protected static void failIfAddressesNotEqual(Address firstAddress, Address secondAddress) {
    if (firstAddress.NE(secondAddress)) {
      writeForDebuggingLn(firstAddress);
      writeForDebuggingLn(secondAddress);
      VM.sysFail("Pointers are not equal, this should not happen.");
    }
  }

  /**
   * Check that all word-like types have the same size.
   */
  // Suppress warnings concerning "comparing identical expressions" because the
  // whole point
  // of this method is to check if the expression are not identical for some strange reason
  @SuppressWarnings("all")
  protected static void assertWordLikeTypesAreConsistent() {
    boolean typesConsistent = (BYTES_IN_WORD == BYTES_IN_EXTENT);
    typesConsistent = (BYTES_IN_OFFSET == BYTES_IN_ADDRESS) && (BYTES_IN_ADDRESS == BYTES_IN_WORD) && typesConsistent;
    if (!typesConsistent) {
      if (VM.VerifyAssertions) {
        VM._assert(VM.NOT_REACHED);
      } else {
        VM.sysFail("Word-like types not consistent");
      }
    }


  }

}

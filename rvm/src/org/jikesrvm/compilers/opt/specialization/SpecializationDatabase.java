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
package org.jikesrvm.compilers.opt.specialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.util.HashMapRVM;
import org.jikesrvm.util.HashSetRVM;
import org.jikesrvm.util.ImmutableEntryHashMapRVM;
import org.vmmagic.pragma.Uninterruptible;

/**
 * Database to store multiple specialized versions for a given method.
 *
 * <p> The overall design is very similar to that of the
 * InvalidationDatabase (see InvalidationDatabase.java)
 * In this database, the key is the RVMMethod object of the source method
 * and the value is a method set. The method set is a list of
 * specialized versions of the method pointed by the key. Specialized
 * versions are represented by using the SpecializedMethod class.
 * There is no provision for removing/deleting method versions as classes
 * are never unloaded and the ClassLoader.compiledMethods[] is never cleaned.
 */
public final class SpecializationDatabase {

  private static boolean DEBUG = false;

  private static int[] smidsForParameterSpecializedMethods;

  private static int currentIndexForSavedSmids;

  private static SpecializationDatabase instance = new SpecializationDatabase();

  public SpecializationDatabase() {
    smidsForParameterSpecializedMethods = new int[16];
    currentIndexForSavedSmids = 0;
  }

  private static boolean specializationInProgress;

  private HashSetRVM<SpecializedMethod> deferredMethods =
    new HashSetRVM<SpecializedMethod>();

  private ImmutableEntryHashMapRVM<RVMMethod, MethodSet<RVMMethod>> specialVersionsHash =
      new ImmutableEntryHashMapRVM<RVMMethod, MethodSet<RVMMethod>>();

  private HashMapRVM<RVMMethod, HashSetRVM<SpecializationContext>> methodsWithSpecializedParameters =
      new HashMapRVM<RVMMethod, HashSetRVM<SpecializationContext>>();

  private static synchronized SpecializationDatabase getInstance() {
    return instance;
  }

  /**
   * Resets the singleton instance.<p>
   *
   * NB: for testcases only.
   */
  public static synchronized void resetInstance() {
    instance = new SpecializationDatabase();
  }

  /**
   * Drain the queue of methods waiting for specialized code
   * generation.
   */
  public static synchronized void doDeferredSpecializations() {
    // prevent recursive entry to this method
    if (specializationInProgress) {
      return;
    }
    specializationInProgress = true;
    Iterator<SpecializedMethod> methods = getInstance().deferredMethods.iterator();
    while (methods.hasNext()) {
      SpecializedMethod m = methods.next();
      if (m.getCompiledMethod() == null) {

        if (DEBUG) {
          VM.sysWriteln("Starting opt-compilation of specialized method " + m);
        }

        m.compile();

        if (DEBUG) {
          VM.sysWriteln("Finished opt-compilation of specialized method " + m + ". Will register method now!");
        }

        registerCompiledMethod(m);

        if (DEBUG) {
          VM.sysWriteln("Successfully registered specialized method " + m);
        }

      }
      getInstance().deferredMethods.remove(m);
      // since we modified the set, reset the iterator.
      // TODO: use a better abstraction
      // (ModifiableSetIterator of some kind?)
      methods = getInstance().deferredMethods.iterator();
    }
    specializationInProgress = false;
  }

  // write the new compiled method in the specialized method pool
  private static void registerCompiledMethod(SpecializedMethod m) {
    SpecializedMethodPool.registerCompiledMethod(m);
  }

  /**
   * @param m the method whose specialized methods are queried
   * @return an iteration of specialized compiled versions, {@code null}
   *  if no specialized versions
   */
  static synchronized Iterator<SpecializedMethod> getSpecialVersions(RVMMethod m) {
    MethodSet<RVMMethod> s = getInstance().specialVersionsHash.get(m);
    if (s == null) {
      return null;
    } else {
      return s.iterator();
    }
  }

  /**
   * Returns those specialized methods of a method that need to be invoked via the
   * general method version.
   * @param method a given method
   * @return a (possibly empty) list of the specialized methods that need to be invoked via the general
   *  method version.
   */
  public static synchronized List<SpecializedMethod> getSpecialVersionsThatNeedToBeCalledFromGeneralMethod(NormalMethod method) {
    List<SpecializedMethod> specializedMethods = new ArrayList<SpecializedMethod>();

    Iterator<SpecializedMethod> methodsIter = getSpecialVersions(method);
    if (methodsIter == null) {
      return Collections.emptyList();
    }

    while (methodsIter.hasNext()) {
      SpecializedMethod spMethod = methodsIter.next();
      SpecializationContext context = spMethod.getSpecializationContext();
      NormalMethod normalMethod = spMethod.getMethod();
      HashSetRVM<SpecializationContext> contexts = getInstance().methodsWithSpecializedParameters.get(normalMethod);
      if (contexts.contains(context)) {
        specializedMethods.add(spMethod);
      }
    }

    return specializedMethods;
  }

  static int getSpecialVersionCount(RVMMethod m) {
    Iterator<SpecializedMethod> versions = getSpecialVersions(m);
    int count = 0;
    if (versions != null) {
      while (versions.hasNext() && (versions.next() != null)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Records a new specialized method in this database.
   * Also remember that this method will need to be compiled later,
   * at the next call to <code> doDeferredSpecializations() </code>
   *
   * @param spMethod the method to register
   */
  static synchronized void registerSpecialVersion(SpecializedMethod spMethod) {
    RVMMethod source = spMethod.getMethod();
    MethodSet<RVMMethod> s = findOrCreateMethodSet(getInstance().specialVersionsHash, source);
    s.add(spMethod);
    getInstance().deferredMethods.add(spMethod);
  }

  /**
   * Register a specialized version that is eglible to be called by the general
   * version of a method.<p>
   *
   * This currently applies only to methods with specialized parameters.
   *
   * @param context the specialized method's specialization context
   * @param spMethod the specialized method
   */
  static synchronized void registerContextWithSpecializedParameters(SpecializationContext context, SpecializedMethod spMethod) {
    RVMMethod method = spMethod.getMethod();
    HashSetRVM<SpecializationContext> specializationContexts = getInstance().methodsWithSpecializedParameters.get(method);
    if (specializationContexts == null) {
      specializationContexts = new HashSetRVM<SpecializationContext>();
      getInstance().methodsWithSpecializedParameters.put(method, specializationContexts);
    }
    specializationContexts.add(context);

    growSMIDArrayIfNecessary();

    smidsForParameterSpecializedMethods[currentIndexForSavedSmids] = spMethod.getSpecializedMethodIndex();
    currentIndexForSavedSmids++;
  }

  private static void growSMIDArrayIfNecessary() {
    int currentLength = smidsForParameterSpecializedMethods.length;
    if (currentIndexForSavedSmids >= currentLength) {
      int newLength = 2 * currentLength;
      int[] newSmids = new int[newLength];
      System.arraycopy(smidsForParameterSpecializedMethods, 0, newSmids, 0, currentLength);
      smidsForParameterSpecializedMethods = newSmids;
    }
  }

  /**
   * Does the given method have special versions that should be called directly by the
   * general version of the method? This is the case for methods with specialized parameters
   * that are created by the opt compiler (i.e. not for scanning in MMTk).<p>
   *
   * Such versions need to be registered by {@link SpecializationContext}s via
   * {@link #registerContextWithSpecializedParameters(SpecializationContext, SpecializedMethod)}.
   *
   * @param method a method
   * @return <code>true</code> if this method has specialized versions that should
   *  to be called in the general method, if parameters match
   */
  public static synchronized boolean shouldCallSpecialVersions(RVMMethod method) {
    HashSetRVM<SpecializationContext> contexts = getInstance().methodsWithSpecializedParameters.get(method);
    boolean hasSpecialVersions = contexts != null && contexts.size() > 0;
    return hasSpecialVersions;
  }

  @Uninterruptible
  public static int[] getSMIDsForParamSpecializedMethods() {
    return smidsForParameterSpecializedMethods;
  }

  /**
   * Looks up the MethodSet corresponding to a given key in the database.
   *
   * @param <T> type of the key in the database
   * @param hash the database
   * @param key the key
   * @return the method set for the given key
   */
  private static <T> MethodSet<T> findOrCreateMethodSet(ImmutableEntryHashMapRVM<T, MethodSet<T>> hash, T key) {
    MethodSet<T> result = hash.get(key);
    if (result == null) {
      result = new MethodSet<T>(key);
      hash.put(key, result);
    }
    return result;
  }

  /**
   * The following defines a set of methods that share a common "key"
   */
  static class MethodSet<T> {
    final T key;

    /**
     * a set of SpecializedMethod
     */
    final HashSetRVM<SpecializedMethod> methods = new HashSetRVM<SpecializedMethod>();

    MethodSet(T key) {
      this.key = key;
    }

    void add(SpecializedMethod spMethod) {
      methods.add(spMethod);
    }

    public Iterator<SpecializedMethod> iterator() {
      return methods.iterator();
    }
  }


}

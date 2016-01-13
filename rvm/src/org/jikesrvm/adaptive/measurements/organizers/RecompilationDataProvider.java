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
package org.jikesrvm.adaptive.measurements.organizers;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.measurements.Reportable;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.runtime.Callbacks.MethodCompileMonitor;

public class RecompilationDataProvider implements MethodCompileMonitor, Reportable {

  private ConcurrentHashMap<RVMMethod, Integer> initialOptLevel = new ConcurrentHashMap<RVMMethod, Integer>();
  private ConcurrentHashMap<RVMMethod, Deque<Integer>> allRecompilations = new ConcurrentHashMap<RVMMethod, Deque<Integer>>();
  private int totalOptCompilations = 0;

  //TODO introduce option for enabling recompilation monitor
  public static final boolean ENABLED = false;

  public RecompilationDataProvider() {
  }

  @Override
  public void notifyMethodCompile(RVMMethod method, int compiler) {
    // data ignored
  }

  @Override
  public void notifyMethodOptCompile(RVMMethod method, CompilationPlan plan) {
    totalOptCompilations++;

    int optLevel = plan.options.getOptLevel();
    if (!initialOptLevel.containsKey(method)) {
      initialOptLevel.put(method, optLevel);
      return;
    }

    Deque<Integer> deque = allRecompilations.get(method);
    if (deque == null) {
      deque = new LinkedList<Integer>();
      allRecompilations.put(method, deque);
    }
    deque.addLast(optLevel);
  }

  @Override
  public void report() {
    VM.sysWrite("TOTAL_OPT_COMPILATIONS");
    VM.sysWrite("\t");
    VM.sysWrite(totalOptCompilations);
    VM.sysWriteln();

    VM.sysWrite("TOTAL_OPT_METHOD_COUNT");
    VM.sysWrite("\t");
    VM.sysWrite(initialOptLevel.size());
    VM.sysWriteln();

    for (Entry<RVMMethod, Integer> initialOptCompileData : initialOptLevel.entrySet()) {
      VM.sysWrite("INITIAL_OPT_COMPILE");
      VM.sysWrite("\t");
      VM.sysWrite(initialOptCompileData.getKey().toString());
      VM.sysWrite("\t");
      VM.sysWrite(initialOptCompileData.getValue());
      VM.sysWriteln();
    }

    for (Entry<RVMMethod, Deque<Integer>> recompileOptData : allRecompilations.entrySet()) {
      VM.sysWrite("RECOMPILE_COUNT");
      VM.sysWrite("\t");
      VM.sysWrite(recompileOptData.getKey().toString());
      VM.sysWrite("\t");
      Deque<Integer> value = recompileOptData.getValue();
      VM.sysWrite(value.size());
      VM.sysWriteln();

      while (true) {
        Integer recompileOptLevel = value.removeFirst();
        VM.sysWrite("RECOMPILE");
        VM.sysWrite("\t");
        VM.sysWrite(recompileOptData.getKey().toString());
        VM.sysWrite("\t");
        VM.sysWrite(recompileOptLevel);
        VM.sysWriteln();

        if (value.isEmpty()) {
          break;
        }
      }
    }

    VM.sysWriteln();
  }

  @Override
  public void reset() {
    initialOptLevel = new ConcurrentHashMap<RVMMethod, Integer>();
    allRecompilations = new ConcurrentHashMap<RVMMethod, Deque<Integer>>();
    totalOptCompilations++;
  }

}

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

import java.util.List;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.controller.Controller;
import org.jikesrvm.adaptive.parameterprofiling.ParameterProfileInformation;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.compilers.opt.specialization.DebugSpecializationOracle;
import org.jikesrvm.compilers.opt.specialization.DebugSpecializeEagerlyOracle;
import org.jikesrvm.compilers.opt.specialization.DefaultSpecializationOracle;
import org.jikesrvm.compilers.opt.specialization.NeverSpecializeOracle;
import org.jikesrvm.compilers.opt.specialization.ParameterValueSpecializationContext;
import org.jikesrvm.compilers.opt.specialization.SpecializationDecision;
import org.jikesrvm.compilers.opt.specialization.SpecializationDecisionShutdownHook;
import org.jikesrvm.compilers.opt.specialization.SpecializationOracle;
import org.jikesrvm.runtime.Callbacks.MethodCompileMonitor;

public class SpecializedMethodCreater implements MethodCompileMonitor {

  private static final String NO_SPECIALIZATION_DURING_BOOTIMAGE_WRITING =
      "The whole specialization infrastructure only supports specializing " +
      "methods at runtime (i.e. not when writing the bootimage)!";

  private static final boolean DEBUG = false;

  private static final boolean DEBUG_USING_FIXED_SPECIALIZED_METHODS = DEBUG && false;

  private static final boolean DEBUG_BY_SPECIALIZING_EAGERLY = DEBUG && !DEBUG_USING_FIXED_SPECIALIZED_METHODS;

  private final ParameterProfileOrganizer organizer;

  private final SpecializationOracle oracle;

  private final SpecializationDecisionShutdownHook decisionHook;

  public SpecializedMethodCreater(ParameterProfileOrganizer ppo) {
    this.organizer = ppo;

    if (DEBUG_USING_FIXED_SPECIALIZED_METHODS) {
      oracle = new DebugSpecializationOracle();
    } else if (DEBUG_BY_SPECIALIZING_EAGERLY) {
      oracle = new DebugSpecializeEagerlyOracle();
    } else if (Controller.options.noSpecialization()) {
      oracle = new NeverSpecializeOracle();
    } else if (Controller.options.atMostOneVersionPerOptCompiledMethod()) {
      oracle = new DefaultSpecializationOracle();
    } else {
      if (VM.VerifyAssertions) {
        VM._assert(VM.NOT_REACHED, "Unknown specialization mode!");
      }
      VM.sysWriteln("Unknown specialization mode!");
      oracle = new NeverSpecializeOracle();
    }

    if (Controller.options.SPEC_DECISIONS_LOGGING) {
      String logFile = Controller.options.SPECIALIZATION_DECISIONS_LOGFILE;
      decisionHook = new SpecializationDecisionShutdownHook(logFile);
      Runtime.getRuntime().addShutdownHook(new Thread(decisionHook));
    } else {
      decisionHook = null;
    }
  }

  @Override
  public void notifyMethodCompile(RVMMethod method, int compiler) {
    // nothing to do, all work is done in notfiyMethodOptCompile
  }

  @Override
  public void notifyMethodOptCompile(RVMMethod method, CompilationPlan plan) {
    if (!VM.runningVM) {
      if (VM.VerifyAssertions) {
        VM._assert(VM.NOT_REACHED, NO_SPECIALIZATION_DURING_BOOTIMAGE_WRITING);
      }
      return;
    }

    List<ParameterProfileInformation> profiles = organizer.getProfiles(method);
    SpecializationDecision decision = oracle.shouldSpecialize(method, profiles, plan);

    if (Controller.options.SPEC_DECISIONS_LOGGING) {
      decisionHook.addDecision(decision);
    }

    boolean specialize = decision.isYES();
    if (specialize) {
      ParameterValueSpecializationContext context = decision.getContext();
      context.findOrCreateSpecializedVersion((NormalMethod) method);
      organizer.throwAwayDataForMethod(method);
    }
  }

}

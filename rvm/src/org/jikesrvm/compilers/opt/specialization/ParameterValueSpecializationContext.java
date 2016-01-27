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

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.controller.Controller;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.jikesrvm.compilers.opt.OptOptions;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.compilers.opt.driver.OptimizationPlanElement;
import org.jikesrvm.compilers.opt.driver.OptimizationPlanner;
import org.jikesrvm.compilers.opt.driver.OptimizingCompiler;

public class ParameterValueSpecializationContext extends SpecializationContext {

  private static final boolean DEBUG = false;

  /**
   * Standard options for specialization, created when the opt compiler is
   * initialized.
   */
  private static OptOptions options;
  private static OptimizationPlanElement[] optimizationPlan;

  private final AbstractParameterInfo[] parameterInformation;
  private final NormalMethod normalMethod;

  /**
   * This field provides a possibility to override the options from
   * {@link ParameterValueSpecializationContext#options}. If it is non-
   * <code>null</code>, these options will be used instead of the class-wide
   * ones.
   */
  private final OptOptions overridingOptions;

  /**
   * NB: Use only in testcases.
   * @param method the method to specialize
   */
  ParameterValueSpecializationContext(RVMMethod method) {
    parameterInformation = null;
    overridingOptions = options;
    normalMethod = null;
  }

  public ParameterValueSpecializationContext(NormalMethod nm, AbstractParameterInfo[] paramValues) {
    this(nm, null, paramValues);
  }

  public ParameterValueSpecializationContext(NormalMethod nm, OptOptions opts, AbstractParameterInfo[] paramValues) {
    this.normalMethod = nm;
    this.overridingOptions = opts;
    this.parameterInformation = paramValues;
    if (VM.VerifyAssertions) {
      boolean parameterCountMatches = nm.getParameterTypes().length == paramValues.length;
      if (!parameterCountMatches) {
        String parameterCountMismatchMsg = "Length of parameter types does not match length of value array" +
            " for specialized values. ParameterValueSpecializationContext" +
            "can only specialize on real parameters, not" +
            "the implicit this parameter";
            VM._assert(VM.NOT_REACHED, parameterCountMismatchMsg);
      }
    }
  }

  /**
   * Generates code to specialize a method in this context. Namely, builds a
   * {@link CompilationPlan} for the specialized method and invokes the opt
   * compiler with the built plan and the options.
   *
   * @param source the method to specialize
   */
  @Override
  CompiledMethod specialCompile(NormalMethod source) {
    OptOptions optsToUse = (overridingOptions == null) ? options : overridingOptions;

    CompilationPlan cp = new CompilationPlan(normalMethod, optimizationPlan, null, optsToUse, parameterInformation);
    return OptimizingCompiler.compile(cp);
  }

  /**
   * {@inheritDoc}<p>
   *
   * For specialization on parameters, tell the {@link SpecializationDatabase}
   * that there is no one more specialized version that needs special treatment
   * by the opt compiler when recompiling the general method.
   */
  @Override
  protected SpecializedMethod createSpecializedMethod(NormalMethod method) {
    SpecializedMethod sp = super.createSpecializedMethod(method);
    SpecializationDatabase.registerContextWithSpecializedParameters(this, sp);
    return sp;
  }

  /**
   * Initializes options to the standard options and builds the optimization
   * plan that will be used for all compiles.
   */
  public static void init() {
    options = new OptOptions();

    // To reduce the chance that a specialized method will be invalidated,
    // disable OSR for inlining and disable code-patch guards.
    options.OSR_GUARDED_INLINING = false;
    options.INLINE_GUARD_KIND = OptOptions.INLINE_GUARD_METHOD_TEST;

    if (DEBUG) {
      options.PRINT_ALL_IR = true;
    }

    // In order to reduce the potential number of invalidations, always
    // compile specialized versions at the maximum opt level.
    options.setOptLevel(Controller.options.MAX_OPT_LEVEL);

    optimizationPlan = OptimizationPlanner.createOptimizationPlan(options);
  }

  public AbstractParameterInfo[] getParameterInformation() {
    return parameterInformation;
  }

  @Override
  public String toString() {
    StringBuilder rep = new StringBuilder();
    rep.append(this.getClass().getName());
    rep.append(" ");

    if (parameterInformation == null) {
      rep.append("NO_PARAMETER_INFORMATION: THIS SHOULD ONLY HAPPEN IN TEST CODE!");
    } else {
      rep.append("Information about parameters: ");
      for (int param = 0; param < parameterInformation.length; param++) {
        rep.append(param);
        rep.append(": ");

        AbstractParameterInfo abstractParameterInfo = parameterInformation[param];
        if (abstractParameterInfo == null) {
          rep.append(" no info");
        } else {
          rep.append(abstractParameterInfo.toString());
        }
        rep.append(" | ");
      }
    }

    return rep.toString();
  }

}

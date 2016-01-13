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
package org.jikesrvm.tools.oth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterValueFactory;
import org.jikesrvm.adaptive.parameterprofiling.ParameterValueFactoryImpl;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.jikesrvm.compilers.opt.OptOptions;
import org.jikesrvm.compilers.opt.OptimizingCompilerException;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.compilers.opt.driver.OptimizationPlanner;
import org.jikesrvm.compilers.opt.driver.OptimizingCompiler;
import org.jikesrvm.compilers.opt.runtimesupport.OptCompiledMethod;
import org.jikesrvm.compilers.opt.specialization.ParameterValueSpecializationContext;
import org.jikesrvm.compilers.opt.specialization.SpecializationDatabase;
import org.jikesrvm.compilers.opt.specialization.SpecializedMethod;

public class StandardSpecializationLayer implements SpecializationLayer {

  public static final String RECOMPILING_GENERAL_VERSION_OF = "Recompiling general version of ";

  protected Map<RVMMethod, List<Integer>> methodsToSpecialize;
  protected Map<RVMMethod, List<OptOptions>> optionsForSpecialization;
  protected Map<RVMMethod, List<Map<Integer, String>>> specializationInfo;
  protected Map<Map<Integer, String>, Object[]> argInfo;

  protected Vector<SpecializedMethod> specializedMethods;

  protected ParameterValueFactory pvf;

  protected RVMClass klazz;

  // TODO rename field
  protected final OptTestHarnessOutput outputLayer;

  protected OptTestHarness oth;

  public StandardSpecializationLayer(OptTestHarnessOutput outputLayer) {
    this.outputLayer = outputLayer;
    methodsToSpecialize = new HashMap<RVMMethod, List<Integer>>();
    optionsForSpecialization = new HashMap<RVMMethod, List<OptOptions>>();
    specializationInfo = new HashMap<RVMMethod, List<Map<Integer,String>>>();

    specializedMethods = new Vector<SpecializedMethod>();

    pvf = new ParameterValueFactoryImpl();
  }

  @Override
  public void injectOTH(OptTestHarness oth) {
    this.oth = oth;
  }

  @Override
  public final RVMClass getSpecializationClass() {
    return klazz;
  }

  @Override
  public final void setSpecializationClass(RVMClass klazz) {
    this.klazz = klazz;

  }

  @Override
  public void addMethodForSpecialization(RVMMethod method, OptOptions options, Map<Integer, String> paramsAndValues) {
    List<Integer> versions = methodsToSpecialize.get(method);
    if (versions == null) {
      versions = new ArrayList<Integer>();
      methodsToSpecialize.put(method, versions);
    }
    Integer newVersion = Integer.valueOf(versions.size());
    versions.add(newVersion);

    List<OptOptions> optionsForSpecializedVersion = optionsForSpecialization.get(method);
    if (optionsForSpecializedVersion == null) {
      optionsForSpecializedVersion = new ArrayList<OptOptions>();
      optionsForSpecialization.put(method, optionsForSpecializedVersion);
    }
    optionsForSpecializedVersion.add(options);

    addSpecializationInfo(method, paramsAndValues);
  }

  protected final void addSpecializationInfo(RVMMethod method, Map<Integer, String> paramsAndValues) {
    List<Map<Integer, String>> specInfoForMethod = specializationInfo.get(method);
    if (specInfoForMethod == null) {
      specInfoForMethod = new ArrayList<Map<Integer,String>>();
      specializationInfo.put(method, specInfoForMethod);
    }
    specInfoForMethod.add(paramsAndValues);
  }

  @Override
  public void specializeAllMethods() {
    for (Entry<RVMMethod, List<Integer>> e: methodsToSpecialize.entrySet()) {
      List<Integer> versions = e.getValue();
      for (Integer version : versions) {
        specializeAMethod(e, version);
      }
    }
  }

  protected void specializeAMethod(Entry<RVMMethod, List<Integer>> e, Integer version) {
    NormalMethod nm = (NormalMethod) e.getKey();
    OptOptions opts = optionsForSpecialization.get(nm).get(version);
    TypeReference[] pms = nm.getParameterTypes();
    int lengthForOperands = nm.getParameterTypes().length;

    AbstractParameterInfo[] paramValues = new AbstractParameterInfo[lengthForOperands];

    Map<Integer, String> infoForMethod = specializationInfo.get(nm).get(version);
    for (Entry<Integer, String> entry : infoForMethod.entrySet()) {
      int parameterIndex = entry.getKey();
      paramValues[parameterIndex] = getValueForSpecialization(pms, parameterIndex, entry.getValue());
    }

    CompiledMethod cm = nm.getCurrentCompiledMethod();
    while (cm == null) {
      nm.compile();
      cm = nm.getCurrentCompiledMethod();
    }

    ParameterValueSpecializationContext paramContext = new ParameterValueSpecializationContext(nm, opts, paramValues);
    SpecializedMethod sp = paramContext.findOrCreateSpecializedVersion(nm);
    CompiledMethod compiledSpecializedMethod = sp.getCompiledMethod();

    try {
      if (compiledSpecializedMethod == null) {
        SpecializationDatabase.doDeferredSpecializations();
      }
      compiledSpecializedMethod = sp.getCompiledMethod();
      if (VM.VerifyAssertions) {
        VM._assert(compiledSpecializedMethod != null, "Specialised version was not compiled!");
      }

      if (VM.VerifyAssertions) {
        VM._assert(nm.getCurrentCompiledMethod() != compiledSpecializedMethod,
            "Compiled specialized method and compiled \"normal\" method were identical");
        VM._assert(SpecializationDatabase.shouldCallSpecialVersions(nm),
            "Specialized version for a normal method was not registered properly");
      }
      specializedMethods.add(sp);
    } catch (OptimizingCompilerException optException) {
      if (optException.isFatal && VM.ErrorsFatal) {
        optException.printStackTrace();
        VM.sysFail("Internal vm error: " + optException.toString());
      } else {
        System.err.println("SKIPPING opt-compilation of " + nm + ":\n  " + optException.getMessage());
        if (opts.PRINT_METHOD) {
          optException.printStackTrace();
        }
      }
    }
  }

  protected AbstractParameterInfo getValueForSpecialization(TypeReference[] pms, int i, String value) {
    TypeReference typeForValue = pms[i];
    if (typeForValue.isBooleanType()) {
      return pvf.createBooleanParameter(Boolean.parseBoolean(value));
    } else if (typeForValue.isByteType()) {
      return pvf.createByteParameter(Byte.parseByte(value));
    } else if (typeForValue.isCharType()) {
      char c = value.charAt(0);
      return pvf.createCharParameter(c);
    } else if (typeForValue.isClassType()) {
      if (value.equalsIgnoreCase("null")) {
        return pvf.createNullParameter();
      }
      return pvf.createTypeValueForObjectParameter(getTypeForClassName(value));
    } else if (typeForValue.isDoubleType()) {
      return pvf.createDoubleParameter(Double.parseDouble(value));
    } else if (typeForValue.isFloatType()) {
      return pvf.createFloatParameter(Float.parseFloat(value));
    } else if (typeForValue.isIntType()) {
      return pvf.createIntParameter(Integer.parseInt(value));
    } else if (typeForValue.isLongType()) {
      return pvf.createLongParameter(Long.parseLong(value));
    } else if (typeForValue.isShortType()) {
      return pvf.createShortParameter(Short.parseShort(value));
    }

    return null;
  }

  protected RVMType getTypeForClassName(String value) throws NoClassDefFoundError {
    try {
      return oth.loadClass(value);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Vector<SpecializedMethod> getSpecializedMethodsForInvocation() {
    return specializedMethods;
  }

  @Override
  public int getNumberOfMethodsMarkedForSpecialization() {
    return methodsToSpecialize.size();
  }

  @Override
  public void recompileGeneralMethodVersions(OptOptions optionsForGeneralMethods) {
    Set<NormalMethod> methodsToRecompile = new HashSet<NormalMethod>();
    for (SpecializedMethod sm : specializedMethods) {
      methodsToRecompile.add(sm.getMethod());
    }
    for (NormalMethod nm : methodsToRecompile) {
      recompileGeneralMethod(nm, optionsForGeneralMethods);
    }
  }

  protected void recompileGeneralMethod(NormalMethod nm, OptOptions opts) {
    CompiledMethod currentCompiledMethod = nm.getCurrentCompiledMethod();
    if (currentCompiledMethod != null) {
      nm.invalidateCompiledMethod(currentCompiledMethod);
      currentCompiledMethod = nm.getCurrentCompiledMethod();
    }

    if (VM.VerifyAssertions) {
      VM._assert(currentCompiledMethod == null, "Current compiled method was present before recompilation!");
    }

    CompiledMethod cm = null;
    CompilationPlan cp = new CompilationPlan(nm, OptimizationPlanner.createOptimizationPlan(opts), null, opts);
    cm = OptimizingCompiler.compile(cp);
    nm.replaceCompiledMethod(cm);

    if (VM.VerifyAssertions) {
      VM._assert(nm.getCurrentCompiledMethod() instanceof OptCompiledMethod, "replaced method with baseline comp method!");
    }

    outputLayer.sysOutPrintln(RECOMPILING_GENERAL_VERSION_OF + nm.getName());
  }

}

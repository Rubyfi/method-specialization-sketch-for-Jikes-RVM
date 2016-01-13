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

import java.util.List;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.parameterprofiling.ParameterProfileInformation;
import org.jikesrvm.adaptive.parameterprofiling.ParameterValueFactoryImpl;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMClassLoader;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.runtime.Callbacks;
import org.jikesrvm.runtime.Callbacks.ClassResolvedMonitor;

/**
 * A {@link SpecializationOracle} that is intended for debugging only. It returns
 * hard coded decisions to trigger method specialization for predefined methods.
 * <p>
 * Note: Use with enabled assertions, e.g. in a prototype-opt or development build.
 */
public class DebugSpecializationOracle implements SpecializationOracle, ClassResolvedMonitor {

  private boolean waitingForClassToBeLoaded;
  private int index = 0;
  private String[] classDescriptors = {"Lorg/apache/lucene/queryParser/QueryParserTokenManager;"};
  private String[] methodNames = {"jjCheckNAddStates"};
  private int[] parameterIndex = {0};
  private int[] parameterValue = {10};
  private ParameterValueFactoryImpl parameterValueFactoryImpl;

  public DebugSpecializationOracle() {
    parameterValueFactoryImpl = new ParameterValueFactoryImpl();
    Callbacks.addClassResolvedMonitor(this);
  }

  @Override
  public SpecializationDecision shouldSpecialize(RVMMethod method, List<ParameterProfileInformation> profiles, CompilationPlan plan) {
    SpecializationDecision nextDecision = pickNextDecision();
    if (nextDecision == null && !waitingForClassToBeLoaded) {
      return SpecializationDecision.newNO(method, profiles, "ALL_CANDIDATES_CREATED");
    } else if (nextDecision == null && waitingForClassToBeLoaded) {
      return SpecializationDecision.newNO(method, profiles, "WAITING_FOR_CLASS_LOADING");
    }

    return nextDecision;
  }

  private SpecializationDecision pickNextDecision() {
    if (index >= classDescriptors.length || waitingForClassToBeLoaded) {
      return null;
    }

    String className = classDescriptors[index];
    RVMMethod targetMethod = null;

    Class<?> clazz = null;
    try {
      clazz = Class.forName(className);
      RVMType type = JikesRVMSupport.getTypeForClass(clazz);
      targetMethod = searchTargetMethod(type);
      waitingForClassToBeLoaded = false;
    } catch (ClassNotFoundException e) {
      // ignore
    }

    if (targetMethod == null) {
      ClassLoader cl = RVMClassLoader.findWorkableClassloader(Atom.findOrCreateAsciiAtom(className));
      if (cl == null) {
        waitingForClassToBeLoaded = true;
        return null;
      }
      TypeReference tRef = TypeReference.findOrCreate(cl, Atom.findOrCreateAsciiAtom(className));
      tRef.resolve();
      RVMType type = tRef.peekType();
      targetMethod = searchTargetMethod(type);
    }

    if (targetMethod == null) {
      VM.sysFail("Could not find target method " + methodNames[index]);
    }

    NormalMethod nm = (NormalMethod) targetMethod;

    AbstractParameterInfo[] info = new AbstractParameterInfo[targetMethod.getParameterTypes().length];
    int paramIndex = parameterIndex[index];
    int value = parameterValue[index];
    info[paramIndex] = parameterValueFactoryImpl.createIntParameter(value);
    ParameterValueSpecializationContext paramContext = new ParameterValueSpecializationContext(nm, info);
    SpecializationDecision decision = SpecializationDecision.newYES(targetMethod, null, paramContext);

    index++;

    return decision;
  }

  private RVMMethod searchTargetMethod(RVMType type) {
    String methodName = methodNames[index];
    RVMMethod targetMethod = null;
    int targetMethodCount = 0;
    for (RVMMethod staticMethod : type.getStaticMethods()) {
      if (staticMethod.getName().toString().equals(methodName)) {
        targetMethod = staticMethod;
        targetMethodCount++;
      }
    }
    for (RVMMethod virtualmethod : type.getVirtualMethods()) {
      if (virtualmethod.getName().toString().equals(methodName)) {
        targetMethod = virtualmethod;
        targetMethodCount++;
      }
    }
    if (targetMethodCount > 1) {
      VM.sysFail("Multiple target methods were found for " + methodName + "!");
    } else if (targetMethodCount == 0) {
      VM.sysFail("Target method not found: " + methodName);
    }
    return targetMethod;
  }

  @Override
  public void notifyClassResolved(RVMClass klass) {
    if (!VM.runningVM || !VM.fullyBooted || index >= classDescriptors.length) {
      return;
    }

    String classDesc = klass.getDescriptor().toString();

    VM.sysWriteln("Class " + classDesc + " was initialized!");

    if (classDesc.equals(classDescriptors[index])) {
      waitingForClassToBeLoaded = false;
      SpecializationDecision desc = pickNextDecision();
      if (desc == null) {
        VM.sysFail("No proper decision made after resolving class " + klass);
      }

      NormalMethod nm = (NormalMethod) desc.getMethod();
      desc.getContext().findOrCreateSpecializedVersion(nm);
    }
  }

}

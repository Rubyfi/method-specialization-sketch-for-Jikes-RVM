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

import java.util.Map;
import java.util.Vector;

import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.OptOptions;
import org.jikesrvm.compilers.opt.specialization.SpecializedMethod;

public interface SpecializationLayer {

  RVMClass getSpecializationClass();

  void setSpecializationClass(RVMClass klazz);

  void addMethodForSpecialization(RVMMethod method, OptOptions options, Map<Integer, String> paramsAndValues);

  void specializeAllMethods();

  int getNumberOfMethodsMarkedForSpecialization();

  Vector<SpecializedMethod> getSpecializedMethodsForInvocation();

  void recompileGeneralMethodVersions(OptOptions optionsForGeneralMethods);

  void injectOTH(OptTestHarness oth);

}

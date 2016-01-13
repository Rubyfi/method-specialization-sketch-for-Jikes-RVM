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

import org.jikesrvm.adaptive.parameterprofiling.ParameterProfileInformation;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;

public class NeverSpecializeOracle implements SpecializationOracle {

  @Override
  public SpecializationDecision shouldSpecialize(RVMMethod method, List<ParameterProfileInformation> profiles, CompilationPlan plan) {
    return SpecializationDecision.newNO(method, profiles, "SPECIALIZATION DISABLED");
  }

}

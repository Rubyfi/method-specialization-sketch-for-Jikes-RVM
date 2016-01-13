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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.OptOptions;

public class TestSpecializationLayer extends StandardSpecializationLayer {

  private int specializedMethodsCount;
  private int recompiledMethods;
  private OptOptions optionsForRecompilingGeneralMethods;

  public TestSpecializationLayer(OptTestHarnessOutput outputLayer) {
    super(outputLayer);
  }

  public int getNumberOfSpecializedMethods() {
    return specializedMethodsCount;
  }

  @Override
  protected void specializeAMethod(Entry<RVMMethod, List<Integer>> e, Integer version) {
    super.specializeAMethod(e, version);
    specializedMethodsCount++;
  }

  public String getValueForSpecializationOfMethod(String methodName, int parameterIndex, int version) {
    RVMMethod desiredMethod = null;
    for (Entry<RVMMethod,List<Integer>> entry : methodsToSpecialize.entrySet()) {
      if (entry.getKey().getName().toString().equals(methodName)) {
        List<Integer> versions = entry.getValue();
        if (version < versions.size()) {
          desiredMethod = entry.getKey();
        } else {
          throw new RuntimeException("Desired specialized version does not exist!");
        }
      }
    }

    List<Map<Integer, String>> specInfoForVersions = specializationInfo.get(desiredMethod);
    Map<Integer, String> specializationInfo = specInfoForVersions.get(version);
    return specializationInfo.get(Integer.valueOf(parameterIndex));
  }

  @Override
  public void recompileGeneralMethodVersions(OptOptions optionsForGeneralMethods) {
    super.recompileGeneralMethodVersions(optionsForGeneralMethods);
    this.optionsForRecompilingGeneralMethods = optionsForGeneralMethods;
  }

  @Override
  protected void recompileGeneralMethod(NormalMethod nm, OptOptions opts) {
    super.recompileGeneralMethod(nm, opts);
    recompiledMethods++;
  }

  public int getNumberOfRecompiledMethods() {
    return recompiledMethods;
  }

  public OptOptions getOptionsForRecompilingGeneralMethods() {
    return optionsForRecompilingGeneralMethods;
  }

}

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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.controller.Controller;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.adaptive.parameterprofiling.MethodProfile;
import org.jikesrvm.adaptive.parameterprofiling.ParameterProfileInformation;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.compilers.opt.driver.CompilationPlan;
import org.jikesrvm.util.Pair;

public class DefaultSpecializationOracle implements SpecializationOracle {

  @Override
  public SpecializationDecision shouldSpecialize(RVMMethod method, List<ParameterProfileInformation> profiles, CompilationPlan plan) {
    LinkedList<String> reasons = new LinkedList<String>();

    boolean yes = true;
    if (profiles == null) {
      yes = false;
      reasons.add("NO_PROFILES");
    }

    // ignore methods without parameters for now
    if (method.getParameterTypes().length == 0) {
      yes = false;
      reasons.add("NO_NON_RECEIVER_PARAMS");
    }

    int maximumOptLevel = Controller.options.MAX_OPT_LEVEL;
    if (maximumOptLevel < 2) {
       if (plan.options.getOptLevel() < maximumOptLevel) {
         yes = false;
         reasons.add("OPT_LEVEL_SMALLER_THAN_MAXIMUM");
       }
    } else {
      if (plan.options.getOptLevel() < 2) {
        yes = false;
        reasons.add("OPT_LEVEL_SMALLER_THAN_TWO");
      }
    }

    ParameterValueSpecializationContext context = null;
    if (yes) {
      context = createContext(method, profiles);
    }
    if (context == null) {
      yes = false;
      reasons.add("NO_CANDIDATES_FOUND");
    }

    if (!yes) {
      StringBuilder prettyPrintedReasons = new StringBuilder();
      Iterator<String> iter = reasons.iterator();
      boolean hasNext = iter.hasNext();
      while (hasNext) {
        prettyPrintedReasons.append(iter.next());
        hasNext = iter.hasNext();
        if (hasNext) {
          prettyPrintedReasons.append(" ");
        }
      }

      return SpecializationDecision.newNO(method, profiles, prettyPrintedReasons.toString());
    }

    return SpecializationDecision.newYES(method, profiles, context);
  }

  private ParameterValueSpecializationContext createContext(RVMMethod method, List<ParameterProfileInformation> profiles) {
    ParameterProfileInformation singleProfile = profiles.get(0);
    MethodProfile mp = (MethodProfile) singleProfile;
    int paramCount = method.getParameterTypes().length;
    if (!method.isStatic()) {
      paramCount++;
    }
    // ignore receiver for now
    int startParam = method.isStatic() ? 0 : 1;
    int maxCount = Integer.MIN_VALUE;
    AbstractParameterInfo mostFrequentCandidate = null;
    int paramIndex = 0;
    for (int i = startParam; i < paramCount; i++) {
      List<Pair<AbstractParameterInfo, Integer>> candidatesForParameter = mp.getCandidatesForParameter(i);

      // Note: Types that match the parameter type in the signature are not
      // contained in the candidate list. The candidate list will
      // be empty when all types seen during profiling are exactly the same as
      // the parameter type from the signature.
      if (candidatesForParameter.size() == 0) {
        continue;
      }

      Pair<AbstractParameterInfo, Integer> pair = candidatesForParameter.get(0);

      Integer count = pair.second;
      if (count > maxCount) {
        mostFrequentCandidate = pair.first;
        paramIndex = i;
        maxCount = count;
      }
    }

    // No candidate found: Can happen for methods with one reference parameter
    // when no types other than the one from the signature were seen during
    // profiling.
    if (mostFrequentCandidate == null) {
      return null;
    }

    if (VM.VerifyAssertions) {
      boolean methodIsNormalMethod = method instanceof NormalMethod;
      if (!methodIsNormalMethod) {
        String badMethodType = "Illegal type of method for specialization " +
            method.getClass() + "! This method should have been filtered during profiling.";
        VM._assert(VM.NOT_REACHED, badMethodType);
      }
    }

    NormalMethod nm = (NormalMethod) method;
    AbstractParameterInfo[] paramValues = new AbstractParameterInfo[method.getParameterTypes().length];
    int offset = method.isStatic() ? 0 : -1;
    paramValues[paramIndex + offset] = mostFrequentCandidate;
    return new ParameterValueSpecializationContext(nm, paramValues);
  }

}

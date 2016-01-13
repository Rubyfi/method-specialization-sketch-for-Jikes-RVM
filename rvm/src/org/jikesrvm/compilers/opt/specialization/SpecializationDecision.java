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
import org.jikesrvm.classloader.TypeReference;

public final class SpecializationDecision {

  private final boolean yes;
  private final List<ParameterProfileInformation> profileData;
  private final RVMMethod method;
  private final String reason;
  private ParameterValueSpecializationContext context;

  private SpecializationDecision(RVMMethod method, boolean yes, List<ParameterProfileInformation> profileData) {
    this(method, yes, profileData, "NO_REASON");
  }

  private SpecializationDecision(RVMMethod method, boolean yes, List<ParameterProfileInformation> profileData, String reason) {
    this.method = method;
    this.yes = yes;
    this.profileData = profileData;
    this.reason = reason;
  }

  public static SpecializationDecision newYES(RVMMethod method, List<ParameterProfileInformation> profileData, ParameterValueSpecializationContext context) {
    SpecializationDecision decision = new SpecializationDecision(method, true, profileData);
    decision.setContext(context);
    return decision;
  }

  public static SpecializationDecision newNO(RVMMethod method, List<ParameterProfileInformation> profileData) {
    return new SpecializationDecision(method, false, profileData);
  }

  public static SpecializationDecision newNO(RVMMethod method, List<ParameterProfileInformation> profileData, String reason) {
    return new SpecializationDecision(method, false, profileData, reason);
  }

  public boolean isYES() {
    return yes;
  }

  public boolean isNO() {
    return !isYES();
  }

  public void setContext(ParameterValueSpecializationContext context) {
    this.context = context;
  }

  public ParameterValueSpecializationContext getContext() {
    return context;
  }

  public RVMMethod getMethod() {
    return method;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SPEC_DECISION");
    sb.append(" ");
    sb.append(method.getDeclaringClass());
    sb.append(" ");
    sb.append(method.getName());
    sb.append(" ");
    sb.append(method.getDescriptor());
    sb.append(" ");
    boolean isStatic = method.isStatic();
    String type = isStatic ? "STATIC" : "INSTANCE";
    sb.append(type);
    sb.append(" ");

    int primitive = 0;
    int realObjs = 0;
    int arrays = 0;
    for (TypeReference tref : method.getParameterTypes()) {
      if (tref.isReferenceType()) {
        if (tref.isArrayType()) {
          arrays++;
        } else {
          realObjs++;
        }
      } else {
        primitive++;
      }
    }
    sb.append("PRIM_PARAMS");
    sb.append(" ");
    sb.append(primitive);
    sb.append(" ");
    sb.append("REAL_OBJ_PARAMS");
    sb.append(" ");
    sb.append(realObjs);
    sb.append(" ");
    sb.append("ARRAY_PARAMS");
    sb.append(" ");
    sb.append(arrays);
    sb.append(" ");
    String decision = yes ? "YES" : "NO";
    sb.append(decision);
    sb.append(" ");
    sb.append(reason);
    sb.append("\n");
    if (yes) {
      sb.append(context.toString());
      sb.append("\n");
    }
    if (profileData != null) {
      sb.append(" PROFILES: \n");
      for (ParameterProfileInformation ppi : profileData) {
        sb.append("\t");
        sb.append(ppi);
        sb.append("\n");
      }
    }
    sb.append("\n");
    return sb.toString();
  }

}

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
package org.vmmagic.pragma;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.vmmagic.Pragma;

/**
 * This pragma indicates that a method makes assumptions about the call stack, e.g.
 * because it examines other frames on the call stack using Magic. This pragma
 * is not necessarily applied to all cases with assumptions but it should cover
 * those case that can cause errors.
 * <p>
 * Note that this pragma is not applied to classes annotated with {@link DynamicBridge}
 * or {@link NativeBridge} as those are not subject to inlining or specialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR,ElementType.METHOD,ElementType.TYPE})
@Pragma
public @interface MakesAssumptionsAboutCallStack {

  public enum How {
    /**
     * Makes assumptions directly.
     */
    Direct,
    /**
     * Calls a method or uses a class that makes such assumptions.
     */
    Transitive
  }
  How value() default How.Direct;

}

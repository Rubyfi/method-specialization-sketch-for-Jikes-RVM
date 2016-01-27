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

import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.compilers.common.CompiledMethod;

abstract class SpecializationContext {

  /**
   * Finds or creates a specialized version of source for this context.
   * <p>
   * If no specialized version was found in the {@link SpecializationDatabase},
   * a {@link SpecializedMethod} will be allocated and registered in the
   * database.
   * <p>
   * Note that the specialized version will <b>NOT</b> be compiled immediately.
   * Compilation of the specialized version can be forced via a call to
   * {@link SpecializationDatabase#doDeferredSpecializations()}.
   * <p>
   * The opt compiler will make such a call after compiling a method, see
   * {@link org.jikesrvm.compilers.opt.driver.OptimizingCompiler#compile(org.jikesrvm.compilers.opt.driver.CompilationPlan)}.
   *
   *
   * @param source the method that needs a specialized version
   * @return the created specialized method
   */
  public SpecializedMethod findOrCreateSpecializedVersion(NormalMethod source) {
    SpecializedMethod spMethod = searchSpecializedVersionInDatabase(source);
    if (spMethod == null) {
      spMethod = createSpecializedMethod(source);
    }

    SpecializationDatabase.registerSpecialVersion(spMethod);
    return spMethod;
  }

  /**
   * Searches a specialized version of the given method in the database. A
   * specialized method in the database matches if both the source method and
   * the {@link SpecializationContext} (i.e. this context) match.
   *
   * @param source
   *          the normal method that
   * @return a specialized version or <code>null</code> if no matching
   *         specialized version was found
   */
  protected final SpecializedMethod searchSpecializedVersionInDatabase(NormalMethod source) {
    java.util.Iterator<SpecializedMethod> versions = SpecializationDatabase.getSpecialVersions(source);
    if (versions != null) {
      while (versions.hasNext()) {
        SpecializedMethod spMethod = versions.next();
        SpecializationContext context = spMethod.getSpecializationContext();
        if (context == this) {
          return spMethod;
        }
      }
    }

    return null;
  }


  /**
   * Generates code for a specialized version of source in this
   * context.
   *
   * @param source the method whose specialized version will be
   *  compiled
   * @return the compiled method associated with the specialized method
   *  version
   */
  abstract CompiledMethod specialCompile(NormalMethod source);

  /**
   * Create specialized method in this context.
   *
   * @param method the normal method that will be specialized
   * @return the specialized method for the normal method
   */
  protected SpecializedMethod createSpecializedMethod(NormalMethod method) {
    return (new SpecializedMethod(method, this));
  }

}

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
package org.jikesrvm.adaptive.measurements.listeners.parameterprofiling;

import static org.jikesrvm.VM.NOT_REACHED;
import static org.jikesrvm.compilers.common.CompiledMethod.BASELINE;
import static org.jikesrvm.compilers.common.CompiledMethod.JNI;
import static org.jikesrvm.compilers.common.CompiledMethod.OPT;
import static org.jikesrvm.compilers.common.CompiledMethod.TRAP;
import static org.jikesrvm.ia32.StackframeLayoutConstants.INVISIBLE_METHOD_ID;
import static org.jikesrvm.ia32.StackframeLayoutConstants.STACKFRAME_SENTINEL_FP;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_DOUBLE;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_FLOAT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_INT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_LONG;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_ADDRESS;
import static org.jikesrvm.scheduler.RVMThread.BACKEDGE;
import static org.jikesrvm.scheduler.RVMThread.EPILOGUE;
import static org.jikesrvm.scheduler.RVMThread.PROLOGUE;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.AosEntrypoints;
import org.jikesrvm.adaptive.controller.Controller;
import org.jikesrvm.adaptive.measurements.listeners.ContextListener;
import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.compilers.baseline.BaselineCompiledMethod;
import org.jikesrvm.compilers.baseline.ia32.BaselineCompilerImpl;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.jikesrvm.compilers.common.CompiledMethods;
import org.jikesrvm.ia32.StackframeLayoutConstants;
import org.jikesrvm.mm.mmtk.Lock;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.scheduler.Synchronization;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.NoCheckStore;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;

/**
 * A listener that extracts parameter values from methods on the stack.
 * <p>
 * Note that all internal methods are marked with {@link Inline} because listeners
 * are supposed to be as fast as possible.
 */
@Uninterruptible
public final class ParameterListener extends ContextListener {

  private static final boolean DEBUG = false;

  private static final int CAPACITY = 20000;

  /**
   * The number of samples a listener takes before activating its organizer.
   */
  protected static final int SAMPLE_COUNT = 1000;

  private static final Lock indexCalculation = new Lock("Index calculation");

  public static final int NO_ENTRY = -1;

  /**
   * Should VM-internal methods be profiled?
   */
  private static boolean PROFILE_VM_METHODS;

  protected RVMMethod[] methods;

  protected int[] paramStartIndexes;

  private final EncodingHelper encodingHelper;

  /**
   * The number of samples that this listener will take before activating its
   * organizer the next time.
   * <p>
   * Accessed via {@link AosEntrypoints}.
   */
  protected int samplesLeftToTake;

  /**
   * The number of update calls that are currently active.
   * <p>
   * Accessed via {@link AosEntrypoints}.
   */
  protected int activeUpdateCalls;

  private static final Lock dumpLock = new Lock("ParameterDumping");

  private static final Lock updateCalls = new Lock("UpdateCalls");

  /**
   * Accessed via {@link AosEntrypoints}.
   */
  protected int nextParamIndex;

  //TODO consider extracting counters to separate class

  /* Counters for debugging. These do not need to be accurate, so
   * do not bother with synchronization. */

  protected int backedge;

  protected int prologue;

  protected int epilogue;

  protected int otherYieldpoints;

  /**
   * The total number of samples that were skipped because it was impossible to take data.
   */
  protected int skippedSamples;

  /**
   * The number of samples that were skipped because the target method was compiled with
   * the optimizing compiler.
   */
  protected int skippedSamplesOpt;

  protected int skippedSamplesOptPrologue;

  protected int skippedSamplesOptBackedge;

  protected int skippedSamplesOptEpilogue;

  protected int skippedSamplesOrganizerBusy;

  protected int takenSamples;

  public static void enableProfilingOfVMMethods() {
    PROFILE_VM_METHODS = true;
  }

  /**
   * Constructs a listener that extracts parameter information from methods on
   * the stack.
   */
  public ParameterListener() {
    encodingHelper = new EncodingHelper(CAPACITY);
    methods = new RVMMethod[SAMPLE_COUNT];
    paramStartIndexes = new int[SAMPLE_COUNT];
    reset();
  }

  /**
   * Extract a parameter profile for relevant methods. Currently only supports
   * primitive types and objects.
   * <p>
   * TODO Support more complex parameter profiles, e.g. for boxing types or
   * word-like types.
   */
  @Override
  public void update(Address sfp, int whereFrom) {
    if (whereFrom == PROLOGUE) {
      prologue++;
    } else if (whereFrom == BACKEDGE) {
      backedge++;
    } else if (whereFrom == EPILOGUE) {
      epilogue++;
    } else {
      otherYieldpoints++;
    }

    int methodCMID = Magic.getCompiledMethodID(sfp);
    if (!methodExistsForID(methodCMID)) {
      skippedSamples++;
      return;
    }

    CompiledMethod compiledMethod = CompiledMethods.getCompiledMethod(methodCMID);
    if (isNoLongerRelevant(compiledMethod) || isNativeOrTrap(compiledMethod)) {
      skippedSamples++;
      return;
    }

    if (isOptCompiled(compiledMethod)) {
      skippedSamplesOpt++;
      if (whereFrom == PROLOGUE) {
        skippedSamplesOptPrologue++;
      } else if (whereFrom == BACKEDGE) {
        skippedSamplesOptBackedge++;
      } else if (whereFrom == EPILOGUE) {
        skippedSamplesOptEpilogue++;
      }
      skippedSamples++;
      return;
    }

    RVMMethod m = compiledMethod.getMethod();
    if (isUninteresting(m) || usesUnboxedTypes(m) || impossibleToSpecialize(m) || isUnsafeToSpecialize(m)) {
      skippedSamples++;
      return;
    }

    // Ignore calls from backedge and epilogue till I have an analysis that
    // determines if a method writes to its parameters
    if (whereFrom != PROLOGUE) {
      skippedSamples++;
      return;
    }

    if (VM.VerifyAssertions && !isBaselineCompiled(compiledMethod)) {
      VM._assert(NOT_REACHED, "Unhandled compiler");
    }

    RVMClass declaringClass = m.getDeclaringClass();
    boolean isVMMethod = declaringClass.getDescriptor().isRVMDescriptor();
    if (isVMMethod && !PROFILE_VM_METHODS) {
      skippedSamples++;
      return;
    }

    BaselineCompiledMethod baselineCompMethod = (BaselineCompiledMethod) compiledMethod;
    int spaceNeeded = calculateSpaceNeededForParameters(compiledMethod);

    boolean registered = attemptToRegisterUpdateCall();
    if (!registered) {
      skippedSamples++;
      skippedSamplesOrganizerBusy++;
      return;
    }

    // Need to ensure that all parts of index calculation match. It is unproblematic
    // if the sample ends up not being taken. This just means that no index will be written
    // and the "gap" in the indexes will be skipped when the organizer processes the data.
    indexCalculation.acquire();
    int paramIndexForThisSample = Synchronization.fetchAndAdd(this, AosEntrypoints.paramListenerParamField.getOffset(),
        spaceNeeded);
    int samplesLeft = Synchronization.fetchAndDecrement(this, AosEntrypoints.paramListenerSamplesLeftToTakeField.getOffset(),
        1);
    indexCalculation.release();

    int index = SAMPLE_COUNT - samplesLeft;
    if (noSpaceForParameters(paramIndexForThisSample) || samplesLeft <= 0) {
      unregisterUpdateCall(true);

      // Must discard this sample because there is no space for its information
      skippedSamples++;
      return;
    }

    if (stackEndReached(sfp)) {
      if (VM.VerifyAssertions) {
        VM.sysFail("ParameterListener attempted to walk off the stack");
      }
      ParameterUtilities.writeForDebuggingLn("ParameterListener attempted to walk off the stack, returning now to cancel attempt");
      skippedSamples++;
      return;
    }

    dumpLock.acquire();

    storeRVMMethod(compiledMethod.getMethod(), index);
    paramStartIndexes[index] = paramIndexForThisSample;

    dumpParameters(sfp, baselineCompMethod, paramIndexForThisSample);

    dumpLock.release();

    unregisterUpdateCall(false);

    takenSamples++;
  }

  @Inline
  private boolean attemptToRegisterUpdateCall() {
    updateCalls.acquire();
    if (!isActive()) {
      updateCalls.release();
      return false;
    }
    Synchronization.fetchAndAdd(this, AosEntrypoints.activeUpdateCallsField.getOffset(), 1);
    updateCalls.release();
    return true;
  }

  @Inline
  private void unregisterUpdateCall(boolean activateOrganizer) {
    updateCalls.acquire();
    int preDecrementValue = Synchronization.fetchAndDecrement(this, AosEntrypoints.activeUpdateCallsField.getOffset(), 1);
    if (preDecrementValue == 1 && activateOrganizer) {
      if (isActive()) {
        activateOrganizer();
      }
    }
    updateCalls.release();
  }

  /**
   * Is it impossible to specialize this method? This applies to methods bridging from
   * and to native code. Those methods' calling convention is not supported by the
   * opt compiler. It is also impossible to specialize methods that are not supposed
   * to be opt-compiled.
   *
   * @param m a method
   * @return {@code true} if the given method is a bridge method or must not be
   *  opt compiled
   *
   * @see org.vmmagic.pragma.NativeBridge NativeBridge
   * @see org.vmmagic.pragma.DynamicBridge DynamicBridge
   */
  @Inline
  protected static boolean impossibleToSpecialize(RVMMethod m) {
    RVMClass declaringClass = m.getDeclaringClass();
    boolean isBridge = declaringClass.hasDynamicBridgeAnnotation() ||
        declaringClass.hasBridgeFromNativeAnnotation();
    boolean noOptCompile = m.hasNoOptCompileAnnotationUnint();
    return isBridge || noOptCompile;
  }

  /**
   * Is it unsafe to specialize the given method? Specialization of a method is considered
   * unsafe if inserting calls in the method is disallowed or discouraged, i.e. if
   * the method is unpreemptible or uninterruptible.
   *
   * @param m the method
   * @return <code>true</code> if this method is not safe for specialization
   */
  @Inline
  protected static boolean isUnsafeToSpecialize(RVMMethod m) {
    RVMClass declaringClass = m.getDeclaringClass();

    boolean definitivelyUnsafe = m.isUninterruptible() || m.isUnpreemptible() ||
        m.hasMakesAssumptionsAboutCallStackAnnotation() || declaringClass.hasMakesAssumptionsAboutCallStackAnnotation() ||
        // other invocation mechanism, do not profile
        m.isSpecializedInvoke();
    boolean probablyUnsafe = declaringClass.hasUninterruptibleNoWarnAnnotation() ||
        declaringClass.hasUnpreemptibleNoWarnAnnotation() ||
        m.hasLogicallyUninterruptibleAnnotation();
    return definitivelyUnsafe || probablyUnsafe;
  }

  /**
   * Does this method use unboxed types as parameters or return values?
   * @param m a method
   * @return <code>true</code> if and only if this method uses an unboxed type
   *  as a return value or as a parameter type
   */
  @Inline
  protected static boolean usesUnboxedTypes(RVMMethod m) {
    for (TypeReference tr : m.getParameterTypes()) {
      if (tr.isUnboxedArrayType() || tr.isUnboxedType()) {
        return true;
      }
    }

    TypeReference returnType = m.getReturnType();
    return returnType.isUnboxedType() || returnType.isUnboxedArrayType();
  }


  @NoCheckStore
  @Uninterruptible
  @Inline
  private void storeRVMMethod(RVMMethod method, int index) {
    if (VM.VerifyAssertions) {
      VM._assert(method != null, "storeRVMMethod: Method was null!");
    }
    methods[index] = method;
  }

  @Inline
  protected boolean noSpaceForParameters(int paramIndexForThisSample) {
    return paramIndexForThisSample >= SAMPLE_COUNT;
  }

  @Inline
  protected static int calculateSpaceNeededForParameters(CompiledMethod compiledMethod) {
    RVMMethod method = compiledMethod.getMethod();

    int neededBytes = 0;
    if (!method.isStatic()) {
      neededBytes += EncodingHelper.getBytesForTypeReference(TypeReference.Class);
    }

    TypeReference[] parameterTypes = method.getParameterTypes();
    for (TypeReference typeRef : parameterTypes) {
      int bytes = EncodingHelper.getBytesForTypeReference(typeRef);
      neededBytes += bytes;
    }

    return neededBytes;
  }

  /**
   * Is this method no longer relevant, e.g. because it won't be invoked in the
   * future?
   *
   * @param compiledMethod
   *          the compiled method
   * @return <code>true</code> if this method is not relevant for future
   *         executions, <code>false</code> otherwise
   */
  @Inline
  protected static boolean isNoLongerRelevant(CompiledMethod compiledMethod) {
    return compiledMethod.isOutdated() || compiledMethod.isInvalid();
  }

  /**
   * Is this method uninteresting?
   * <p>
   * Methods are considered uninteresting if they are not subject to inlining
   * (e.g. runtime service methods) or are only run once (e.g. class
   * initializers).
   *
   * @param m
   *          the method
   * @return <code>true</code> if this method is uninteresting,
   *         <code>false</code> otherwise
   */
  @Inline
  protected static boolean isUninteresting(RVMMethod m) {
    boolean onlyRunOnce = m.isClassInitializer();
    return m.getDeclaringClass().hasBridgeFromNativeAnnotation() || m.isSysCall() || m.isRuntimeServiceMethod() || onlyRunOnce;
  }

  /**
   * Dump all parameters of the compiled method except the "implicit this"
   * pointer for instance methods.
   *
   * @param sfp
   *          pointer to the stack frame
   * @param baselineCompMethod
   *          the (baseline) compiled method that is being examined
   * @param indexForParams
   *          starting index where parameters should be saved
   * @see ParameterListener#dumpImplicitThisParameter(Address, Offset, int)
   */
  @Inline
  protected void dumpParameters(Address sfp, BaselineCompiledMethod baselineCompMethod, int indexForParams) {
    Offset zeroOffset = Offset.zero();
    short paramStartOffset = baselineCompMethod.getGeneralLocalLocation(0);
    Offset paramStart = zeroOffset.plus(BaselineCompilerImpl.locationToOffset(paramStartOffset));

    Offset currentParamOffset = paramStart;

    int currentIndexForParams = indexForParams;
    if (!baselineCompMethod.getMethod().isStatic()) {
      currentParamOffset = dumpImplicitThisParameter(sfp, currentParamOffset, currentIndexForParams);
      currentIndexForParams = newIndexForParams(currentIndexForParams, TypeReference.Class);
    }

    TypeReference[] parameterTypes = baselineCompMethod.getMethod().getParameterTypes();

    for (int typeIndex = 0; typeIndex < parameterTypes.length; typeIndex++, currentParamOffset = offsetForNextStackframeSlot(currentParamOffset)) {
      TypeReference ref = parameterTypes[typeIndex];
      currentParamOffset = dumpPararameter(sfp, currentParamOffset, ref, currentIndexForParams);
      currentIndexForParams = newIndexForParams(currentIndexForParams, ref);
    }

  }

  @Inline
  private int newIndexForParams(int currentIndexForParams, TypeReference ref) {
    int newIndex = currentIndexForParams;
    if (ref.isBooleanType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Boolean);
    } else if (ref.isByteType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Byte);
    } else if (ref.isCharType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Char);
    } else if (ref.isDoubleType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Double);
    } else if (ref.isFloatType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Float);
    } else if (ref.isIntType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Int);
    } else if (ref.isLongType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Long);
    } else if (ref.isShortType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Short);
    } else if (ref.isReferenceType()) {
      newIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Class);
    } else {
      if (ref.isWordLikeType()) {
        // do nothing
      } else if (ref.isCodeType()) {
        // do nothing
      } else {
        // do nothing
      }
    }

    return newIndex;
  }

  /**
   * Dump the implicit this parameter.
   *
   * @param sfp
   *          stack frame pointer
   * @param currentParamOffset
   *          offset for the this parameter
   * @param indexForParams
   *          starting index for saving of implicit this parameter
   * @return offset for the next parameter
   */
  @Inline
  protected Offset dumpImplicitThisParameter(Address sfp, Offset currentParamOffset, int indexForParams) {
    int typeSize = BYTES_IN_ADDRESS;
    // First parameter should be the "implicit this" pointer

    Address implicitThisParameterAddr = calulcateParameterAddress(sfp, currentParamOffset, typeSize);
    Object implicitThisParameter = buildObjectFromAddress(loadObjectAddressFromAddress(implicitThisParameterAddr));

    if (implicitThisParameter != null) {
      RVMType type = ObjectModel.getObjectType(implicitThisParameter);

      encodingHelper.encodeType(indexForParams, type);

    } else {
      if (VM.VerifyAssertions) {
        VM._assert(NOT_REACHED, "Implicit this parameter could not be found!");
      }
    }
    return offsetForNextStackframeSlot(currentParamOffset);
  }

  /**
   * Dump a parameter.
   *
   * @param sfp
   *          stack frame pointer
   * @param currentParamOffset
   *          offset for the current parameter
   * @param ref
   *          type reference for the current parameter
   * @param indexForParams
   *          starting index to save a single parameter
   * @return Offset for the next Parameter
   */
  @Inline
  protected Offset dumpPararameter(Address sfp, Offset currentParamOffset, TypeReference ref, int indexForParams) {
    int typeSize;
    if (ref.isReferenceType()) {
      typeSize = BYTES_IN_ADDRESS;
      Address stackSlot = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      Object obj = null;

      boolean done = false;
      Address objectsAddress = loadObjectAddressFromAddress(stackSlot);
      if (objectsAddress.isZero()) {
        encodingHelper.encodeType(indexForParams, null);
        done = true;
      }

      if (!done) {
        obj = buildObjectFromAddress(objectsAddress);

        RVMType realType = null;
        if (obj != null) {
          realType = ObjectModel.getObjectType(obj);

          encodingHelper.encodeType(indexForParams, realType);

          // writeForDebugging(realType.getDescriptor());
          // writeForDebuggingLn();
        }
      }

    } else if (ref.isBooleanType()) {
      typeSize = BYTES_IN_INT;
      Address booleanAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      int tempInt = booleanAddress.loadInt();
      boolean bool = (tempInt != 0);
      encodingHelper.encodeBoolean(indexForParams, bool);
    } else if (ref.isByteType()) {
      typeSize = BYTES_IN_INT;
      Address byteAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      byte b = (byte) byteAddress.loadInt();
      encodingHelper.encodeByte(indexForParams, b);
    } else if (ref.isCharType()) {
      typeSize = BYTES_IN_INT;
      Address charAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      char c = (char) charAddress.loadInt();
      encodingHelper.encodeChar(indexForParams, c);

    } else if (ref.isDoubleType()) {
      typeSize = BYTES_IN_DOUBLE;
      Address doubleAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      double d = doubleAddress.loadDouble();
      encodingHelper.encodeDouble(indexForParams, d);

      // double occupies two slots, so skip a slot
      currentParamOffset = offsetForNextStackframeSlot(currentParamOffset);
    } else if (ref.isFloatType()) {
      typeSize = BYTES_IN_FLOAT;
      Address floatAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      float f = floatAddress.loadFloat();
      encodingHelper.encodeFloat(indexForParams, f);
    } else if (ref.isIntType()) {
      typeSize = BYTES_IN_INT;
      Address intAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      int i = intAddress.loadInt();
      encodingHelper.encodeInt(indexForParams, i);
    } else if (ref.isLongType()) {
      typeSize = BYTES_IN_LONG;
      Address longAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      long l = longAddress.loadLong();
      encodingHelper.encodeLong(indexForParams, l);

      // long occupies two slots, so skip a slot
      currentParamOffset = offsetForNextStackframeSlot(currentParamOffset);
    } else if (ref.isShortType()) {
      typeSize = BYTES_IN_INT;
      Address shortAddress = calulcateParameterAddress(sfp, currentParamOffset, typeSize);

      short s = (short) shortAddress.loadInt();
      encodingHelper.encodeShort(indexForParams, s);
    } else {
      // Unboxed types are not currently supported. It would be possible to
      // support them but it's probably not worth the effort for an initial
      // implementation, especially because there's no specialization during
      // bootimage writing.
      VM.sysFail("Saw unsupported type in ParameterListener!");
    }
    return currentParamOffset;
  }

  /**
   * Calculates the address on the stack that should be used to load a
   * parameter.
   *
   * @param sfp
   *          stack frame pointer
   * @param currentParamOffset
   *          the offset for the current parameter, relative to the stack frame
   *          pointer
   * @param typeSize
   *          the size of the parameter's type, in bytes
   * @return the address to load the parameter from
   */
  @Inline
  protected static Address calulcateParameterAddress(Address sfp, Offset currentParamOffset, int typeSize) {
    return sfp.plus(currentParamOffset.minus(typeSize));
  }

  /**
   * Build an object from an address.
   *
   * @param a
   *          the address
   * @return the object that lies at the given address
   */
  @Inline
  private static Object buildObjectFromAddress(Address a) {
    Object result = a.toObjectReference().toObject();

    if (DEBUG) {
      Object sameResult = Magic.addressAsObject(a);

      Object tempObj = a.toObjectReference().toObject();
      ObjectReference or = ObjectReference.fromObject(tempObj);
      Object thirdPossibility = or.toObject();

      if (result != sameResult) {
        VM.sysFail("Inconsistent object building ...");
      } else if (result != thirdPossibility) {
        VM.sysFail("Inconsistent object building, Case 2 ...");
      }
    }
    return result;
  }

  /**
   * Loads an address from the given address.
   *
   * @param slotOnStack
   *          the address to load from
   * @return the address loaded from the given address
   */
  @Inline
  private Address loadObjectAddressFromAddress(Address slotOnStack) {
    Address objAddress = slotOnStack.loadAddress();
    return objAddress;
  }

  /**
   * Calculates the offset for the next stack frame slot. This method assumes
   * all parameters are 1 word wide, i.e. it does <b>NOT</b> do special handling
   * for long and double which are 2 words wide.
   *
   * @param currentParamOffset
   *          the offset for the current parameter
   * @return the offset for the next slot in the stack frame, assuming the
   *         current parameter was 1 word wide
   */
  @Inline
  protected Offset offsetForNextStackframeSlot(Offset currentParamOffset) {
    return currentParamOffset.minus(StackframeLayoutConstants.BYTES_IN_STACKSLOT);
  }

  /**
   * Calculates the total number of parameter words for a given method.
   *
   * @param method
   *          the method
   * @return the total number of words used for parameters including the
   *         implicit <code>this</code> parameter, if any
   */
  @Inline
  protected static int getTotalNumberOfParametersWords(RVMMethod method) {
    int parameterWords = method.getParameterWords();
    if (!method.isStatic()) {
      parameterWords++;
    }
    return parameterWords;
  }

  /**
   * Was this method compiled by the baseline compiler?
   *
   * @param compiledMethod
   *          the compiled method to check
   * @return <code>true</code> if the method was compiled with the baseline
   *         compiler, <code>false</code> otherwise
   */
  @Inline
  protected boolean isBaselineCompiled(CompiledMethod compiledMethod) {
    return compiledMethod.getCompilerType() == BASELINE;
  }

  /**
   * Was this method compiled by the opt compiler?
   *
   * @param compiledMethod
   *          the compiled method to check
   * @return <code>true</code> if the method was compiled with the opt compiler,
   *         <code>false</code> otherwise
   */
  @Inline
  protected static boolean isOptCompiled(CompiledMethod compiledMethod) {
    return compiledMethod.getCompilerType() == OPT;
  }

  /**
   * Is this method a native method or does it belong to a hardware trap?
   *
   * @param compiledMethod
   *          the compiled method to check
   * @return <code>true</code> if the method transitions from Java to C (JNI) or
   *         belongs a special frame for trap handling (TRAP)
   */
  @Inline
  protected static boolean isNativeOrTrap(CompiledMethod compiledMethod) {
    return compiledMethod.getCompilerType() == TRAP || compiledMethod.getCompilerType() == JNI;
  }

  /**
   * Is there a {@link RVMMethod} for the given ID?
   *
   * @param methodCMID
   *          the method's ID
   * @return <code>true</code> if the ID belongs to a normal, compiled method,
   *         <code>false</code> if the there is no source method because the ID
   *         was taken from an "assembler" frame
   */
  @Inline
  protected static boolean methodExistsForID(int methodCMID) {
    return methodCMID != INVISIBLE_METHOD_ID;
  }

  /**
   * Have we reached the end of the stack?
   *
   * @param sfp
   *          pointer to the stack frame
   * @return <code>true</code> if the end of the stack has been reached,
   *         <code>false</code> otherwise
   */
  @Inline
  protected static boolean stackEndReached(Address sfp) {
    return sfp.loadAddress().EQ(STACKFRAME_SENTINEL_FP);
  }

  @Override
  public void report() {
    if (Controller.options.LOGGING_LEVEL >= 1) {
      ParameterUtilities.writeForDebuggingLn();
      ParameterUtilities.writeForDebugging("Parameter Listener Report\n");

      int totalPotentialSamples = (prologue - skippedSamplesOptPrologue) + skippedSamplesOrganizerBusy;
      ParameterUtilities.writeForDebugging("TOTAL_POTENTIAL_SAMPLES");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(totalPotentialSamples);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Total number of samples that could have been taken, i.e. samples from prologue yieldpoints in baseline compiled methods)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_ORGANIZER_BUSY");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(skippedSamplesOrganizerBusy);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that were skipped because organizer was busy processing the current buffer)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SAMPLES_PROLOGUE");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(prologue);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that came from prologue yieldpoints in all kinds methods (taken or not))");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SAMPLES_BACKEDGE");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(backedge);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that came from backedge yieldpoints in all kinds methods (taken or not))");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SAMPLES_EPILOGUE");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(epilogue);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that came from epilogue yieldpoints in all kinds methods (taken or not))");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_SAMPLES_PROLOGUE_OPT");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(skippedSamplesOptPrologue);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that were skipped at prologue yieldpoints in opt-compiled methods)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_SAMPLES_BACKEDGE_OPT");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(skippedSamplesOptBackedge);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("Samples that were skipped at backedge yieldpoints in opt-compiled methods)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_SAMPLES_EPILOGUE_OPT");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(skippedSamplesOptEpilogue);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that were skipped at epilogue yieldpoints in opt-compiled methods)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_OTHER_YIELDPOINTS");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(otherYieldpoints);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that came from uninteresting yieldpoints)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("SKIPPED_OPT");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(skippedSamplesOpt);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that came from interesting yieldpoints but were skipped because methods were opt-compiled)");
      ParameterUtilities.writeForDebugging("\n");

      int samplesSkippedForOtherReasons = skippedSamples - skippedSamplesOrganizerBusy - skippedSamplesOpt -
          (backedge - skippedSamplesOptBackedge) - (epilogue  - skippedSamplesOptEpilogue) - otherYieldpoints;

      ParameterUtilities.writeForDebugging("TOTAL_SKIPPED_REST");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(samplesSkippedForOtherReasons);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that were skipped but do not match any of the previous skipping reasons)");
      ParameterUtilities.writeForDebugging("\n");

      ParameterUtilities.writeForDebugging("TOTAL_TAKEN_SAMPLES");
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging(takenSamples);
      ParameterUtilities.writeForDebugging("\t");
      ParameterUtilities.writeForDebugging("(Samples that were taken)");
      ParameterUtilities.writeForDebugging("\n");


      ParameterUtilities.writeForDebuggingLn();
    }
  }

  @Override
  public void reset() {
    // Don't reset counters for backedge, prologue, epilogue and skippedSamples
    // because these are supposed to be persistent

    // Reset fields using synchronization primitives - for consistency with other accesses of the fields
    Synchronization.fetchAndStore(this, AosEntrypoints.paramListenerSamplesLeftToTakeField.getOffset(),
        SAMPLE_COUNT);
    Synchronization.fetchAndStore(this, AosEntrypoints.paramListenerParamField.getOffset(),
        0);

    clearSavedMethods();
    for (int i = 0; i < paramStartIndexes.length; i++) {
      paramStartIndexes[i] = NO_ENTRY;
    }

    encodingHelper.reset();
  }

  @NoCheckStore
  private void clearSavedMethods() {
    for (int i = 0; i < methods.length; i++) {
      methods[i] = null;
    }
  }

  public ParameterDecoder getParameterDecoder() {
    return encodingHelper;
  }

  public int[] getParamStartIndexes() {
    return paramStartIndexes;
  }

  public RVMMethod[] getMethods() {
    return methods;
  }

}

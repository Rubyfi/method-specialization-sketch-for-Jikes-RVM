/*
 * (C) Copyright IBM Corp. 2001
 */
//$Id$
package com.ibm.JikesRVM.classloader;

import com.ibm.JikesRVM.*;

/**
 * Runtime system mechanisms and data structures to implement interface invocation.
 * 
 * We support five mechanisms:
 * <pre>
 *   IMT-based (Alpern, Cocchi, Fink, Grove, and Lieber OOPSLA'01). 
 *      - embedded directly in the TIB
 *      - indirectly accessed off the TIB
 *   ITable-based
 *     - directly indexed (by interface id) iTables. 
 *     - searched (at dispatch time)
 *   Naive, class object is searched for matching method on every dispatch.
 * </pre>
 * 
 * @author Bowen Alpern
 * @author Stephen Fink
 * @author Dave Grove
 */
public class VM_InterfaceInvocation implements VM_TIBLayoutConstants {

  /*
   * PART I: runtime routines to implement the invokeinterface bytecode.
   *         these routines are called from the generated code
   *         as part of the interface invocation sequence.
   */

  /**
   * Resolve an interface method call.
   * This routine is never called by the IMT-based dispatching code.
   * It is only called for directly indexed ITables when the table
   * index was unknown at compile time (ie the target Interface was not loaded).
   *
   * @param target object to which interface method is to be applied
   * @param dictionaryId interface method sought (VM_MethodDictionary id)
   * @return machine code corresponding to desired interface method
   */
  public static INSTRUCTION[] invokeInterface(Object target, int dictionaryId) 
    throws IncompatibleClassChangeError, VM_ResolutionException {
    
    VM_Method sought = 
      VM_MethodDictionary.getValue(dictionaryId).resolveInterfaceMethod(true);
    VM_Class I = sought.getDeclaringClass();
    VM_Class C = VM_Magic.getObjectType(target).asClass(); 
    if (VM.BuildForITableInterfaceInvocation) {
      Object[] tib = C.getTypeInformationBlock();
      Object[] iTable = findITable(tib, I.getInterfaceId());
      return (INSTRUCTION[])iTable[getITableIndex(I, sought)];
    } else { 
      if (!VM_Runtime.isAssignableWith(I, C)) throw new IncompatibleClassChangeError();
      VM_Method found  = C.findVirtualMethod(sought.getName(), 
                                             sought.getDescriptor());
      if (found == null) throw new IncompatibleClassChangeError();
      return found.getCurrentInstructions();
    }
  }
  
  /**
   * Return a reference to the itable for a given class, interface pair
   * Under searched iTables, we might not have created the iTable yet,
   * in which case we will do that and then return it.
   * 
   * @param tib the TIB for the class
   * @param id interface id of the interface sought (NOT dictionary id!!)
   * @return iTable for desired interface
   */
  public static Object[] findITable(Object[] tib, int id) 
    throws IncompatibleClassChangeError, VM_ResolutionException {
    Object[] iTables = 
      (Object[])tib[TIB_ITABLES_TIB_INDEX];
    if (VM.DirectlyIndexedITables) {
      // ITable is at fixed offset
      return (Object[])iTables[id];
    } else {
      // Search for the right ITable
      VM_Type I = VM_Class.getInterface(id);
      if (iTables != null) {
	// check the cache at slot 0
	Object[] iTable = (Object[])iTables[0];
	if (iTable[0] == I) { 
	  return iTable; // cache hit :)
	}
	  
	// cache miss :(
	// Have to search the 'real' entries for the iTable
	for (int i=1; i<iTables.length; i++) {
	  iTable = (Object[])iTables[i];
	  if (iTable[0] == I) { 
	    // found it; update cache
	    iTables[0] = iTable;
	    return iTable;
	  }
        }
      }

      // Didn't find the itable, so we don't yet know if 
      // the class implements the interface. :((( 
      // Therefore, we need to establish that and then 
      // look for the iTable again.
      VM_Class C = (VM_Class)tib[0];
      if (!VM_Runtime.isAssignableWith(I, C)) throw new IncompatibleClassChangeError();
      synchronized (C) {
	installITable(C, (VM_Class)I);
      }
      Object[] iTable = findITable(tib, id);
      if (VM.VerifyAssertions) VM._assert(iTable != null);
      return iTable;
    }
  }

  
  /**
   * LHSclass is an interface that RHS class must implement.
   * Raises a VM_ResolutionException if RHStib does not implement LHSclass
   * 
   * @param LHSclass an class (should be an interface)
   * @param RHStib the TIB of an object that must implement LHSclass
   */
  public static void invokeinterfaceImplementsTest (VM_Class LHSclass, Object[] RHStib) 
    throws VM_ResolutionException, IncompatibleClassChangeError {
    if (!LHSclass.isResolved()) {
      LHSclass.load();
      LHSclass.resolve();
    }
    if (LHSclass.isInterface() && VM_DynamicTypeCheck.instanceOfInterface(LHSclass, RHStib)) return;
    // Raise an IncompatibleClassChangeError.
    throw new IncompatibleClassChangeError();
  }


  /**
   * mid is the dictionary id of an interface method we are trying to invoke
   * RHStib is the TIB of an object on which we are attempting to invoke it
   * We were unable to tell at compile time if mid is a real or ghost reference,
   * Therefore we must resolve it now and then call mandatoryInstanceOfInterface
   * with the right LHSclass.
   * 
   * @param mid the dictionary id of the target interface method
   * @param RHStib, the TIB of the object on which we are attempting to 
   * invoke the interface method
   */
  public static void unresolvedInvokeinterfaceImplementsTest (int mid, Object[] RHStib) 
    throws VM_ResolutionException, IncompatibleClassChangeError {
    VM_Method m = VM_MethodDictionary.getValue(mid);
    VM_Method nm = m.resolveInterfaceMethod(true);
    invokeinterfaceImplementsTest(nm.getDeclaringClass(), RHStib);
  }



  /*
   * PART II: Code to initialize the interface dispatching data structures.
   *          Called during the instantiate step of class loading.
   *          Preconditions: 
   *            (1) the caller has the lock on the VM_Class object
   *                whose data structures and being initialized.
   *            (2) the VMT for the class contains valid code.
   */
  

  /**
   * Main entrypoint called from VM_Class.instantiate to
   * initialize the interface dispatching data structues for 
   * the given class.
   *
   * @param klass the VM_Class to initialize the disaptch structures for.
   */
  public static void initializeDispatchStructures(VM_Class klass) {
    // if klass is abstract, we'll never use the dispatching structures.
    if (klass.isAbstract()) return; 
    VM_Class[] interfaces = collectInterfaces(klass);
    if (interfaces.length != 0) {
      if (VM.BuildForIMTInterfaceInvocation) {
	IMTDict d = buildIMTDict(klass, interfaces);
	if (VM.BuildForEmbeddedIMT) {
	  populateEmbeddedIMT(klass, d);
	} else {
	  populateIndirectIMT(klass, d);
	}
      } else if (VM.DirectlyIndexedITables) {
	populateITables(klass, interfaces);
      }
    }
  }
  
  /**
   * Build up the list of interfaces that a class implements.
   * 
   * @param klass the VM_Class to find the interfaces for.
   */
  private static VM_Class[] collectInterfaces(VM_Class klass) {
    int count = 0;
    int [] doesImplement = klass.getDoesImplement();
    for (int i=0; i<doesImplement.length; i++) {
      for (int mask = doesImplement[i]; mask != 0; mask = mask >>> 1) {
	if ((mask & 0x1) != 0) count++;
      }
    }
    VM_Class[] ans = new VM_Class[count];
    for (int i =0, idx = 0; i<doesImplement.length; i++) {
      int mask = doesImplement[i];
      if (mask != 0) {
	for (int j=0; j<32; j++) {
	  if ((mask & (1<<j)) != 0) {
	    int id = 32 * i + j;
	    ans[idx++] = VM_Class.getInterface(id);
	  }
	}
      }
    }
    
    return ans;
  }
  
  /**
   * Build up a description of the IMT contents for the given class.
   * NOTE: this structure is only used during class loading, so
   *       we don't have to worry about making it space efficient.
   * 
   * @param klass the VM_Class whose IMT we are going to build.
   * @return an IMTDict that describes the IMT we need to build for the class.
   */
  private static IMTDict buildIMTDict(VM_Class klass, VM_Class[] interfaces) {
    IMTDict d = new IMTDict(klass);
    for (int i=0; i<interfaces.length; i++) {
      VM_Method[] interfaceMethods = interfaces[i].getDeclaredMethods();
      for (int j=0; j<interfaceMethods.length; j++) {
	VM_Method im = interfaceMethods[j];
	if (im.isClassInitializer()) continue; 
	if (VM.VerifyAssertions) VM._assert(im.isPublic() && im.isAbstract()); 
	int id = VM_ClassLoader.findOrCreateInterfaceMethodSignatureId(im.getName(), im.getDescriptor());
	VM_Method vm = klass.findVirtualMethod(im.getName(), im.getDescriptor());
	// NOTE: if there is some error condition, then we are playing a dirty trick and
	//       pretending that a static method of VM_Runtime is a virtual method.
	//       Since the methods in question take no arguments, we can get away with this.
	if (vm == null || vm.isAbstract()) {
	  vm = VM_Entrypoints.raiseAbstractMethodError;
	} else if (!vm.isPublic()) {
	  vm = VM_Entrypoints.raiseIllegalAccessError;
	}
	d.addElement(id, vm);
      }
    }
    return d;
  }

  /**
   * Populate an embedded IMT for C using the IMTDict d
   */
  private static void populateEmbeddedIMT(VM_Class klass, IMTDict d) {
    Object[] tib = klass.getTypeInformationBlock();
    d.populateIMT(tib, tib);
  }

  /**
   * Populate an indirect IMT for C using the IMTDict d
   */
  private static void populateIndirectIMT(VM_Class klass, IMTDict d) {
    Object[] tib = klass.getTypeInformationBlock();
    INSTRUCTION[][] IMT = new INSTRUCTION[IMT_METHOD_SLOTS][];
    d.populateIMT(tib, IMT);
    tib[TIB_IMT_TIB_INDEX] = IMT;
  }


  /**
   * Populate the ITables array for DirectlyIndexedITables
   */
  private static void populateITables(VM_Class klass, VM_Class[] interfaces) {
    int maxId = 0;
    for (int i=0; i<interfaces.length; i++) {
      int cur = interfaces[i].getInterfaceId();
      if (cur > maxId) maxId = cur;
    }
    Object[][] iTables = new Object[maxId+1][];
    for (int i=0; i<interfaces.length; i++) {
      VM_Class interf = interfaces[i];
      iTables[interf.getInterfaceId()] = buildITable(klass, interf);
    }
    Object[] tib = klass.getTypeInformationBlock();
    tib[TIB_ITABLES_TIB_INDEX] = iTables;
  }    


  /**
   * Build and install an iTable for the given class interface pair
   * (used for iTable miss on searched iTables).
   */
  private static void installITable(VM_Class C, VM_Class I) {
    Object[] tib = C.getTypeInformationBlock();
    Object[] iTables = (Object[])tib[TIB_ITABLES_TIB_INDEX];

    if (iTables == null) {
      iTables = new Object[2];
      tib[TIB_ITABLES_TIB_INDEX] = iTables;
    } else {
      for (int i=0; i<iTables.length; i++) {
	if (((Object[])iTables[i])[0] == I) {
	  return; // some other thread just built the iTable
	}
      }
      Object[] tmp = new Object[iTables.length+1];
      System.arraycopy(iTables, 0, tmp, 0, iTables.length);
      iTables = tmp;
      tib[TIB_ITABLES_TIB_INDEX] = iTables;
    }
    if (VM.VerifyAssertions) VM._assert(iTables[iTables.length-1] == null);
    Object[] iTable = buildITable(C, I);
    iTables[iTables.length - 1] = iTable;
    // iTables[0] is a move to front cache; fill it here so we can
    // assume it always contains some iTable.
    iTables[0] = iTable;
  }


  /**
   * Build a single ITable for the pair of class C and interface I
   */
  private static Object[] buildITable(VM_Class C, VM_Class I) {
    VM_Method [] interfaceMethods = I.getDeclaredMethods();
    Object[] tib = C.getTypeInformationBlock();
    Object[] iTable = new Object[interfaceMethods.length+1];
    iTable[0] = I; 
    for (int i=0; i<interfaceMethods.length; i++) {
      VM_Method im = interfaceMethods[i];
      if (im.isClassInitializer()) continue; 
      if (VM.VerifyAssertions) VM._assert(im.isPublic() && im.isAbstract()); 
      VM_Method vm = C.findVirtualMethod(im.getName(), im.getDescriptor());
      // NOTE: if there is some error condition, then we are playing a dirty trick and
      //       pretending that a static method of VM_Runtime is a virtual method.
      //       Since the methods in question take no arguments, we can get away with this.
      if (vm == null || vm.isAbstract()) {
	vm = VM_Entrypoints.raiseAbstractMethodError;
      } else if (!vm.isPublic()) {
	vm = VM_Entrypoints.raiseIllegalAccessError;
      }
      if (vm.isStatic()) {
	vm.compile();
	iTable[getITableIndex(I, im)] = vm.getCurrentInstructions();
      } else {
	iTable[getITableIndex(I, im)] = (INSTRUCTION []) tib[vm.getOffset()>>2];
      }
    }
    return iTable;
  }


  /*
   * PART III: Supporting low-level code for manipulating IMTs and ITables
   */

  /**
   * If using embedded IMTs, Get offset of interface method slot in TIB.
   * If using indirect IMTs, Get offset of interface method slot in IMT.
   * Note that all methods with same name & descriptor map to the same slot.
   * <p>
   * TODO!! replace the simplistic hash algorithm with something more reasonable.
   * 
   * @param id an interface method signature if obtained from 
   *           VM_ClassLoader.findOrCreateInterfaceMethodSignatureId
   * @return offset in TIB/IMT
   */ 
  public static int getIMTOffset(int id) {
    if (VM.VerifyAssertions) VM._assert(VM.BuildForIMTInterfaceInvocation);
    int offset = VM_InterfaceMethodSignatureDictionary.getValue(id);
    if (offset == UNRESOLVED_INTERFACE_METHOD_OFFSET) {
      // we haven't assigned this signature an offset yet; do it now.
      int slot = id % IMT_METHOD_SLOTS;
      if (VM.BuildForEmbeddedIMT) {
	slot += TIB_FIRST_INTERFACE_METHOD_INDEX;
      }
      offset = slot << 2;
      VM_InterfaceMethodSignatureDictionary.setValue(id, offset);
    }
    return offset;
  }


  /**
   * Return the index of the interface method m in the itable
   */
  public static int getITableIndex(VM_Class klass, VM_Method m) {
    if (VM.VerifyAssertions) VM._assert(VM.BuildForITableInterfaceInvocation);
    if (VM.VerifyAssertions) VM._assert(klass.isLoaded() && klass.isInterface());
    VM_Method[] methods = klass.getDeclaredMethods();
    for (int i=0; i<methods.length; i++) {
      if (methods[i] == m) return i+1;
    }
    return -1;
  }


  /**
   * If there is an an IMT or ITable entry that contains 
   * compiled code for the argument method, then update it to
   * contain the current compiled code for the method.
   * 
   * @param klass the VM_Class who's IMT/ITable is being reset
   * @param m the method that needs to be updated.
   */
  public static void updateTIBEntry(VM_Class klass, VM_Method m) {
    Object[] tib = klass.getTypeInformationBlock();
    if (VM.BuildForIMTInterfaceInvocation) {
      VM_Method[] map = klass.noIMTConflictMap;
      if (map != null) {
	for (int i = 0; i<IMT_METHOD_SLOTS; i++) {
	  if (map[i] == m) {
	    if (VM.BuildForIndirectIMT) {
	      INSTRUCTION[][] IMT = (INSTRUCTION[][])tib[TIB_IMT_TIB_INDEX];
              IMT[i] = m.getCurrentInstructions();
            } else {
	      tib[i+TIB_FIRST_INTERFACE_METHOD_INDEX] = m.getCurrentInstructions();
	    }
	    return; // all done -- a method is in at most 1 IMT slot
	  }
	}
      }
    } else if (VM.BuildForITableInterfaceInvocation) {
      if (tib[TIB_ITABLES_TIB_INDEX] != null) {
        Object[] iTables = (Object[])tib[TIB_ITABLES_TIB_INDEX];
        for (int i=0; i<iTables.length; i++) {
          Object[] iTable = (Object[])iTables[i];
          if (iTable != null) {
            VM_Class I = (VM_Class)iTable[0];
            VM_Method [] interfaceMethods = I.getDeclaredMethods();
            for (int j=0; j<interfaceMethods.length; j++) {
              VM_Method im = interfaceMethods[j];
              if (im.getName() == m.getName() && 
                  im.getDescriptor() == m.getDescriptor()) {
                iTable[getITableIndex(I, im)] = m.getCurrentInstructions();
              }
            }
          }
        }
      }
    }
  }

  /*
   * Helper class used for IMT construction
   */
  private static final class IMTDict {
    private VM_Class klass;
    private Link[] links;

    IMTDict(VM_Class c) {
      klass = c;
      links = new Link[IMT_METHOD_SLOTS];
    }
    
    // Convert from the internally visible IMTOffset to an index
    // into my internal data structure.
    private int getIndex(int id) {
      int idx = getIMTOffset(id) >> 2;
      if (VM.BuildForEmbeddedIMT) {
	idx -= TIB_FIRST_INTERFACE_METHOD_INDEX;
      }
      return idx;
    }

    // count the number of signatures in the given IMT slot
    private int populationCount (int index) {
      Link p = links[index];
      int count = 0;
      while (p != null) {
	count++;
	p = p.next;
      }
      return count;
    }

    private VM_Method getSoleTarget(int index) {
      if (VM.VerifyAssertions) VM._assert(populationCount(index) == 1);
      return links[index].method;
    }

    // Add an element to the IMT dictionary (does nothing if already there)
    public void addElement(int id, VM_Method m) {
      int index = getIndex(id);
      Link p = links[index];
      if (p == null || p.signatureId > id) {
	links[index] = new Link(id, m, p);
      } else {
	Link q = p;
	while (p != null && p.signatureId <= id) {
	  if (p.signatureId == id) return; // already there so nothing to do.
	  q = p;
	  p = p.next;
	}
	q.next = new Link(id, m, p);
      }
    }

    // populate the
    public void populateIMT(Object[] tib, Object[] IMT) {
      int adjust = VM.BuildForEmbeddedIMT ? TIB_FIRST_INTERFACE_METHOD_INDEX : 0;
      for (int slot = 0; slot<links.length; slot++) {
	int extSlot = slot + adjust;
	int count = populationCount(slot);
	if (count == 0) {
	  VM_Entrypoints.raiseAbstractMethodError.compile();
	  IMT[extSlot] = VM_Entrypoints.raiseAbstractMethodError.getCurrentInstructions();
	} else if (count == 1) {
	  VM_Method target = getSoleTarget(slot);
	  if (target.isStatic()) {
	    target.compile();
	    IMT[extSlot] = target.getCurrentInstructions();
	  } else {
	    IMT[extSlot] = (INSTRUCTION []) tib[target.getOffset()>>2];
	    if (klass.noIMTConflictMap == null) {
	      klass.noIMTConflictMap = new VM_Method[IMT_METHOD_SLOTS];
	    }
	    klass.noIMTConflictMap[slot] = target;
	  }
	} else {
	  VM_Method[] targets = new VM_Method[count];
	  int[] sigIds = new int[count];
	  int idx = 0;
	  for (Link p = links[slot]; p != null; idx++, p = p.next) {
	    targets[idx] = p.method;
	    sigIds[idx] = p.signatureId;
	  }
	  IMT[extSlot] = VM_InterfaceMethodConflictResolver.createStub(sigIds, targets);
	}
      }
    }

    private static class Link {
      int       signatureId;
      VM_Method method;
      Link      next;
      Link (int sId, VM_Method m, Link n) {
	signatureId = sId;
	method      = m;
	next        = n;
      }
    }
  }
}

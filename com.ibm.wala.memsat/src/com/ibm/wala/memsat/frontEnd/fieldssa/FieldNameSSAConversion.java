/******************************************************************************
 * Copyright (c) 2009 - 2015 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.memsat.frontEnd.fieldssa;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.loader.*;
import com.ibm.wala.cast.java.ipa.callgraph.AstJavaSSAPropagationCallGraphBuilder.EnclosingObjectReferenceKey;
import com.ibm.wala.cast.java.ssa.*;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.memsat.frontEnd.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.intset.*;
import com.ibm.wala.shrikeBT.IInstruction;

import java.util.*;

public class FieldNameSSAConversion extends AbstractSSAConversion {
  private static final Object USES = new Object();
  private static final Object DEFS = new Object();
    
  public static class LexicalPointerKey implements PointerKey {
	  private final String varName;
	  private final String varDefiner;
	  
	  private LexicalPointerKey(String varName, String varDefiner) {
		this.varName = varName;
		this.varDefiner = varDefiner;
	  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((varDefiner == null) ? 0 : varDefiner.hashCode());
		result = prime * result + ((varName == null) ? 0 : varName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LexicalPointerKey other = (LexicalPointerKey) obj;
		if (varDefiner == null) {
			if (other.varDefiner != null)
				return false;
		} else if (!varDefiner.equals(other.varDefiner))
			return false;
		if (varName == null) {
			if (other.varName != null)
				return false;
		} else if (!varName.equals(other.varName))
			return false;
		return true;
	}
  }
  
  public static class LocalFieldAccesses implements FieldAccesses {

    private final boolean heapWritesAsUpdates;

    private final IClassHierarchy cha;

    private final HeapModel heapModel;

    private final PointerAnalysis pointerAnalysis;

    private final CGNode node;

    public LocalFieldAccesses(boolean heapWritesAsUpdates, 
			      CGNode node,
			      PointerAnalysis pointerAnalysis,
			      IClassHierarchy cha)
    {
      this.cha = cha;
      this.node = node;
      this.pointerAnalysis = pointerAnalysis;
      this.heapWritesAsUpdates = heapWritesAsUpdates;

      this.heapModel = pointerAnalysis.getHeapModel();
    }

    private boolean isNewArray(SSAInstruction inst) {
      if (inst instanceof SSANewInstruction) {
	SSANewInstruction ni = (SSANewInstruction) inst;
	if (ni.getConcreteType().isArrayType()) {
	  return true;
	}
      }

      return false;
    }

    private PointerKey[] resolve(int valueNumber, FieldReference ref) {
      IField f = cha.resolveField(ref);

      if (f.isStatic()) {
	return new PointerKey[]{
	  heapModel.getPointerKeyForStaticField(f)
	};
	  
      } else {
	OrdinalSet objs =
	  pointerAnalysis.getPointsToSet(
	    heapModel.getPointerKeyForLocal(node, valueNumber));
	
	PointerKey[] result = 
	  new PointerKey[ objs.size() ];

	int i = 0;
	for(Iterator iks = objs.iterator(); iks.hasNext(); ) {
	  result[i++] =
	    heapModel
	      .getPointerKeyForInstanceField((InstanceKey)iks.next(), f);
	}
	
	return result;
      }
    }

    private PointerKey[] resolveLength(int valueNumber) {
      OrdinalSet objs =
	pointerAnalysis.getPointsToSet(
	  heapModel.getPointerKeyForLocal(node, valueNumber));
	
      PointerKey[] result = new PointerKey[ objs.size() ];
      int i = 0;
      for(Iterator iks = objs.iterator(); iks.hasNext(); ) {
    	  result[i++] = new ArrayLengthKey((InstanceKey)iks.next());
      }

      return result;
    }

    private PointerKey[] resolveState(int valueNumber) {
      OrdinalSet objs =
	pointerAnalysis.getPointsToSet(
	  heapModel.getPointerKeyForLocal(node, valueNumber));
	
      PointerKey[] result = new PointerKey[ objs.size() ];
      int i = 0;
      for(Iterator iks = objs.iterator(); iks.hasNext(); ) {
	  result[i++] = (PointerKey) heapModel.getPointerKeyForArrayContents((InstanceKey)iks.next());
      }

      return result;
    }

    private PointerKey[] resolveEnclosing(int valueNumber, IClass cls) {
      OrdinalSet objs =
	pointerAnalysis.getPointsToSet(
	  heapModel.getPointerKeyForLocal(node, valueNumber));
	
      PointerKey[] result = new PointerKey[ objs.size() ];
      int i = 0;
      for(Iterator iks = objs.iterator(); iks.hasNext(); ) {
	result[i++] =
	  new EnclosingObjectReferenceKey((InstanceKey)iks.next(), cls);
      }

      return result;
    }

    private PointerKey[] resolveLexical(Access[] accesses) {
    	int i = 0;
    	PointerKey[] keys = new PointerKey[ accesses.length ];
    	for(Access a : accesses) {
    		keys[i++] = new LexicalPointerKey(a.variableName, a.variableDefiner);
    	}
    	return keys;
    }
    
    private PointerKey[] combine(PointerKey[] a, PointerKey[] b) {
    	PointerKey[] result = new PointerKey[ a.length+b.length ];
    	System.arraycopy(a, 0, result, 0, a.length);
    	System.arraycopy(b, 0, result, a.length, b.length);
    	return result;
    }
    
    public PointerKey[] getUses(SSAInstruction inst) {
    	if (inst instanceof SSAGetInstruction) {
    		SSAGetInstruction i = (SSAGetInstruction) inst;
    		return resolve(i.getRef(), i.getDeclaredField());
    	} else if (inst instanceof SSAArrayLoadInstruction) {
    		return combine(resolveState(inst.getUse(0)), resolveLength( inst.getUse(0) ));
    	} else if (inst instanceof SSAArrayStoreInstruction) {
    		if (heapWritesAsUpdates) {
    			return combine(resolveState(inst.getUse(0)), resolveLength( inst.getUse(0) ));
    		} else {
    			return resolveLength( inst.getUse(0) );   		
    		}
    	} else if (inst instanceof SSAArrayLengthInstruction) {
    		return resolveLength( inst.getUse(0) );
    	} else if (inst instanceof AstIsDefinedInstruction) {
    		AstIsDefinedInstruction i = (AstIsDefinedInstruction)inst;
    		if (i.getFieldRef() != null) {
    			return resolve(i.getUse(0), i.getFieldRef());
    		} else {
    			throw new AssertionError("unreachable");
    		}
    	} else if (inst instanceof EnclosingObjectReference) {
    		EnclosingObjectReference i = (EnclosingObjectReference)inst;
    		return 
    		resolveEnclosing(/* i.getUse(0) */ 1, cha.lookupClass(i.getEnclosingType()));
    	} else if (inst instanceof AstLexicalRead) {
    		return resolveLexical(((AstLexicalRead)inst).getAccesses());
    	} else if (heapWritesAsUpdates) {
    		return getDefs(inst);
    	} else {
    		return new PointerKey[0];
    	}
    }

    public PointerKey[] getDefs(SSAInstruction inst) {
    	if (inst instanceof SSAPutInstruction) {
    		SSAPutInstruction i = (SSAPutInstruction) inst;
    		return resolve(i.getRef(), i.getDeclaredField());
    	} else if (inst instanceof SSAArrayStoreInstruction) {
    		return resolveState(inst.getUse(0));
    	} else if (inst instanceof SSANewInstruction) {
    		SSANewInstruction i = (SSANewInstruction)inst;
    		IClass klass = cha.lookupClass(i.getConcreteType());
    		if (isNewArray(inst)) {
    			return resolveLength(inst.getDef(0));
    		} else if (klass instanceof JavaClass) {
    			List x = new ArrayList();
     			IClass enclosingClass = ((JavaClass) klass).getEnclosingClass();
    			while (enclosingClass != null) {

    				PointerKey[] y =
     					resolveEnclosing(inst.getDef(0), enclosingClass);
    				for(int j = 0; j < y.length; j++) {
    					x.add(y[j]);
    				}

    				enclosingClass = ((JavaClass) enclosingClass).getEnclosingClass();
    			}

    			return (PointerKey[])x.toArray(new PointerKey[x.size()]);
    		} else {
    			return new PointerKey[0];
    		}
    	} else if (inst instanceof AstLexicalWrite) {
    		return resolveLexical(((AstLexicalWrite)inst).getAccesses());
    	} else {
    		return new PointerKey[0];
    	}
    }      
  }

  public static class IPFieldAccesses extends LocalFieldAccesses {

    private final FieldAccesses invokeAccesses;

    public IPFieldAccesses(boolean heapWritesAsUpdates,
			   CGNode node,
			   PointerAnalysis pointerAnalysis,
			   IClassHierarchy cha,
			   FieldAccesses invokeAccesses) {
      super(heapWritesAsUpdates, node, pointerAnalysis, cha);
      this.invokeAccesses = invokeAccesses;
    }

    public PointerKey[] getUses(SSAInstruction inst) {
      if (inst instanceof SSAAbstractInvokeInstruction) {
	return invokeAccesses.getUses(inst);
      } else {
	return super.getUses(inst);
      }
    }

    public PointerKey[] getDefs(SSAInstruction inst) {
      if (inst instanceof SSAAbstractInvokeInstruction) {
	return invokeAccesses.getDefs(inst);
      } else {
	return super.getDefs(inst);
      }
    }
  }

  public FieldNameSSAConversion(IR ir, FieldAccesses fieldAccesses) {
    super(ir, new SSAOptions());
    this.ir = ir;
    this.fieldAccesses = fieldAccesses;
    fieldPhiSet = new LinkedHashSet();
    fieldPhiNodes = new LinkedHashMap();
    initialFieldNumbers = makeInitialFieldNumbers(ir);
    valueNumbers = makeValueNumbers(ir);
    nextFreeNumber = getMaxValueNumber() + 1;
  }

  private final IR ir;

  private final FieldAccesses fieldAccesses;


  private final ObjectArrayMapping initialFieldNumbers;
  private final Set fieldPhiSet;
  private final Map fieldPhiNodes;
  private final Map valueNumbers;

  private int[] exitLives;
  private int nextFreeNumber;

  private int getInitialFieldNumber(PointerKey fr) {
    assert initialFieldNumbers.getMappedIndex(fr) >= 0 : "no mapping for " + fr + " in " + ir.getMethod();
    return initialFieldNumbers.getMappedIndex(fr) + 1;
  }

  private ObjectArrayMapping makeInitialFieldNumbers(IR ir) {
    Set fields = new LinkedHashSet();
    for(Iterator is = iterateInstructions(ir); is.hasNext(); ) {
      SSAInstruction i = (SSAInstruction)is.next();
      if (i == null) continue;

      PointerKey[] defs = fieldAccesses.getDefs(i);
      for(int f = 0; f < defs.length; f++) {
	fields.add( defs[f] );
      }

      PointerKey[] uses = fieldAccesses.getUses(i);
      for(int f = 0; f < uses.length; f++) {
	fields.add( uses[f] );
      }
    }

    return new ObjectArrayMapping(
      fields.toArray(new PointerKey[ fields.size() ])
    );
  }
    
  private Map makeValueNumbers(IR ir) {
    Map vns = new LinkedHashMap();
    for(Iterator is = iterateInstructions(ir); is.hasNext(); ) {
      SSAInstruction inst = (SSAInstruction)is.next();
      if (inst == null) continue;

      PointerKey[] uses = fieldAccesses.getUses(inst);
      int[] useValueNumbers = new int[ uses.length ];
      for(int j = 0; j < uses.length; j++) {
	useValueNumbers[j] = getInitialFieldNumber( uses[j] );
      }
      vns.put(Pair.make(inst, USES), useValueNumbers);
      
      PointerKey[] defs = fieldAccesses.getDefs(inst);
      int[] defValueNumbers = new int[ defs.length ];
      for(int j = 0; j < defs.length; j++) {
	defValueNumbers[j] = getInitialFieldNumber( defs[j] );
      }
      vns.put(Pair.make(inst, DEFS), defValueNumbers);
    }
    
    return vns;
  }

  protected int getNumberOfDefs(SSAInstruction inst) {
    if (fieldPhiSet.contains(inst)) {
      return inst.getNumberOfDefs();
    } else {
    	int[] defs = (int[])valueNumbers.get(Pair.make(inst, DEFS));
    	if ( defs == null) {
    		return 0;
    	} else { 
    		return defs.length;
    	}
    }
  }

  protected int getDef(SSAInstruction inst, int index) {
    if (fieldPhiSet.contains(inst)) {
      return inst.getDef(index);
    } else {
      return ((int[])valueNumbers.get(Pair.make(inst, DEFS)))[index];
    }
  }

  protected int getNumberOfUses(SSAInstruction inst) {
    if (fieldPhiSet.contains(inst)) {
      return inst.getNumberOfUses();
    } else {
      return ((int[])valueNumbers.get(Pair.make(inst, USES))).length;
    }
  }

  protected int getUse(SSAInstruction inst, int index) {
    if (fieldPhiSet.contains(inst)) {
      return inst.getUse(index);
    } else {
      return ((int[])valueNumbers.get(Pair.make(inst, USES)))[index];
    }
  }


  protected int getMaxValueNumber() {
    return initialFieldNumbers.getSize();
  }

  protected int getNextNewValueNumber() {
    return nextFreeNumber++;
  }

  protected boolean isLive(SSACFG.BasicBlock Y, int V) {
    return true;
  }

  protected boolean skip(int vn) {
    return false;
  }

  protected boolean isConstant(int valueNumber) {
    return false;
  }

  protected boolean isAssignInstruction(SSAInstruction inst) {
    return false;
  }

  protected void initializeVariables() {
    for (int i = 1; i <= getMaxValueNumber(); i++) {
      S[i].push( i );
      valueMap[ i ] = i;
    }
  }

  protected void repairExit() {
    exitLives = new int[ getMaxValueNumber() ];
    for(int i = 1; i <= getMaxValueNumber(); i++) {
      assert ! S[i].isEmpty();
      exitLives[i-1] = top(i);
    }
  }


  protected void placeNewPhiAt(int value, SSACFG.BasicBlock Y) {
    int[] params = new int[CFG.getPredNodeCount(Y)];
    for (int i = 0; i < params.length; i++)
      params[i] = value;

    SSAPhiInstruction phi = new SSAPhiInstruction(Y.getGraphNodeId(), value, params);
    fieldPhiSet.add(phi);

    SSAPhiInstruction[] phis = (SSAPhiInstruction[])fieldPhiNodes.get(Y);
    if (phis == null) {
      phis = new SSAPhiInstruction[1];
      phis[0] = phi;
      fieldPhiNodes.put(Y, phis);
    } else {
      SSAPhiInstruction[] newPhis = new SSAPhiInstruction[ phis.length+1 ];
      System.arraycopy(phis, 0, newPhis, 1, phis.length);
      newPhis[0] = phi;
      fieldPhiNodes.put(Y, newPhis);
    }
  }

  protected SSAPhiInstruction getPhi(SSACFG.BasicBlock B, int index) {
    return ((SSAPhiInstruction[])fieldPhiNodes.get(B))[index];
  }

  protected void setPhi(SSACFG.BasicBlock B, int index, SSAPhiInstruction inst) {
    ((SSAPhiInstruction[])fieldPhiNodes.get(B))[index] = inst;
  }

  protected SSAPhiInstruction repairPhiDefs(SSAPhiInstruction phi, int[] newDefs) {
    SSAPhiInstruction np = (SSAPhiInstruction)phi.copyForSSA(ir.getMethod().getDeclaringClass().getClassLoader().getInstructionFactory(), newDefs, null);
    fieldPhiSet.remove(phi);
    fieldPhiSet.add(np);
    return np;
  }

  protected void repairPhiUse(SSACFG.BasicBlock BB, int phiIndex, int rvalIndex, int newRval) {
    SSAPhiInstruction phi = getPhi(BB, phiIndex);

    int[] newUses = new int[getNumberOfUses(phi)];
    for (int v = 0; v < newUses.length; v++) {
      int oldUse = getUse(phi, v);
      int newUse = (v==rvalIndex)? newRval: oldUse;
      newUses[v] = newUse;
    }

    phi.setValues(newUses);
  }

  protected void repairInstructionUses(SSAInstruction inst, int index, int[] newUses) {

  }

  protected void repairInstructionDefs(SSAInstruction inst, int index, int[] newDefs, int[] newUses) {
    valueNumbers.put(Pair.make(inst, DEFS), newDefs);
    valueNumbers.put(Pair.make(inst, USES), newUses);
    
   for(int i = 0; i < newDefs.length; i++){
    	for(int j = 0; j < newUses.length; j++){
    		assert newDefs[i] != newUses[j];
    	}
    }
  }
    
  protected void pushAssignment(SSAInstruction inst, int index, int newRhs) {
    Assertions.UNREACHABLE();
  }

  protected void popAssignment(SSAInstruction inst, int index) {
    Assertions.UNREACHABLE();
  }

  public FieldSSATable convert() {
    perform();
    return new FieldSSATable() {

      public Iterator<PointerKey> getFields() {
        return initialFieldNumbers.iterator();
      }

      public boolean isHeapPhi(SSAPhiInstruction inst) {
	return fieldPhiSet.contains(inst);
      }
	    
      public int getMaxHeapNumber() {
	return nextFreeNumber-1;
      }
    
      public int getMaxInitialHeapNumber() {
	return initialFieldNumbers.getSize();
      }
	
      public boolean isArrayNumber(int valueNumber) {
	return getField(valueNumber) instanceof ArrayContentsKey;
      }

      public PointerKey getField(int vn) {
	return (PointerKey)initialFieldNumbers.getMappedObject(valueMap[vn]-1);
      }

      public Iterator getPhiNodes(SSACFG.BasicBlock BB) {
	if (! fieldPhiNodes.containsKey( BB )) {
	  return EmptyIterator.instance();
	} else {
	  return
	    new ArrayIterator(((SSAPhiInstruction[])fieldPhiNodes.get(BB)));
	}
      }		

      public Iterator getPhiNodes() {
	return fieldPhiSet.iterator();
      }

      public int getEntryValue(PointerKey field) {
	return getInitialFieldNumber(field);
      }

      public int getExitValue(PointerKey field) {
    	  if (initialFieldNumbers.getMappedIndex(field) == -1)
    	  	return -1;
	return exitLives[ initialFieldNumbers.getMappedIndex(field) ];
      }

      public int[] getUses(SSAInstruction inst) {
	  return (int[]) valueNumbers.get(Pair.make(inst, USES));
      }

      public int[] getDefs(SSAInstruction inst) {
	  return (int[]) valueNumbers.get(Pair.make(inst, DEFS));
      }

      public int getUse(SSAInstruction inst, int i) {
	return getUses(inst)[i];
      }

      public int getDef(SSAInstruction inst, int i) {
	return getDefs(inst)[i];
      }
	
      public int getUse(SSAInstruction inst, PointerKey field) {
	for(int i = 0; i < getNumberOfUses(inst); i++) {
	  if (field.equals(getField(getUse(inst, i))))
	    return i;
	}
      
	Assertions.UNREACHABLE();
	return -1;
      }

      public int getDef(SSAInstruction inst, PointerKey field) {
	for(int i = 0; i < getNumberOfDefs(inst); i++) {
	  if (field.equals(getField(getDef(inst, i))))
	    return i;
	}
      
	Assertions.UNREACHABLE();
	return -1;
      }

      public DefUse getDefUse() {
	return new DefUse(ir) {
	  protected int getMaxValueNumber() {
	    return getMaxHeapNumber();
	  }

	  protected void initAllInstructions() {
	    for (SSAInstruction inst : FieldNameSSAConversion.this.getInstructions(ir)) {
	      allInstructions.add(inst);
	    }
	    allInstructions.addAll(fieldPhiSet);
	  }
	    
	  protected int getDef(SSAInstruction s, int i) {
	    return FieldNameSSAConversion.this.getDef(s, i);
	  }

	  protected int getUse(SSAInstruction s, int i) {
	    return FieldNameSSAConversion.this.getUse(s, i);
	  }

	  protected int getNumberOfDefs(SSAInstruction s) {
	    return FieldNameSSAConversion.this.getNumberOfDefs(s);
	  }

	  protected int getNumberOfUses(SSAInstruction s) {
	    return FieldNameSSAConversion.this.getNumberOfUses(s);
	  }
	};
      }
 
      public String toString() {
	StringBuffer B = new StringBuffer();
	B.append("Field SSA for ").append(CFG.getMethod()).append("\n");

	for(int i = 1; i < nextFreeNumber; i++) {
	  B.append("number ")
	      .append(i)
	      .append(" is ")
	      .append(getField(i))
	      .append(":")
	      .append(i)
	      .append(i <= getMaxInitialHeapNumber()? "(initial)": "(derived)")
	      .append("\n");
	}
	
	SSAInstruction[] insts = getInstructions(ir);
	for(Iterator BBs = CFG.iterator(); BBs.hasNext(); ) {

	  SSACFG.BasicBlock BB = (SSACFG.BasicBlock)BBs.next();
	  B.append("block ").append(BB).append("\n");

	  if (fieldPhiNodes.containsKey(BB)) {
	    SSAPhiInstruction[] phis = 
	      (SSAPhiInstruction[])fieldPhiNodes.get(BB);
	    for(int i = 0; i < phis.length; i++) { 
	      B.append("  phi ").append(phis[i]).append("\n");
	    }
	  }

	  for(int i = BB.getFirstInstructionIndex();
	      i <= BB.getLastInstructionIndex();
	      i++)
	  {
	    if (insts[i] != null) {
	      B.append("  ").append( insts[i] );
	      int[] uses = (int[])
		valueNumbers.get(Pair.make(insts[i], USES));
	      if (uses.length > 0) {
		B.append(" (uses: ");
		for(int j = 0; j < uses.length; j++) {
		  B.append(uses[j]).append(" ");
		}
		B.append(")");
	      }
	      int[] defs = (int[])
		valueNumbers.get(Pair.make(insts[i], DEFS));
	      if (defs.length > 0) {
		B.append(" (defs: ");
		for(int j = 0; j < defs.length; j++) {
		  B.append(defs[j]).append(" ");
		}
		B.append(")");
	      }
	    }
	    B.append("\n");
	  }
	}

	return B.toString();
      }
    };
  }
}

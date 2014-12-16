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
/**
 * 
 */
package com.ibm.wala.memsat.frontEnd;

import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;

/**
 * Symbol table for field SSA conversion of a given CGNode.
 * The table stores value numbers for field defs/uses in 
 * the CGNode's IR, as well as the phi nodes for merges of 
 * heap values.
 * 
 * @specfield cgNode: CGNode // underlying call graph node
 * @specfield fields: set PointerKey // SSA converted fields of this.cgNode
 * @specfield valueNumbers: set int // value numbers for all field defs
 * @specfield fieldValueNumbers: valueNumbers ->one fields
 * @specfield definers: valueNumbers lone->one SSAInstruction
 * @specfield phiNodes: set SSAPhiInstructions // synthetic phi nodes for heap merges
 * @specfield initialHeapValueNumbers: set int // ssa value numbers for this.fields in the initial heap
 * 
 * @invariant all n: valueNumbers | n > max(initialHeapValueNumbers)
 * 
 * @author Julian Dolby
 * @author Emina Torlak
 */
public interface FieldSSATable {
	/**
	 * Returns true if the given instruction is a synthetic
	 * phi instruction in this table.
	 * @return inst in this.phiNodes
	 */
	public abstract boolean isHeapPhi(SSAPhiInstruction inst);

	/**
	 * Returns an iterator over all instructions from this.phiNodes that 
	 * belong in the given basic block.
	 * @requires block is a basic block in this.CGNode.getIR() 
	 * @return an iterator over all instructions from this.phiNodes that 
	 * belong in the given basic block.
	 */
    public abstract Iterator<SSAPhiInstruction> getPhiNodes(SSACFG.BasicBlock block);

    /**
     * Returns an iterator over all synthetic phi nodes in this table.
     * @return an iterator over this.phiNodes
     */
    public abstract Iterator<SSAPhiInstruction> getPhiNodes();

    /**
     * Returns the maximum heap value number for this.cgNode.
     * @return max(this.valueNumbers)
     */
    public abstract int getMaxHeapNumber();
	
    /**
     * Returns the maximum heap value number in this.initialHeapValues.
     * @return max(this.initialHeapValues)
     */
    public abstract int getMaxInitialHeapNumber();
	
    /**
     * Returns true if the given heap value number corresponds
     * to a field of an array type.
     * @requires valueNumber in this.valueNumbers
     * @return  true if the given heap value number corresponds
     * to a field of an array type.
     */
    public abstract boolean isArrayNumber(int valueNumber);
    
    /**
     * Returns the field corresponding to the given value number.
     * @requires valueNumber in this.valueNumbers
     * @return the field corresponding to the given value number.
     */
    public abstract PointerKey getField(int valueNumber);

    /**
     * Returns the SSAInstruction that defines the given heap value number.
     * @requires valueNumber in this.valueNumbers
     * @return this.definers[valueNumber]
     */
    public abstract DefUse getDefUse();

    /**
     * Returns the heap value number that corresponds to the input value of 
     * the given field (i.e. the value number of the given field in the initial heap).
	 * @requires field in this.fields
     * @return the heap value number that corresponds to the input value of 
     * the given field 
     */
    public abstract int getEntryValue(PointerKey field);

    /**
     * Returns the heap value number that corresponds to the output value of 
     * the given field (i.e. the value number of the given field in the final heap).
	 * @requires field in this.fields
     * @return the heap value number that corresponds to the output value of 
     * the given field
     */
    public abstract int getExitValue(PointerKey field);
    
    /**
     * Returns an iterator over this.fields.
     * @return an iterator over this.fields.
     */
    public abstract Iterator<PointerKey> getFields();

    /**
     * Returns the heap value number of the field at the 
     * given <tt>use</tt> index in the specified instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @requires 0 <= use < inst.getNumberOfUses()
     * @return the heap value number of the field at the 
     * given <tt>use</tt> index in the specified instruction.
     */
    public abstract int getUse(SSAInstruction inst, int use);

    /**
     * Returns the heap use value number of the given field at the specified instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @return the heap use value number of the field at the specified instruction.
     */
    public abstract int getUse(SSAInstruction inst, PointerKey field);
    
    /**
     * Returns the heap def value number of the given field at the specified instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @return the heap def value number of the field at the specified instruction.
     */
    public abstract int getDef(SSAInstruction inst, PointerKey field);
    
    /**
     * Returns the heap value number of the field at the 
     * given <tt>def</tt> index in the specified instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @requires 0 <= use < inst.getNumberOfDefs()
     * @return the heap value number of the field at the 
     * given <tt>def</tt> index in the specified instruction.
     */
    public abstract int getDef(SSAInstruction inst, int def);

    /**
     * Returns all heap value numbers used by the given instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @return all heap value numbers used by the given instruction.
     */
    public abstract int[] getUses(SSAInstruction inst);

    /**
     * Returns all heap value numbers defined by the given instruction.
     * @requires inst in this.cgNode.getIR().getInstructions()[int]
     * @return all heap value numbers defined by the given instruction.
     */
    public abstract int[] getDefs(SSAInstruction inst);
}

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

import kodkod.util.ints.IndexedEntry;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * Stores the control flow, field ssa conversion, type data, etc., 
 * information for a single call graph node.
 * 
 * @specfield cgNode: CGNode
 * @specfield typeData: int ->one IRType
 * @specfield fieldSSA: FielSSATable
 * @specfield controlDep: DependenceGraph<SSACFG.BasicBlock>
 *
 * @author Emina Torlak
 */
public interface WalaCGNodeInformation {

	/**
	 * Returns this.cgNode.
	 * @return this.cgNode.
	 */
	public abstract CGNode cgNode();
	
	/**
	 * Returns the control dependence graph for this.cgNode.
	 * The returned graph <i>G</i> has the same set of nodes as this.cgNode.getIR().getControlFlowGraph().
	 * The edge labels for each edge <i>e</i> in <i>G</i> consist of the 
	 * edge labels for the control flow edges in this.cgNode.getIR().getControlFlowGraph() 
	 * that induce <i>e</i> in <i>G</i>.
	 * @return this.controlDep
	 */
	public abstract DependenceGraph<SSACFG.BasicBlock> controlDependences();
	
	/**
	 * Returns an iterator over the instructions in the loop-unrolled 
	 * IR of this.cgNode, ordered so that each use of a local variable
	 * or a field is preceded by its definition.  The returned instructions
	 * include both the instructions from this.cgNode.getIR().getInstructions() and
	 * the synthetic phi nodes from this.fieldSSATable().phiNodes.  As synthetic
	 * instructions have no indices in this.cgNode.IR, their corresponding indices
	 * will be -1.  (For all other instructions, the index field of an indexed
	 * entry returned by the iterator corresponds to the instruction's index in 
	 * this.cgNode.IR.)
	 * @return an iterator over the instructions in the loop-unrolled 
	 * IR of this.cgNode, ordered so that each use of a local variable
	 * or a field is preceded by its definition.
	 * @see #fieldSSA()
	 */
	public abstract Iterator<? extends IndexedEntry<SSAInstruction>> relevantInstructions();
		
	/**
	 * Returns the IRType for the given value number in this.CGNode.getIR().
	 * @requires 0 < valueNumber <= this.cgNode.getIR().getSymbolTable().getMaxValueNumber()
	 * @return this.typeData[valueNumber]
	 */
	public abstract IRType typeOf(int valueNumber);
	
	/**
	 * Returns the local pointer key for the value number in this.CGNode.getIR().
	 * @requires  0 < valueNumber <= this.cgNode.getIR().getSymbolTable().getMaxValueNumber
	 * @return the local pointer key for the given value number in this.CGNode.getIR().
	 */
	public abstract PointerKey pointerKeyFor(int valueNumber);
	
	/**
	 * Returns the FieldSSATable for this.cgNode.
	 * @return this.fieldSSATable
	 * @see FieldSSATable
	 */
	public abstract FieldSSATable fieldSSA();
	
	
}

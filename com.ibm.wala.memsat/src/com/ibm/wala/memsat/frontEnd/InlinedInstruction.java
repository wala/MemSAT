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

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

import kodkod.util.collections.Stack;

/**
 * Specifies the position that an {@linkplain SSAInstruction}
 * from a given {@linkplain CallGraph} would have if its containing graph
 * were flattened by inlining all calls. 
 * 
 * @specfield index: int
 * @specfield instruction: lone SSAInstruction 
 * @specfield cgNode: CGNode // call graph node that contains this.instruction
 * @specfield callStack: Stack<CallSite> // call stack for this.instruction
 * @specfield action: Action
 * @author Emina Torlak
 */
public interface InlinedInstruction {
	/**
	 * Returns the call graph node that contains this.instruction.
	 * @return this.cgNode
	 */
	public abstract CGNode cgNode();
	
	/**
	 * Returns the index of this.instruction in this.cgNode.getIR(). 
	 * If this.index is Integer.MIN_VALUE, then this InlinedInstruction
	 * is the synthetic "thread start" action for a thread executing
	 * this.cgNode.  If the index is Integer.MAX_VALUE, then this instruction
	 * is the synthetic "thread end" action for a thread executing this.cgNode.
	 * @return this.index
	 */
	public abstract int instructionIndex();
	
	/**
	 * Returns this.instruction.
	 * @return this.instruction
	 */
	public abstract SSAInstruction instruction();
	
	/**
	 * Returns the call stack that leads up to this.instruction.
	 * @return this.callStack.
	 */
	public abstract Stack<CallSite> callStack();
	
	/**
	 * Returns true if o is an inlined instruction with the
	 * same cg node, instruction index, and call stack.
	 * @return o in InlinedInstruction and o.cgNode.equals(this.cgNode)
	 * and o.instructionIndex = this.instructionIndex and 
	 * o.callStack.equals(this.callStack)
	 */
	public abstract boolean equals(Object o);
	
	/**
	 * Returns the hash code for this inlined instruction.
	 * @return cgNode.hashCode() + sum[ this.callStack().elems[int].hashCode() ] + this.instructionIndex()
	 */
	public abstract int hashCode();

	/**
	 * Returns the action of this inlined instruction on shared memory.
	 * @return this.action
	 */
	public abstract Action action();
	
	/**
	 * Enumerates the kind of actions that InlinedInstructions have on shared memory.
	 * @author etorlak
	 */
	public static enum Action {
		/** A normal read action is the result of an {@linkplain SSAArrayLoadInstruction array load} 
		 * or a {@linkplain SSAGetInstruction field get} from a non-volatile field. */
		NORMAL_READ 	{ public String toString() { return "NormalRead"; } }, 
		/** A volatile read action is the result of a {@linkplain SSAGetInstruction field get} from on a volatile field. */
		VOLATILE_READ 	{ public String toString() { return "VolatileRead"; } },
		/** A normal write action is the result of an {@linkplain SSAArrayStoreInstruction array store} 
		 * or a {@linkplain SSAPutInstruction field put} to a non-volatile field. */
		NORMAL_WRITE 	{ public String toString() { return "NormalWrite"; } }, 
		/** A volatile write action is the result of a {@linkplain SSAPutInstruction field put} to a volatile field. */
		VOLATILE_WRITE 	{ public String toString() { return "VolatileWrite"; } },
		/** A lock action is the result of an {@linkplain SSAMonitorInstruction} that acquires a monitor. */
		LOCK			{ public String toString() { return "Lock"; } }, 
		/** A lock action is the result of an {@linkplain SSAMonitorInstruction} that releases a monitor. */
		UNLOCK			{ public String toString() { return "Unlock"; } },
		/** A start action is a synthetic action indicating the start of a thread. */
		START			{ public String toString() { return "Start"; } }, 
		/** An end action is a synthetic action indicating the end of a thread. */
		END				{ public String toString() { return "End"; } }, 
		/** A special action is the result of an {@linkplain SSAAbstractInvokeInstruction invocation} 
		 * of a {@linkplain MemoryModel#externals() method that can affect memory ordering} */
		SPECIAL			{ public String toString() { return "Special"; } };
	
	}
}

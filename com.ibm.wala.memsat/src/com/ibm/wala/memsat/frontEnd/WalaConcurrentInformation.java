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

import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.graph.Graph;

/**
 * Encapsulates the information computed by {@linkplain WalaEngine}
 * about the concurrent behavior of a given thread with respect to 
 * one or more other threads.  A WalaConcurrentInformation includes 
 * the thread's memory actions, program order on the memory actions,
 * write visibility, etc.  
 * 
 * @specfield root: CGNode // the entry method
 * @specfield actions: set InlinedInstruction // relevant memory actions
 * 
 * @author Emina Torlak
 */
public interface WalaConcurrentInformation {
	
	/**
	 * Returns the CGNode for the entry method to the thread described by this concurrent info object.
	 * @return this.root
	 */
	public abstract CGNode root();
	
	/**
	 * Returns the set of memory actions performed by this thread
	 * that may affect the execution.  In particular, each returned action A
	 * has the following properties.  A.instruction is an SSAFieldAccessInstruction
	 * to a field that may be the target of a conflicting access by another thread
	 * (i.e. at least one of the access is a write); or A.instruction is an 
	 * SSAMonitorInstruction; or A.instruction is a call to a user-specified method,
	 * which represents a memory fence or another operation that has an effect on 
	 * memory ordering (but has no effect on the heap and no return values).  
	 * @return this.actions
	 */
	public abstract Set<InlinedInstruction> actions();
	
	/**
	 * Returns the (synthetic) start action of this.cgNode.
	 * @return start action of this.root.
	 */
	public abstract InlinedInstruction start();
	
	/**
	 * Returns the (synthetic) end action of this.cgNode.
	 * @return end action of this.root.
	 */
	public abstract InlinedInstruction end();
	
	/**
	 * Returns a graph representation of the upper bound on the execution order of this thread. 
	 * Let tCFG be the interprocedural CFG for this thread.  The returned graph contains an edge
	 * A1->A2, where A1 and A2 are in this.actions(), if there is a path p from A1 to A2 in tCFG 
	 * such that no element in p other than A1 and A2 is a member of this.actions().
	 * The source of the returned graph is this.start() and its sink is this.end().
	 * @return upper bound on the execution order of this thread
	 */
	public abstract Graph<InlinedInstruction> threadOrder();
	
	/**
	 * Returns all write instructions to action.instruction.getDeclaredField() 
	 * that might be seen by the given read action.
	 * @requires action in this.actions
	 * @requires action.instruction in SSAGetInstruction + SSAArrayLoadInstruction
	 * @return all write instructions to action.instruction.getDeclaredField() 
	 * that might be seen by the given read action.
	 */
	public abstract Set<InlinedInstruction> visibleWrites(InlinedInstruction action);
}

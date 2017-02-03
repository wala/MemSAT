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
package com.ibm.wala.memsat.concurrent;

import java.util.Map;

import com.ibm.wala.memsat.frontEnd.InlinedInstruction;

import kodkod.ast.Expression;
import kodkod.ast.Relation;



/**
 * Models an execution of a (concurrent) {@linkplain Program program}, in 
 * terms of the actions performed by each of its threads:
 * reads, writes, locks, unlocks and other synchronization actions specified
 * by the execution's {@linkplain MemoryModel}.
 * 
 * @specfield prog: Program
 * @specfield mem: MemoryModel
 * @specfield action: prog.instruction ->one Relation
 * @specfield v: Relation // binary relation representing the value written by each executed write
 * @specfield w: Relation // binary relation representing the write seen by each executed read
 * @specfield location: Relation // binary relation representing the location accessed by each executed read or write
 * @specifeld monitor: Relation // binary relation representing the monitor accessed by each executed lock or unlock
 * 
 * @author etorlak
 */
public interface Execution {

	/**
	 * Returns true if this is a speculative execution.
	 * Otherwise returns false.
	 * @return true if this is a speculative execution.
	 */
	public abstract boolean isSpeculative();
	
	/**
	 * Returns the unary expression that evaluates to the action performed by the given
	 * instruction in this Execution or to the empty set if the given instruction was
	 * not executed in this Execution.
	 * @requires inst in prog.instructions
	 * @return this.action[inst]
	 */
	public abstract Relation action(InlinedInstruction inst);

	/**
	 * Returns the (binary) relation (from write actions to values) that represents the value-written relation for this Execution.
	 * @return this.v
	 */
	public abstract Relation v() ;
	
	/**
	 * Returns the (binary) relation (from read to write actions) that represents the write-seen function for this Execution.
	 * @return this.w
	 */
	public abstract Relation w() ;
	
	/**
	 * Returns the (binary) relation that binds each read/write in this Execution to 
	 * the location (specified as a non-empty set of atoms) that it accesses.
	 * @return this.location
	 */
	public abstract Relation location() ;
	
	/**
	 * Returns the (binary) function that binds each lock/unlock in this Execution to 
	 * the monitor (specified as a single atoms) that it accesses.
	 * @return this.location
	 */
	public abstract Relation monitor() ;
	
	/**
	 * Returns a set of named expressions that should be visualized when displaying
	 * this Execution.  Each expression in the returned map is a binary relation over
	 * the actions executed in this Execution.
	 * @return a set of named expressions that should be visualized when displaying
	 * this Execution.
	 */
	public abstract Map<Expression, String> viz();

}

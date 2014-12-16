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
package com.ibm.wala.memsat.concurrent;

import java.util.List;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.instance.Bounds;

/**
 * Stores the formula and bounds that form a constraint solving problem
 * that is satisfiable iff a given execution is a legal execution of a given program with
 * respect to a particular memory model.
 * 
 * @specfield mem: MemoryModel
 * @specfield prog: Program
 * @specfield exec: Execution
 * @specfield formula: Formula
 * @specfield bounds: Bounds
 * @specfield speculations: seq Execution
 * @specfield commits: seq Expression
 * @invariant some speculations => mem.usesSpeculation()
 * @invariant !exec.isSpeculative()
 * @invariant all e: speculations[int] | e.isSpeculative()
 * @invariant #spculations = #commits
 * @invariant (exec + speculations[int]).(prog + mem) = this.prog + this.mem 
 * @author etorlak
 */
public interface Justification {
	
	/**
	 * Returns the program whose execution is justified by this justification.
	 * @return this.prog
	 */
	public abstract Program program();
	
	/**
	 * Returns the execution justified by this justification.
	 * @return this.exec
	 */
	public abstract Execution execution();
	
	/**
	 * Returns the justification formula.
	 * @return this.formula
	 */
	public abstract Formula formula();
	
	/**
	 * Returns the justification bounds.
	 * @return this.bounds
	 * @return
	 */
	public abstract Bounds bounds();

	/**
	 * Returns the speculative executions used in this justification (if any).
	 * @return this.speculations
	 */
	public abstract List<? extends Execution> speculations();

	/**
	 * Returns the sequence of action sets committed using speculative 
	 * executions (if any).
	 * @return this.commits
	 */
	public abstract List<? extends Expression> commits();
}

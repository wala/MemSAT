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
package com.ibm.wala.memsat.concurrent.memory;


import static com.ibm.wala.memsat.util.Programs.instructions;
import static com.ibm.wala.memsat.util.Strings.instructionNames;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;

import kodkod.ast.Expression;
import kodkod.ast.Relation;

/**
 * A base implementation of the {@linkplain Execution} interface
 * that implements convenience methods for computing 
 * various subsets of executed actions.
 * 
 * @specfield prog: Program
 * @specfield mem: MemoryModel
 * @specfield action: prog.instructions ->one Expression
 * @specfield v, w, location, monitor: Expression
 * 
 * @author etorlak
 */
public abstract class AbstractExecution implements Execution {

	private final Map<InlinedInstruction, Relation> insts;
	private final Relation v, w, location, monitor;
	/**
	 * Constructs a new abstract execution for the given program.
	 * The constructor creates and caches a fresh relation for each
	 * {@linkplain InlinedInstruction} in the given program.  The
	 * relation names are derived from the instruction names and the
	 * given suffix.  This constructor also allocates fresh Relations
	 * for the V, W, location and monitor relations.  These are named
	 * by appending the given suffix to "V," "W," "location," and "monitor,"
	 * respectively.
	 * @effects this.action' in prog.instructions lone->one Relation
	 */
	protected AbstractExecution(Program prog, String suffix) { 
		this.insts = new LinkedHashMap<InlinedInstruction, Relation>();
	
		for(Map.Entry<InlinedInstruction,String> named : instructionNames(instructions(prog.info())).entrySet()) { 
			insts.put(named.getKey(), Relation.unary(named.getValue()+suffix));
		}
		
		this.v = Relation.binary("V"+suffix);
		this.w = Relation.binary("W"+suffix);
		this.location = Relation.binary("location"+suffix);
		this.monitor = Relation.binary("monitor"+suffix);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#action(com.ibm.wala.memsat.frontEnd.InlinedInstruction)
	 */
	public final Relation action(InlinedInstruction inst) { return insts.get(inst); }
	
	/**
	 * Returns a unary expression that evaluates to all actions executed by this Execution.
	 * @return Expression.union(this.action[InlinedInstruction])
	 */
	public final Expression actions() {	return Expression.union(insts.values()); }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#v()
	 */
	public final Relation v() { return v; }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#w()
	 */
	public final Relation w() { return w; }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#location()
	 */
	public final Relation location() { return location; }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#monitor()
	 */
	public final Relation monitor() { return monitor; }
	
	/**
	 * Returns the write last seen by the given read (if r evaluates to a read in this.actions).
	 * @requires r.arity = 1
	 * @return r.(this.w)
	 */
	public final Expression w(Expression r) { return r.join(w); }
	
	/**
	 * Returns the value written by the given write (which may be the empty set).
	 * @requires w.arity = 1
	 * @return w.(this.v)
	 */
	public final Expression v(Expression w) { return w.join(v); }
	
	/**
	 * Returns an expression that evaluates to the location on which the given action is performed
	 * or to the empty set if the given expression does not evaluate to a read/write action in this.actions.
	 * The expression representing the location of an action is not necessarily a singleton.
	 * @requires a.arity = 1
	 * @return a.(this.location)
	 */
	public final Expression locationOf(Expression a) { return a.join(location); }
	
	/**
	 * Returns an expression that evaluates to the monitor on which the given action is performed or to
	 * the empty set if the given expression does not evaluate to a lock/unlock in this.actions.
	 * @requires action.arity = 1
	 * @return a.(this.monitor)
	 */
	public final Expression monitorOf(Expression a) { return a.join(monitor); }
	
	/**
	 * Returns the lock/unlock actions performed on the given monitor or to
	 * the empty set if the given expression does not evaluate to a monitor accessed during this Execution.
	 * @requires m.arity = 1
	 * @return (this.monitor).m
	 */
	public final Expression syncsOn(Expression m) { return monitor.join(m); }
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() { return getClass().getSimpleName(); }

}

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

import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.util.graph.Graph;

/**
 * A relational model of a concurrent program.  A program
 * has one or more threads, each of which has a number
 * of instructions (reads, writes, locks, etc.).  Each instruction,
 * when {@linkplain Execution executed}, performs an <i>action</i>.
 * For example, a write instruction performs a write action on a memory 
 * location (variable) using a particular value. 
 * 
 * 
 * @specfield info: WalaInformation
 * @specfield options: Options
 * @specfield instructions: set InlinedInstruction // all instructions in this program
 * @specfield universe: Universe // universe of interpretation for this program
 * @specfield threads: info.threads.nodes ->one Expression // constant unary expression that represents all threads in this program
 * @specifeld thread: Expression // constant binary expression that binds each action in this.universe to the unique thread that may execute that action
 * @specfield endsBefore: Expression // constant binary expression that specifies the end/start ordering between info.threads
 * @specfield actions: Action ->one Expression // binds each Action to the constant unary expression that contains all actions of that kind in this.universe
 * @specfield instructions = { inst: InlinedInstruction | some n: CGNode | info.concurrentInformation(n).actions().contains(inst) }
 * @author etorlak
 */
public interface Program {
	
	/**
	 * Returns the wala info for this program.
	 * @return this.info 
	 */
	public abstract WalaInformation info();
	
	/**
	 * Returns the constant unary expression that represent the threads with the given root (entry) methods.
	 * @requires roots in this.info.threads.nodes
	 * @return union(this.threads[roots])
	 */
	public abstract Expression threads(Set<CGNode> roots);
	
	/**
	 * Returns the constant unary expression that represents all actions of the specified
	 * kind that may be performed when this program is executed
	 * @return kind.length==0 ? Expression.NONE : Expression.union(this.actions[kind[int]])
	 */
	public abstract Expression allOf(Action... kind) ;
	
	/**
	 * Returns an expression that evaluates to the actions that the given thread(s) may perform
	 * when executed or to the empty set if the given expression does not evaluate to a thread
	 * (or a set of threads) in this.universe.
	 * @requires thread.arity = 1
	 * @return { e: Expression | [[e]] = [[this.thread]] . [[thread]] }
	 */
	public abstract Expression actionsOf(Expression thread);
	
	/**
	 * Returns an expression that evaluates to the thread(s) that may execute the given action(s) or to
	 * the empty set if the given expression does not evaluate to an action (or a set of actions)
	 * in this.universe.
	 * @requires action.arity = 1
	 * @return { e: Expression | [[e]] = [[action]] . [[this.thread]] }
	 */
	public abstract Expression threadOf(Expression action);
	
	/**
	 * Returns the constant binary expression that represents the end/start ordering between threads.
	 * In particular, if t1->t2 is in endsBefore, then the thread t1 must finish executing before t2 can start.
	 * @return this.endsBefore
	 */
	public abstract Expression endsBefore();
	
	/**
	 * Returns a Formula that evaluates to true iff the given execution is sequentially valid with respect
	 * to this program.  A non-speculative execution is valid iff its po, v, and w respect the sequential 
	 * semantics of this program; it violates one or more of the user-specified assertions; and it satisfies 
	 * all of the user-specified assumptions.  A speculative execution is valid iff its po, v and w respect
	 * the sequential semantics of this program.   
	 * @requires exec.prog = this
	 * @return a Formula that evaluates to true iff the given execution is sequentially valid with respect
	 * to this program.
	 */
	public abstract Formula sequentiallyValid(Execution exec) ;

	/**
	 * Returns a {@linkplain BoundsBuilder} for this program.  The returned builder is initialized
	 * with a fresh Bounds object that contains sounds lower/upper bounds on all the relations that define
	 * this program.  The builder provides a set of methods for building bounds for a {@linkplain Justification} 
	 * produced by the {@linkplain MemoryModel} used in the construction of this program.
	 * @return a {@linkplain BoundsBuilder} for this program.
	 */
	public abstract BoundsBuilder builder();
	
	/**
	 * Provides a set of  methods for building analysis bounds for 
	 * a {@linkplain Justification} produced by a particular {@linkplain MemoryModel}.
	 * @specfield prog: Program
	 * @specfield mem: prog.options.memoryModel()
	 * @specfield lower: WalaStructures.instructions(prog) ->one TupleSet
	 * @specfield upper: WalaStructures.instructions(prog) ->one TupleSet
	 * @specfield bounds: Bounds \\ bounds being built
	 * @author etorlak
	 */
	public static interface BoundsBuilder {

		/**
		 * Bounds the key relations that comprise the given execution:  e.action[InlinedInstruction], e.v,
		 * e.w, e.location, and e.monitor.
		 * @requires e.prog = this.prog
		 * @effects this.bounds.relations' = this.bounds.relations + e.action[InlinedInstruction] + e.v + e.w + e.location + e.monitor
		 */
		public abstract void boundExecution(Execution e);
		
		/**
		 * Bounds the given relation from above using the upper bounds on the instructions in the given graph.
		 * @requires r.arity = 2
		 * @effects this.bounds.relations' = this.bounds.relations' + r
		 * @effects this.bounds.lowerBound' = this.bounds.lowerBound
		 * @effects this.bounds.upperBound' = this.bounds.upperBound' ++ 
		 *  r->{t: Tuple | some i1, i2: InlinedInstruction | upper.hasEdge(i1, i2) && t in this.upper[i1]->this.upper[i2] }
		 */
		public abstract void boundOrdering(Relation r, Graph<InlinedInstruction> upper);
		
		/**
		 * Bounds the given relation from above using the upper bounds on the given instructions.
		 * @requires r.arity = 1
		 * @effects this.bounds.relations' = this.bounds.relations' + r
		 * @effects this.bounds.lowerBound' = this.bounds.lowerBound
		 * @effects this.bounds.upperBound' = this.bounds.upperBound' ++ 
		 *  r->{t: Tuple | some i: upper | this.upper[i].contains(t)}
		 */
		public abstract void boundActions(Relation r, Set<InlinedInstruction> insts);	
		
		/**
		 * Returns the built bounds.
		 * @return this.bounds
		 */
		public abstract Bounds build();
	}
}

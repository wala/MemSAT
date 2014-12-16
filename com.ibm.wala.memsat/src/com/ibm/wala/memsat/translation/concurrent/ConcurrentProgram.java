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
package com.ibm.wala.memsat.translation.concurrent;

import static com.ibm.wala.memsat.util.Graphs.nodes;
import static com.ibm.wala.memsat.util.Graphs.root;
import static com.ibm.wala.memsat.util.Graphs.transitiveClosure;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.memsat.translation.MethodTranslation;
import com.ibm.wala.memsat.util.Nodes;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Implementation of the {@linkplain Program} interface based on the
 * Miniatur translation.
 * @author etorlak
 */
final class ConcurrentProgram implements Program {
	private final ConcurrentMemoryHandler handler;
	private final ConcurrentFactory factory;
	private final WalaInformation info;
	private final Map<CGNode,MethodTranslation> transls;

	private final Map<CGNode, Relation> threads;
	private final Relation /* Action ->one Thread */ thread;
	private final Relation /* Thread -> Thread */ endsBefore;
	/**
	 * Constructs a new ConcurrentProgram using the provided memory handler and translation that
	 * were generation using the given handler.
	 * @requires handler.factory.info.threads.nodes = transls.keySet()
	 */
	public ConcurrentProgram(ConcurrentMemoryHandler handler, Map<CGNode,MethodTranslation> transls) {
		this.handler = handler;
		this.factory = handler.factory;
		this.info = factory.base().info();
		this.transls = transls;
				
		this.threads = new LinkedHashMap<CGNode, Relation>();
		for(Iterator<CGNode> itr = DFS.iterateDiscoverTime(info.threads(), root(info.threads())); itr.hasNext(); ) { 
			final CGNode root = itr.next();
			threads.put(root, Relation.unary(root.getMethod().getName().toString()));
		}
		
		this.thread = Relation.binary("thread");	
		this.endsBefore = Relation.binary("endsBefore");
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#allOf(com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action[])
	 */
	public Expression allOf(Action... kind) { 
		switch(kind.length) { 
		case 0 : return Expression.NONE;
		case 1 : return factory.valueOf(kind[0]);
		default : 
			final Set<Expression> acts = new LinkedHashSet<Expression>(kind.length*2);
			for(Action a : kind) { acts.add(factory.valueOf(a)); }
			return Expression.union(acts);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#actionsOf(kodkod.ast.Expression)
	 */
	public Expression actionsOf(Expression thread) { return this.thread.join(thread); }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#threadOf(kodkod.ast.Expression)
	 */
	public Expression threadOf(Expression action) { return action.join(thread); }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#threads(java.util.Set)
	 */
	public Expression threads(Set<CGNode> roots) {
		switch(roots.size()) { 
		case 0 : return Expression.NONE;
		case 1 : return threads.get(roots.iterator().next());
		default :
			final Set<Expression> exprs = new LinkedHashSet<Expression>();
			for(CGNode root : roots) { 
				exprs.add(threads.get(root));
			}
			return Expression.union(exprs);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#endsBefore()
	 */
	public Expression endsBefore() { return endsBefore; }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#info()
	 */
	public WalaInformation info() { return info; }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#sequentiallyValid(com.ibm.wala.memsat.concurrent.Execution)
	 */
	public Formula sequentiallyValid(Execution exec) {
		final Collection<Formula> sv = new LinkedHashSet<Formula>();
		
//		System.out.println("----------------------------");
//		System.out.println(handler);
		
		for(Iterator<CGNode> nodes = DFS.iterateDiscoverTime(info.threads(), root(info.threads())); nodes.hasNext(); ) { 
			final CGNode node = nodes.next();
			final MethodTranslation transl = transls.get(node);
			final WalaConcurrentInformation tInfo = info.concurrentInformation(node);
			final Graph<InlinedInstruction> to = tInfo.threadOrder();
			final Graph<InlinedInstruction> toClosure = transitiveClosure(to);
			
			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(to, tInfo.start()); insts.hasNext(); ) { 
				final InlinedInstruction inst = insts.next();
				final Expression action = exec.action(inst);
				final Formula guard = handler.guardFor(inst);
				
//				System.out.println("----------------------------");
//				System.out.println("INLINED INSTRUCTION: " + inst);
//				System.out.println("CALL STACK: " + inst.callStack());
//				System.out.println("INSTRUCTION: " + inst.instruction());
//				System.out.println("ACTION: " + action);
//				System.out.println("GUARD: " + guard);
				
				sv.add( action.lone() );
				sv.add( guard.iff(action.some()) );
				
				final Expression location = handler.locationOf(inst);
				if (location!=null) { 
					sv.add( guard.implies(action.join(exec.location()).eq(location)) );
					final Expression value = handler.valueWritten(inst);
					if (value!=null) { 
						sv.add( guard.implies(action.join(exec.v()).eq(value) ) );
					}
				} else {
					final Expression monitor = handler.monitorOf(inst);
					if (monitor!=null) { 
						sv.add( guard.implies(action.join(exec.monitor()).eq(monitor)) );
					}
				}
				
				for(Iterator<? extends InlinedInstruction> succs = toClosure.getSuccNodes(inst); succs.hasNext(); ) { 
					final InlinedInstruction succ = succs.next();
					if (factory.mayShareActions(inst, succ)) { 
						sv.add( action.intersection(exec.action(succ)).no() );
					}
				}
			}
			
			sv.addAll( transl.assumptions() );
		
			if (!exec.isSpeculative()) { 
				if (factory.base().options().assertsAreAssumptions()) {
					sv.addAll(transl.assertions());
				} else {
					sv.add(Formula.or(transl.assertions()));
				}
			}
		}
				
		return Nodes.replaceAll(Formula.and(sv), executedReads(exec));
	}

	/**
	 * Returns a map that maps each Relation representing the value of a read instruction
	 * to an expression for that value in terms of exec.w and exec.v.
	 * @return a map that maps each Relation representing the value of a read instruction
	 * to an expression for that value in terms of exec.w and exec.v.
	 */
	private final Map<Relation,Expression> executedReads(final Execution exec) { 
		final Map<Relation, Expression> substitution = new LinkedHashMap<Relation, Expression>();
		for(Map.Entry<InlinedInstruction, Relation> read : handler.valuesRead().entrySet()) { 
			final Expression action = exec.action(read.getKey());
			final Expression val = action.join(exec.w()).join(exec.v());
			substitution.put(read.getValue(), val);
		}
		return substitution;
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program#builder()
	 */
	public BoundsBuilder builder() {
		final Set<Object> atoms = new LinkedHashSet<Object>(factory.atoms());
		atoms.addAll(nodes(info.threads()));
		
		final Bounds bounds = new Bounds(new Universe(atoms));
		factory.boundAll(bounds);
		boundThreads(bounds);
		boundEndsBefore(bounds);
		
		return new ConcurrentBoundsBuilder(bounds, factory);
	}

	/**
	 * Adds the bounds for this.thread and this.threads to the given bounds.
	 * @requires this.atoms in bounds.universe.atoms[int]
	 * @effects bounds.relations' = bounds.relations + this.thread + this.threads
	 */
	private final void boundThreads(Bounds bounds) { 
		final TupleFactory tuples = bounds.universe().factory();
		for(CGNode root : info.threads()) {
			bounds.boundExactly(threads.get(root), tuples.setOf(root));
		}
		
		final TupleSet threadBound = tuples.noneOf(2);
		for(CGNode n : info.threads()) { 
			final TupleSet t = tuples.setOf(n);
			for(InlinedInstruction inst : info.concurrentInformation(n).actions()) { 
				threadBound.addAll( factory.actionAtoms(tuples, inst).product(t) );
			}
		}
		bounds.boundExactly(thread, threadBound);
	}
	
	/**
	 * Adds the bounds for this.endsBefore to the given bounds.
	 * @requires this.atoms in bounds.universe.atoms[int]
	 * @effects bounds.relations' = bounds.relations + this.endsBefore
	 */
	private final void boundEndsBefore(Bounds bounds) { 
		final TupleFactory tuples = bounds.universe().factory();
		final Graph<CGNode> eb = transitiveClosure(info.threads());
		final TupleSet endsBound = tuples.noneOf(2);
		for(CGNode n : info.threads()) { 
			for(Iterator<? extends CGNode> succs = eb.getSuccNodes(n); succs.hasNext(); ) { 
				endsBound.add( tuples.tuple(n, succs.next()) );
			}
		}
		bounds.boundExactly(endsBefore, endsBound);
	}

	
}

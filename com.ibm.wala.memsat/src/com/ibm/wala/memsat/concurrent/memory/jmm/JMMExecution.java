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
package com.ibm.wala.memsat.concurrent.memory.jmm;

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.END;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.LOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_WRITE;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.START;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.UNLOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_WRITE;
import static com.ibm.wala.memsat.util.Graphs.root;
import static com.ibm.wala.memsat.util.Nodes.totalOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.concurrent.memory.AbstractExecution;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.util.Graphs;
import com.ibm.wala.memsat.util.Nodes;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
/**
 * An execution specific to the JMM, based on 
 * <ol>
 * <li> J.�Sevc�k and D.�Aspinall. On validity of program transformations in 
 * the Java memory model. In ECOOP �08, volume 5142 of LNCS, pages 27�51. Springer Berlin, 2008. </li>
 * </ol>
 * 
 * @specfield prog: Program
 * @specfield mem: MemoryModel
 * @specfield actions, po, so, hb, sw, v, w: Expression
 * 
 * @author etorlak
 */
public final class JMMExecution extends AbstractExecution {
	private final Relation so;
	private final Expression po, sw, hb;
	private final Formula wellFormed;
	private final boolean speculative;
	
	/**
	 * Creates a new JMMExecution for the given program, using the given speculation
	 * flag and the given suffix in the names of all relations it allocates.
	 */
	private JMMExecution(Program prog, boolean speculative, String suffix) { 
		super(prog, suffix.length() > 0 ? "_" + suffix : suffix);
		this.speculative = speculative;
		this.po = computePO(prog);//Relation.binary("po"+suffix);
		this.so = Relation.binary("so"+suffix);
		
		this.sw = computeSW(prog);	
		this.hb = po.union(sw).closure();
		
		this.wellFormed = computeWellFormed(prog);
	}
	
	/**
	 * Computes the program order expression for this execution of the given program.
	 * @return po for this execution the given program
	 */
	private final Expression computePO(Program p) { 
		final List<Expression> ecfg = new ArrayList<Expression>();
		final WalaInformation info = p.info();
		for(Iterator<CGNode> nodes = DFS.iterateDiscoverTime(info.threads(), root(info.threads())); nodes.hasNext(); ) { 
			final WalaConcurrentInformation tInfo = info.concurrentInformation(nodes.next());
			final Graph<InlinedInstruction> to = tInfo.threadOrder();
			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(to, tInfo.start()); insts.hasNext(); ) { 
				final InlinedInstruction inst = insts.next();
				final Relation action = action(inst);
				ecfg.add(action.product(action));
				for(Iterator<? extends InlinedInstruction> succs = to.getSuccNodes(inst); succs.hasNext(); ) { 
					ecfg.add( action.product(action(succs.next())) );
				}
			}
		}
		return Expression.union(ecfg).closure();
	}
	
	/**
	 * Computes the synchronizes-with expression for this execution of the given program.
	 * @return sw for this execution the given program
	 */
	private final Expression computeSW(Program p) {
		
		final Variable a = Variable.unary("a"), b = Variable.unary("b");
		final Formula synced = a.product(b).in(so);
		final Expression swLocks = synced.and(monitorOf(a).eq(monitorOf(b))).
			comprehension(a.oneOf(p.allOf(UNLOCK)).and(b.oneOf(p.allOf(LOCK))));
		final Expression swVolatiles = synced.and(locationOf(a).eq(locationOf(b))).
			comprehension(a.oneOf(p.allOf(VOLATILE_WRITE)).and(b.oneOf(p.allOf(VOLATILE_READ))));
		final Expression swThreads = synced.and(p.threadOf(a).product(p.threadOf(b)).in(p.endsBefore())).
			comprehension(a.oneOf(p.allOf(END)).and(b.oneOf(p.allOf(START))));

		return Expression.union(swLocks, swVolatiles, swThreads);
	}

	
	/**
	 * Computes the well-formedness constraints for this execution of the given program.
	 * @return  well-formedness constraints for this execution of the given program
	 */
	private final Formula computeWellFormed(Program p) { 
		final List<Formula> wf = new ArrayList<Formula>(15);
		
		final Expression threads = p.threads(Graphs.nodes(p.info().threads()));
		final Expression actions = actions();
		final Expression reads = actions.intersection(p.allOf(NORMAL_READ, VOLATILE_READ));
		final Expression volatileReads = actions.intersection(p.allOf(VOLATILE_READ));
		final Expression writes = actions.intersection(p.allOf(NORMAL_WRITE, VOLATILE_WRITE));
		final Expression volatileWrites = actions.intersection(p.allOf(VOLATILE_WRITE));
		final Expression locks = actions.intersection(p.allOf(LOCK));
		final Expression syncs = actions.intersection(p.allOf(VOLATILE_READ, VOLATILE_WRITE, LOCK, UNLOCK, START, END));
		
		
		// Def 6.3-6.4:  all actions in a thread happen after the  thread start and before the  thread end actions
		// No need for explicit constraint: enforced by Program bounds.
		
		// Def 6.5:  initialization thread contains only start, end and writes
		// No need for explicit constraint:  see WalaInformation.concurrentInformation
		
		// Def 7.2: po restricted to the actions of one thread is a total order and
		// does not relate actions of different threads
		// all t: this.threads | totalOrder[this.po, t.actions & this.actions]
		// all a, b: this.actions | a->b in this.po => a.thread = b.thread	
		final Variable t = Variable.unary("t");
		final Variable a = Variable.unary("a"), b = Variable.unary("b");
//		wf.add( totalOrder(po, actions.intersection(p.actionsOf(t))).forAll(t.oneOf(threads)) );
//		wf.add( a.product(b).in(po).implies(p.threadOf(a).eq(p.threadOf(b))).forAll(a.oneOf(actions).and(b.oneOf(actions))) );
		
		// po is consistent with the program text
//		final WalaInformation info = p.info();
//		for(Iterator<CGNode> nodes = DFS.iterateDiscoverTime(info.threads(), root(info.threads())); nodes.hasNext(); ) { 
//			final WalaConcurrentInformation tInfo = info.concurrentInformation(nodes.next());
//			final Graph<InlinedInstruction> to = tInfo.threadOrder();
//			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(to, tInfo.start()); insts.hasNext(); ) { 
//				final InlinedInstruction inst = insts.next();
//				final Relation action = action(inst);
//				for(Iterator<? extends InlinedInstruction> succs = to.getSuccNodes(inst); succs.hasNext(); ) { 
//					wf.add( action.product(action(succs.next())).in(po) );
//				}
//			}
//		}
		// Def 7.3:  so is a total order on executed synchronization actions
		// totalOrder[this.so, syncs]
		wf.add( totalOrder(so, syncs) );
		
		// Def 7.4: so is consistent with po
		// all a, b: this.actions | (a->b in this.so and b->a in this.po) => a = b
		wf.add( a.product(b).in(so).and(b.product(a).in(po)).implies(a.eq(b)).forAll(a.oneOf(actions).and(b.oneOf(actions))) );
		
		// Def 7.5:  the write-seen must be at the same location as the read;
		// this constraint, in conjunction with "volatility" being associated with locations rather than memory accesses, 
		// ensures that the rule D6.5 holds (i.e. W is properly typed: for every non-volatile read r, W(r) is non-volatile,
		// and for every volatile read r, W(r) is volatile)
		// all r: this.reads | this.W[r].location = r.location and this.W[r] in this.actions and one this.W[r]
		final Variable r = Variable.unary("r");
		wf.add( Formula.and(locationOf(w(r)).eq(locationOf(r)), w(r).in(actions), w(r).one()).forAll(r.oneOf(reads)) );
		
		
		// Def 7.6: locking is proper; for all lock actions l in A on monitors m and all threads t different
		// from the thread of l, the number of locks in t before l in so is the same as the number of unlocks in t before l in so.
		// all l: this.locks, t: this.threads - l.thread | let prevSyncs = (t.actions & (l.monitor).~monitor & ^(this.so).l) |
		//   #(prevSyncs & Lock) = #(prevSyncs & Unlock)
		final Variable l = Variable.unary("l");
		final Expression prevSyncs = Expression.intersection(so.closure().join(l), syncsOn(monitorOf(l)), p.actionsOf(t));  
		wf.add( prevSyncs.intersection(p.allOf(LOCK)).count().eq(prevSyncs.intersection(p.allOf(UNLOCK)).count()).
					forAll(l.oneOf(locks).and(t.oneOf(threads.difference(p.threadOf(l)))))  );
		
		// Def 7.8: so is consistent with W; for every volatile read r of a variable v we have W(r) <=_{so} r
		// and for any volatile write w to v, either w <=_{so} W(r) or r <=_{so} w.
		// all r: this.actions & Volatile.reads, w: this.actions & Volatile.writes | 
		//	r.location=w.location => (this.W[r] -> r in this.so and (w -> this.W[r] in this.so or r -> w in this.so))
		final Variable w = Variable.unary("w");
		wf.add( locationOf(r).eq(locationOf(w)).implies(
				w(r).product(r).in(so).and(w.product(w(r)).in(so).or(r.product(w).in(so)))).
					forAll(r.oneOf(volatileReads).and(w.oneOf(volatileWrites))) );
		
		// Def 7.9: hb is consistent with W; for all reads r of v it holds that r !<=_{hb} W(r) and
		// there is no intervening write w to v, i.e. if W(r) <=_{hb} w <=_{hb} r and w writes to v then W(r) = w.
		// all r: this.reads, w: this.writes | 
		//	r.location=w.location => 
		//   (r->this.W[r] !in this.hb and ((this.W[r] -> w in this.hb and w -> r in this.hb) => this.W[r] = w))
		wf.add( locationOf(r).eq(locationOf(w)).implies(
				r.product(w(r)).in(hb).not().and(w(r).product(w).in(hb).and(w.product(r).in(hb)).implies(w(r).eq(w)))).
					forAll(r.oneOf(reads).and(w.oneOf(writes))) );
		
		// Def 7.10: the initialization thread finishes before any other thread starts
	  	// No need for explicit constraint: enforced by Program bounds.
	  	
		// Def 7.7: program order is intra-thread consistent (sequentially valid)
		wf.add( p.sequentiallyValid(this) );
		
		return Formula.and(wf);

	}
	
	
	/**
	 * Returns a main (non-speculative) execution for the given program.
	 * @return a main (non-speculative) execution for the given program.
	 */
	static JMMExecution main(Program prog) {
		return new JMMExecution(prog, false, "");
	}
	
	/**
	 * Returns a list of the given size, initialized with fresh speculative
	 * executions of the given program.  
	 * @return a list of the given size, initialized with fresh speculative
	 * executions of the given program.
	 */
	static List<JMMExecution> speculative(Program prog, int size) {
		final List<JMMExecution> speculations = new ArrayList<JMMExecution>(size);
		for(int i = 0; i < size; i++) { 
			speculations.add( new JMMExecution(prog, true, String.valueOf(i)) );
		}
		return speculations;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#isSpeculative()
	 */
	public boolean isSpeculative() { return speculative; }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#po()
	 */
	public Expression po() { return po; }
	
	/**
	 * Returns the synchronization order relation for this execution.
	 * @return this.so
	 */
	public Relation so() { return so; }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#hb()
	 */
	public Expression hb() { return hb; }

	/**
	 * Returns the synchronizes-with relation for this execution.
	 * @return this.sw
	 */
	public Expression sw() { return sw; }

	/**
	 * Returns a formula that evaluates to true iff this execution is well-formed
	 * with respect to this.prog.
	 * @return a formula that evaluates to true iff this execution is well-formed
	 * with respect to this.prog.
	 */
	public Formula wellFormed() { return wellFormed; }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#viz()
	 */
	public Map<Expression, String> viz() {
		return Collections.singletonMap(Nodes.transitiveReduction(hb).difference(Expression.IDEN), "happensBefore");
	}


}

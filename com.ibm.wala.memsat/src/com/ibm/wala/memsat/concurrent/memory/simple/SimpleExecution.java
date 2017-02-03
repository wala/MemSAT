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
package com.ibm.wala.memsat.concurrent.memory.simple;

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.LOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_WRITE;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.UNLOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_WRITE;
import static com.ibm.wala.memsat.util.Graphs.roots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.concurrent.memory.AbstractExecution;
import com.ibm.wala.memsat.util.Graphs;
import com.ibm.wala.memsat.util.Nodes;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;

/**
 * A base implementation of the {@linkplain Execution} interface, 
 * suitable for the specification of simple (non-hybrid) memory models 
 * defined by Yang et al [1].
 * 
 * @see  [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * 
 * @specfield ords: Object ->lone Relation // ordering relations
 * 
 * @author etorlak
 *
 */
final class SimpleExecution<T> extends AbstractExecution {
	private final Program prog;
	private final Map<T, Relation> ords;
	private final Map<Expression, String> viz;
	/**
	 * Constructs a simple execution for the given program, using the
	 * given ordering relations and the given visualization expressions.
	 * This constructor assumes that the given maps will not change while
	 * in use by the class.  It also assumes that the viz map is unmodifiable.
	 * @requires all r: ords.values | r.arity = 2
	 * @effects this.ords' = ords
	 */
	public SimpleExecution(Program prog, Map<T, Relation> ords, Map<Expression,String> viz) {
		super(prog, "");
		this.prog = prog;
		this.ords = ords;
		this.viz = viz;
	}
	
	/**
	 * Constructs a simple execution for the given program, using the
	 * given ordering relations and the given visualization expressions.
	 * This constructor assumes that the given map will not change while
	 * in use by the class.  The viz map for this execution is obtained by
	 * mapping the transitive reduction of each Relation in ords.values() to its name.
	 * @requires all r: ords.values | r.arity = 2
	 * @effects this.ords' = ords
	 */
	public SimpleExecution(Program prog, Map<T, Relation> ords) { this(prog, ords, viz(ords)); }

	/**
	 * Returns a map from each relation in ords to its name.
	 * @return a map from each relation in ords to its name.
	 */
	private static final Map<Expression, String> viz(Map<?, Relation> ords) { 
		final Map<Expression,String> viz = new LinkedHashMap<Expression, String>();
		for(Relation ord : ords.values()) { 
			viz.put(Nodes.transitiveReduction(ord), ord.name());
//			viz.put(ord, ord.name());
		}
		return Collections.unmodifiableMap(viz);
	}
	
	/**
	 * Returns the ordering relation that corresponds to the given object.
	 * @return this.ords[o]
	 */
	protected final Relation ordering(T o) { return ords.get(o); }

	/**
	 * Returns the set of objects that are mapped to orderings in this.ords
	 * @return this.ords.Relation
	 */
	protected final Set<T> ordered() { return Collections.unmodifiableSet(ords.keySet()); }
	
	/**
	 * Returns the ordering relations in this ords.
	 * @return this.ords[Object]
	 */
	protected final Collection<Relation> orderings() { return Collections.unmodifiableCollection(ords.values()); }
	
	/**
	 * Returns the requireWeakTotalOrder constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @return all i, j: acts | i != j => (i->j in ord or j->i in ord)
	 */
	protected final Formula weakTotalOrder(Expression acts, Expression ord) { 
		final Variable i = Variable.unary("i"), j = Variable.unary("j");
		return i.eq(j).not().implies(i.product(j).in(ord).or(j.product(i).in(ord))).forAll(i.oneOf(acts).and(j.oneOf(acts)));
	}
	
	/**
	 * Returns the requireTransitiveOrder constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @return all i, j, k: acts | i->j in ord and j->k in ord => i->k in ord
	 */
	protected final Formula transitiveOrder(Expression acts, Expression ord) { 
		// replacing the original definition with a more efficient relational one ...
//		final Variable i = Variable.unary("i"), j = Variable.unary("j"), k = Variable.unary("k");
//		return i.product(j).in(ord).and(j.product(k).in(ord)).implies(i.product(k).in(ord)).forAll(i.oneOf(acts).and(j.oneOf(acts)).and(k.oneOf(acts)));
		final Expression actOrd = ord.intersection(acts.product(acts));
		return actOrd.join(actOrd).in(actOrd);
	}
	
	/**
	 * Returns the requireAsymmetricOrder constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @return all i, j: acts | i->j in ord => !(j->i in ord)
	 */
	protected final Formula asymmetricOrder(Expression acts, Expression ord) { 
		final Variable i = Variable.unary("i"), j = Variable.unary("j");
		return i.product(j).in(ord).implies(j.product(i).in(ord).not()).forAll(i.oneOf(acts).and(j.oneOf(acts)));
	}
	
	/**
	 * Returns the requireReadValue constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @requires [[acts]] in [[this.actions]]
	 * @return all k: acts & Read | 
	 *  one this.w[k] and this.w[k] in acts and this.location[k] = this.location[this.w[k]] and !(k->this.w[k] in ord) and 
	 *  (all j: acts & Write | 
	 *    !(this.location[k] = this.location[j] and i->j in ord and j->k in ord) )
	 */
	protected final Formula readValue(Expression acts, Expression ord) { 
		final Variable k = Variable.unary("k"), j = Variable.unary("j");
		final Expression i = w(k);
		final List<Formula> formulas = new ArrayList<Formula>();
		formulas.add( i.one().forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))) );
		formulas.add( i.in(acts).forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))) );
		formulas.add( locationOf(k).eq(locationOf(i)).forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))) );
		formulas.add( k.product(i).in(ord).not().forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))) );
		formulas.add( Formula.and(locationOf(k).eq(locationOf(j)), i.product(j).in(ord), j.product(k).in(ord)).not().
					forAll(j.oneOf(acts.intersection(prog.allOf(NORMAL_WRITE,VOLATILE_WRITE)))).
					forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))) );
		return Formula.and(formulas);
//		formulas.add( i.one() );
//		formulas.add( i.in(acts) );
//		formulas.add( locationOf(k).eq(locationOf(i)) );
//		formulas.add( k.product(i).in(ord).not() );
//		formulas.add( Formula.and(locationOf(k).eq(locationOf(j)), i.product(j).in(ord), j.product(k).in(ord)).not().forAll(j.oneOf(acts.intersection(prog.allOf(NORMAL_WRITE,VOLATILE_WRITE)))) );
//		return Formula.and(formulas).forAll(k.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ))));

	}
	
	/**
	 * Returns the requireSerialization constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @requires [[acts]] in [[this.actions]]
	 * @return this.weakTotalOrder(acts, ord) and this.transitiveOrder(acts, ord) and this.asymmetricOrder(acts,ord) and this.readValue(acts,ord)
	 */
	protected final Formula serialization(Expression acts, Expression ord) { 
		return Formula.and(weakTotalOrder(acts, ord), transitiveOrder(acts, ord), asymmetricOrder(acts, ord), readValue(acts, ord));
	}
	
	/**
	 * Returns the requireProgramOrder constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @requires [[acts]] in [[this.actions]]
	 * @return 
	 *  let init  = prog.threads(root(prog.info.threads)) |
	 *   (all i: acts, j: acts & this.prog.actionsOf(this.prog.threadOf(i)) | i->j in ord + ~ord) and
	 *   (all i: acts & prog.actionsOf(init), j: acts - prog.actionsOf(init) | i->j in ord)
	 */
	protected final Formula programOrder(Expression acts, Expression ord) { 
		final Variable i = Variable.unary("i"), j = Variable.unary("j");
		final Expression init = prog.threads(roots(prog.info().threads()));
		final Formula f0 = i.product(j).in(ord.union(ord.transpose())).forAll(i.oneOf(acts).and(j.oneOf(acts.intersection(prog.actionsOf(prog.threadOf(i))).difference(i))));
		final Formula f1 = i.product(j).in(ord).forAll(i.oneOf(acts.intersection(prog.actionsOf(init))).and(j.oneOf(acts.difference(prog.actionsOf(init)))));
		return f0.and(f1);
	}
	
	/**
	 * Returns the requireWriteIntoOrder constraint from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and ord.arity = 2 
	 * @requires [[acts]] in [[this.actions]]
	 * @return all i: acts & Write, j: acts & Read | i = this.w[j] => i->j in ord
	 */
	protected final Formula writeIntoOrder(Expression acts, Expression ord) { 
		final Variable i = Variable.unary("i"), j = Variable.unary("j");
		return (i.eq(w(j))).implies(i.product(j).in(ord)).
			forAll(i.oneOf(acts.intersection(prog.allOf(NORMAL_WRITE, VOLATILE_WRITE))).
					and(j.oneOf(acts.intersection(prog.allOf(NORMAL_READ, VOLATILE_READ)))));
	}
	
	/**
	 * Returns a formula stating that locking is proper in the given ordering over this.actions 
	 * @return all l: this.actions & Lock, t: Thread - l.thread | let prevSyncs = (t.actions & (l.(this.monitor)).~(this.monitor) & ^ord.l) |
     *  #(prevSyncs & Lock) = #(prevSyncs & Unlock)
	 */
	protected final Formula properLocking(Expression ord) { 
		final Variable l = Variable.unary("l");
		final Variable t = Variable.unary("t");
		final Expression threads = prog.threads(Graphs.nodes(prog.info().threads()));
		final Expression locks = actions().intersection(prog.allOf(LOCK));
		final Expression prevSyncs = Expression.intersection(ord.closure().join(l), syncsOn(monitorOf(l)), prog.actionsOf(t));  
		return prevSyncs.intersection(prog.allOf(LOCK)).count().eq(prevSyncs.intersection(prog.allOf(UNLOCK)).count()).
					forAll(l.oneOf(locks).and(t.oneOf(threads.difference(prog.threadOf(l)))))  ;
	}
	
	/**
	 * Returns the restrictVar expression from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and var = 1 
	 * @requires [[acts]] in [[this.actions]]
	 * @return { i: acts | this.location[i] = var }
	 */
	protected final Expression restrictVar(Expression acts, Expression var) {  
		final Variable i = Variable.unary("i");
		return locationOf(i).eq(var).comprehension(i.oneOf(acts));
	}
	
	/**
	 * Returns the restrictVarWr expression from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and var = 1
	 * @requires [[acts]] in [[this.actions]]
	 * @return { i: acts & Write | this.location[i] = var }
	 */
	protected final Expression restrictVarWr(Expression acts, Expression var) {  
		final Variable i = Variable.unary("i");
		return locationOf(i).eq(var).comprehension(i.oneOf(acts.intersection(prog.allOf(NORMAL_WRITE, VOLATILE_WRITE))));
	}
	
	
	/**
	 * Returns the restrictProc expression from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts.arity = 1 and p = 1
	 * @return { i: acts | this.prog.threadOf(i) = p or (this.prog.threadOf(i) != p and !(i in Read) ) }
	 */
	protected final Expression restrictProc(Expression acts, Expression p) {  
		final Variable i = Variable.unary("i");
		final Formula f = prog.threadOf(i).eq(p);
		return f.or(f.not().and(i.in(prog.allOf(NORMAL_READ,VOLATILE_READ)).not())).comprehension(i.oneOf(acts));
	}
	
	/**
	 * Returns the mapConstraints predicate from the Nemos formalization of non-hybrid memory consistency models.
	 * @requires acts1.arity = acts2.arity = 1 and ord1.arity = ord2.arity = 2 
	 * @return all i, j: acts1 & acts2 | i->j in ord1 iff i->j in ord2 
	 */
	protected final Formula mapConstraints(Expression acts1, Expression ord1, Expression acts2, Expression ord2) { 
		final Variable i = Variable.unary("i"), j = Variable.unary("j");
		return i.product(j).in(ord1).implies(i.product(j).in(ord2)).
			forAll(i.oneOf(acts1.intersection(acts2)).and(j.oneOf(acts1.intersection(acts2))));
	}
	
	/**
	 * Returns a formula that constrains this execution to be sequentially valid.
	 * @return a formula that constrains this execution to be sequentially valid.
	 */
	protected final Formula wellFormed() { return prog.sequentiallyValid(this); }
	
	/**
	 * Returns false.
	 * @return false
	 */
	public final boolean isSpeculative() {	return false; }

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Execution#viz()
	 */
	public Map<Expression, String> viz() { return viz; }

}

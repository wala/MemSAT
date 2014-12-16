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
package com.ibm.wala.memsat.representation;

import static com.ibm.wala.memsat.util.Strings.prettyPrint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.util.ints.SparseSequence;
import kodkod.util.ints.TreeSequence;

import com.ibm.wala.memsat.util.Nodes;


/**
 * Wraps a collection of Kodkod expressions that represent the contents of a given array.
 * 
 * @specfield refs: Expression // expression representing the arrays whose contents are represented by this array expression
 * @specfield card: [0..) // maximum number of entries, in each array from this.arrays, storing a value other than the default
 * @specfield indices: [0..maxCard) lone->one Expression // indices[i] maps each member of this.arrays to a bitset encoding of its ith index
 * @specfield values: [0..maxCard) lone->one (Node+RealExpression) // values[i] maps each member of this.arrays to the value it stores at the ith index
 * @specfield zero: Expression // default value
 * @invariant all i: [0..maxCard) | indices[i].arity() = 2 and values[i].arity() = zero.arity() + 1

 * @author Emina Torlak
 */
public final class ArrayExpression<T> extends HeapExpression<T>  {
	private final Expression domain;
	private final Expression[] indices, values;
	private final Interpreter<T> valMeaning;
	private final Interpreter<IntExpression> idxMeaning;
	
	/**
	 * Constructs a new array from the given arguments.
	 * @effects
	 * this.refs' = refs and
	 * this.card' = card and
	 * this.zero' = valMeaning.zero() and
	 * all i: [0..card) | 
	 *   this.indeces'[i] = Relation.binary(name+"@idx"+i) and 
	 *   this.values'[i] = Relation.nary(name+"@val"+i,this.zero'.arity+1)
	 */
	private ArrayExpression(String name, int card, Expression refs,
			Interpreter<IntExpression> idxMeaning,  
			Interpreter<T> valMeaning) { 
		this.idxMeaning = idxMeaning;
		this.valMeaning = valMeaning;
		this.indices = new Expression[card];
		this.values = new Expression[card];
		this.domain = refs;
		final int valArity = valMeaning.defaultObj().arity() + 1;
		for(int i = 0; i < card; i++) {
			indices[i] = Relation.binary(name+"@idx"+i);
			values[i] = Relation.nary(name + "@val"+i, valArity);
		}
	}
	
	/**
	 * Constructs a new array expression using the given template and values.
	 * specified instance at the specified index.
	 * @requires values.length = old.values.length
	 * @effects this.indeces' = old.indeces' and this.refs' = old.refs and
	 * this.zero' = old.zero and this.card' = old.card and
	 * this.values' = values
	 */
	private ArrayExpression(ArrayExpression<T> old, Expression[] values) { 
		this.idxMeaning = old.idxMeaning;
		this.valMeaning = old.valMeaning;
		this.indices = old.indices;
		this.domain = old.domain;
		this.values = values;
	}
	
	
	
	/**
	 * Returns an array expression constructed from the given arguments.
	 * @return { a: ArrayExpression<T> | 
	 *   a.card = card and a.zero = valM.zero() and
	 *   a.refs = refs and 
	 *   all i: [0..card) | 
	 *     a.indeces[i] = Relation.binary(name+"@idx"+i) and 
	 *     a.values[i] = Relation.nary(name+"@val"+i,this.zero'.arity+1) }
	 */
	static <T> ArrayExpression<T> array(String name, int card, Expression refs, Interpreter<IntExpression> intM, Interpreter<T> valM) {
		return new ArrayExpression<T>(name,card,refs,intM,valM);
	}

	
	/**
	 * Returns  this.indeces[i]. 
	 * @return this.indeces[i]
	 */
	final Expression index(int i) { return indices[i]; }
	
	/**
	 * Returns this.values[i]. 
	 * @return this.values[i]
	 */
	final Expression value(int i) { return values[i]; }

	/**
	 * Returns true.
	 * @return true
	 * @see com.ibm.wala.memsat.representation.HeapExpression#isArray()
	 */
	public final boolean isArray() { return true; }
	
	/**
	 * Returns the interpreter used for interpreting this.indeces.
	 * @return interpreter used for interpreting this.indeces.
	 */
	public final Interpreter<IntExpression> indexInterpreter() { return idxMeaning; }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.representation.HeapExpression#valueInterpreter()
	 */
	public final Interpreter<T> valueInterpreter() { return valMeaning; }
		
	/**
	 * Returns this.card. 
	 * @return this.card
	 */
	public final int cardinality() { return indices.length; }
	
	/**
	 * Returns the Expression encoding the set of instances whose contents field is 
	 * represented by this array expression.
	 * @return this.refs
	 */
	public final Expression instances() { return domain; }
	
	/**
	 * Returns the ith non-default index for the specified array reference.
	 * @requires 0 <= i < this.card
	 * @return [[ref]] . [[ this.indices[i] ]]
	 */
	public final IntExpression index(Expression ref, int i) { 
		return idxMeaning.fromObj(ref.join(indices[i]));
	}
	
	/**
	 * Returns the value at the ith non-default index for the specified array reference.
	 * @requires 0 <= i < this.card
	 * @return [[ref]] . [[ this.values[i] ]]
	 */
	public final T value(Expression ref, int i) { 
		return valMeaning.fromObj(ref.join(values[i]));
	}
	
	/**
	 * Returns the value that the given array stores at the specified index.  
	 * @requires ref.arity = 1 
	 * @return 
	 * let i = this.indices, v = this.values, c = this.card, empty = Utils.empty(this.zero.arity)) | 
	 *  { t: T | one e: this.convert[t] | [[e]] = 
	 *   ([[ref]].[[i[0]]] = [[index]] => [[ref]].[[v[0]]] else empty) + ... + 
	 *   ([[ref]].[[i[c-1]]] = [[index]] => [[ref]].[[v[c-1]]] else empty) + 
	 *   ([[ref]].[[i[0]]] != [[index]] and ... and [[ref]].[[i[c-1]]] != [[index]] => 
	 *    zero else empty) }
	 */
	public final T read(Expression ref, IntExpression index) {
		final int size = indices.length;
		final Expression zero = valMeaning.defaultObj();
		final Expression idx = idxMeaning.toObj(index);
		
		final Formula[] idxTest = new Formula[size];
		final Expression[] valsAtIdx = new Expression[size+1];
		final Expression empty = Nodes.empty(zero.arity());
		
		for(int i = 0; i < size; i++) {
			idxTest[i] = ref.join(indices[i]).eq(idx);
			valsAtIdx[i] = idxTest[i].thenElse(ref.join(values[i]), empty);
		}
		
		for(int i = 0; i < size; i++) {
			idxTest[i] = idxTest[i].not();
		}
		
		valsAtIdx[size] = Formula.and(idxTest).thenElse(zero, empty);
		return valMeaning.fromObj(Expression.union(valsAtIdx));

	}

	/**
	 * Returns an array expression that represents the state of this ArrayExpression
	 * after the specified value has been written to the given index of <tt>ref</tt>.  
	 * @requires ref.arity = 1 
	 * @return 
	 * let i = this.indices, v = this.values, c = this.card, empty = Utils.empty(this.zero.arity)  |
	 *  { a: ArrayExpression | a.indices = i and a.card = c and a.zero = this.zero and
	 *    (all j: [0..c) | [[a.values[j]]] = 
	 *     [[v[j]]] ++ ([[ref]].[[i[j]]] = [[index]] => [[ref]]->[[value]] else empty)) }
	 */
	public final ArrayExpression<T> write(Expression ref, IntExpression index, T value) {
		final Expression idx = idxMeaning.toObj(index), val = valMeaning.toObj(value);
		final Expression[] retValues = new Expression[values.length];
		
		final Expression empty = Nodes.empty(valMeaning.defaultObj().arity()+1);
		final Expression overrider = ref.product(val);
		for(int i = 0; i < values.length; i++) {
			Formula idxTest = ref.join(indices[i]).eq(idx);
			retValues[i] = values[i].override(idxTest.thenElse(overrider, empty));
		}
		
		return new ArrayExpression<T>(this, retValues);
	}
	
	/**
	 * Returns the sparse sequence mapping each index in this.indeces[ref]
	 * to its corresponding value in this.values[ref], as given by the
	 * specified evaluator. In particular, if the
	 * type if this.values is RealExpression, then the returned value will be 
	 * a sequence of Floats; if the type of 
	 * this value is an IntExpression, the returned value will be a sequence of Integers; 
	 * if the type of this value is an Expression, the returned value will be a sequence of Objects; 
	 * finally, if the type of this.values is Formula, the returned value will be a sequence of Booleans.
	 * @requires  the given evaluator's instance has bindings
	 * for all relations (if any) at the leaves of this array expression.
	 * @requires eval.evaluate(ref) in eval.evaluate(this.refs)
	 * @return sparse sequence mapping each index in this.indeces[ref]
	 * to its corresponding value in this.values[ref], as given by the
	 * specified evaluator.
	 */
	@SuppressWarnings("unchecked")
	public final <V> SparseSequence<V> evaluate(Expression ref, Evaluator eval) {
		final SparseSequence<V> ret = new TreeSequence<V>();
		for(int i = 0, card = cardinality(); i < card; i++) { 
			int idx = eval.evaluate(idxMeaning.fromObj(ref.join(indices[i])));
			V value = (V) valMeaning.evaluate(valMeaning.fromObj(ref.join(values[i])),eval);
			ret.put(idx, value);
		}
		return ret;
	}

	/**
	 * Returns an array expression that evaluates to the ith value returned by the 
	 * arrays' iterator iff the ith guard returned by the guards' iterator evaluates
	 * to true.  This method assumes that at most one of the given guards 
	 * evaluates to true.  If none do, the returned value will evaluate to 
	 * an empty array (i.e. an array that maps everything in its domain to 
	 * a T whose Expression value is the empty set).
	 * @requires guards and values contain the same (positive) number of objects
	 * @requires at most one of the guards can ever evaluate to true.
	 * @requires all array expressions in the given collection represent the same array
	 * @return an array expression that evaluates to the ith value returned by the 
	 * arrays' iterator iff the ith guard returned by the guards' iterator evaluates
	 * to true. 
	 */
	@SuppressWarnings("unchecked")
	static <T> ArrayExpression<T> phi(Collection<Formula> guards, Collection<ArrayExpression<T>> arrays) {
		final int phiNum = guards.size();
		assert phiNum == arrays.size();
		assert phiNum > 0;
		
		final ArrayExpression<T> first = arrays.iterator().next();
		final Expression empty = Nodes.empty(first.valMeaning.defaultObj().arity()+1);
		
		final int card = first.cardinality();
		final List<Expression>[] phis = new ArrayList[card];
		for(int i = 0; i < card; i++) { 
			phis[i] = new ArrayList<Expression>(phiNum);
		}
		
		final Iterator<Formula> gItr = guards.iterator();
		final Iterator<ArrayExpression<T>> aItr = arrays.iterator();
		
		while(gItr.hasNext()) { 
			Formula guard = gItr.next();
			ArrayExpression<T> array = aItr.next();
			assert array.indices == first.indices;
			for(int i = 0; i < card; i++) { 
				phis[i].add(guard.thenElse(array.values[i], empty));
			}
		}
		
		final Expression[] phiVals = new Expression[card];
		for(int i = 0; i < card; i++) { 
			phiVals[i] = phis[i].isEmpty() ? empty : Expression.union(phis[i]);
		}
		
		return new ArrayExpression<T>(first, phiVals);
	}
	
	public String toString(){
		String res = prettyPrint(domain, 0);
		res += "[";
		for(int i = 0; i < indices.length; i++){
			res += prettyPrint(values[i],0);
		}
		res += "]";
		return res;
	}
}

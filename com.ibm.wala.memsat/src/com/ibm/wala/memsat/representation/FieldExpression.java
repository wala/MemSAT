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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.memsat.util.Nodes;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;

/**
 * Wraps a Kodkod expression that represents the contents of a given field.
 * 
 * 
 * @specfield isStatic: boolean // true if the field is static
 * @specfield refs: lone Expression // if non-static, expression representing the objects whose field value is represented by this field expression
 * @specfield field: Expression // expression representing the contents of the field
 * @author Emina Torlak
 */
public final class FieldExpression<T> extends HeapExpression<T> {
	private final Expression field;
	private final Expression domain;
	private final Interpreter<T> meaning;
	
	/**
	 * Constructs a new field from the given name, domain, and meaning function.
	 * @effects this.refs' = domain and (this.isStatic' iff domain = null) and
	 * this.field' = Relation.nary(name, meaning.zero.arity + (isStatic => 0 else 1)) 
	 */
	private FieldExpression(String name, Expression domain, Interpreter<T> meaning) { 
		this.field = Relation.nary(name, meaning.defaultObj().arity() + (domain==null ? 0 : 1));
		this.domain = domain;
		this.meaning = meaning;
	}
	
	/**
	 * Constructs a new field using the given template and new field value.
	 * @effects this.isStatic' = old.isStatic and 
	 * this.field' = field) }
	 */
	private FieldExpression(FieldExpression<T> old, Expression field) { 
		this.domain = old.domain;
		this.meaning = old.meaning;
		this.field = field;
	}

	/**
	 * Returns a field constructed from the given name, domain, and meaning function.
	 * @effects { f: FieldExpression<T> | f.refs' = refs and 
	 *   f.isStatic iff refs = null and
	 *   f.field = Relation.nary(name, valM.zero.arity + (isStatic => 0 else 1)) }
	 */
	static <T> FieldExpression<T> field(String name, Expression refs, Interpreter<T> valM) {
		return new FieldExpression<T>(name,refs,valM);
	}

	public String toString() {
	  return field.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.representation.HeapExpression#valueInterpreter()
	 */
	public final Interpreter<T> valueInterpreter() { return meaning; }
	
	/**
	 * Returns this field.
	 * @return this.field
	 */
	final Expression field() { return field; }
	
	/**
	 * Returns false.
	 * @return false
	 * @see com.ibm.wala.memsat.representation.HeapExpression#isArray()
	 */
	public final boolean isArray() { return false; }
	
	/**
	 * Returns the Expression encoding the set of instances whose field is 
	 * represented by this field expression.
	 * @return this.refs
	 */
	public final Expression instances() { return domain; }
	
	/**
	 * Returns true if this represents a static field.
	 * @return this.isStatic
	 */
	public final boolean isStatic() { return domain==null; }
		
	/**
	 * Returns the value of this field at the given location.
	 * If this field is static, the reference argument is ignored
	 * (it can be null).
	 * @requires ref.arity = 1
	 * @return { t: T | [[n]]= (this.isStatic => this.field else ref.join(this.field)) }
	 */
	public final T read(Expression ref) {
		return meaning.fromObj(isStatic() ? field : ref.join(field));
	}
	
	/**
	 * Returns a new field expression that reflects the state
	 * of the this field after the given value has been written
	 * to the given location.
	 * If this field is static, the reference argument is ignored
	 * (it can be null).
	 * @requires ref.arity = 1 
	 * @return { f: FieldExpression<N> | 
	 *  f.isStatic = this.isStatic and 
	 *  f.field = this.field++(this.isStatic => [[value]] else ref->[[value]]) }
	 */
	public final FieldExpression<T> write(Expression ref, T value) { 
		final Expression expr = meaning.toObj(value);
		final Expression overrider = isStatic() ? expr : ref.product(expr);  
		return new FieldExpression<T>(this, isStatic() ? overrider : 
												field.difference(ref.product(ref.join(field))).union(overrider));
	}
	
	/**
	 * Returns the value of this field expression at the specified reference, as given by the
	 * specified evaluator. The reference expression is ignored (it can be null) if 
	 * this is a static field.  The runtime type of the returned object is 
	 * determined by the type of values stored in this field.  In particular, if 
	 * this field stores RealExpressions, then the returned value will be 
	 * a Float; if this field stores IntExpressions, the returned value will be an Integer; 
	 * if this field stores Expressions, the returned value will be an Object; 
	 * finally, if this field stores Formulas, the returned value will be a Boolean.
	 * @requires  the given evaluator's instance has bindings
	 * for all relations (if any) at the leaves of this field expression.
	 * @requires eval.evaluate(ref) in eval.evaluate(this.refs)
	 * @return  value of this field expression at the specified reference, as given by the
	 * specified evaluator.
	 */
	@SuppressWarnings("unchecked")
	public final <V> V evaluate(Expression ref, Evaluator eval) { 
		final T value = isStatic() ? meaning.fromObj(field) : meaning.fromObj(ref.join(field));
		return (V) meaning.evaluate(value, eval);
	}
	
	/**
	 * Returns a field expression that evaluates to the ith value returned by the 
	 * fields' iterator iff the ith guard returned by the guards' iterator evaluates
	 * to true.  This method assumes that at most one of the given guards 
	 * evaluates to true.  If none do, the returned value will evaluate to 
	 * an empty field (i.e. a field that maps everything in its domain to 
	 * a T whose Expression value is the empty set).
	 * @requires guards and values contain the same (positive) number of objects
	 * @requires at most one of the guards can ever evaluate to true.
	 * @requires all field expressions in the given collection represent the same field
	 * @return a field expression that evaluates to the ith value returned by the 
	 * fields' iterator iff the ith guard returned by the guards' iterator evaluates
	 * to true. 
	 */
	static <T> FieldExpression<T> phi(Collection<Formula> guards, Collection<FieldExpression<T>> fields) { 
		assert guards.size() == fields.size();
		assert !guards.isEmpty();
		
		final List<Expression> phis = new ArrayList<Expression>(guards.size());
		final Iterator<Formula> gItr = guards.iterator();
		final Iterator<FieldExpression<T>> fItr = fields.iterator();
		
		final FieldExpression<T> first = fItr.next();
		final Expression empty = Nodes.empty(first.field.arity());
		
		phis.add(gItr.next().thenElse(first.field, empty));
		
		while(gItr.hasNext()) { 
			phis.add(gItr.next().thenElse(fItr.next().field, empty));
		}
		
		return new FieldExpression<T>(first, phis.isEmpty() ? empty : Expression.union(phis));
	}

}

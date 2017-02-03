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
import java.util.List;

import com.ibm.wala.memsat.frontEnd.IRType;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.engine.Evaluator;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;


/**
 * Wraps a Kodkod representation of a real number.
 *  
 * @author Emina Torlak
 */
public final class RealExpression  {
	private final IntExpression value;
	
	/**
	 * Constructs a new real expression for the given float constant.
	 * This implementation simply rounds the constant to the nearest
	 * integer and represents it using an IntExpression.  
	 */
	RealExpression(float real) { 
		value = IntConstant.constant((int)Math.round(real));
	}
	
	/**
	 * Constructs a new real expression from the given int expression.
	 */
	private RealExpression(IntExpression expr) { 
		this.value = expr;
	}
	

	/**
	 * Returns a number interpreter for real expressions, based on the given
	 * interpreter for the int expressions.
	 * @return a number interpreter for real expressions, based on the given
	 * interpreter for the int expressions
	 */
	static Interpreter<RealExpression> interpreter(final Interpreter<IntExpression> intInterpreter) { 
		return new Interpreter<RealExpression>() {
			final RealExpression zero = new RealExpression(intInterpreter.defaultValue());
		
			public IRType type() { return IRType.REAL; }
			public Expression toObj(RealExpression t) { return intInterpreter.toObj(t.value); }
			public RealExpression fromObj(Expression e) { return new RealExpression(intInterpreter.fromObj(e)); }
			public Expression defaultObj() { return intInterpreter.defaultObj(); }
			public RealExpression guardedValue(Formula guard, RealExpression value) {
				return fromIntExpr(intInterpreter.guardedValue(guard, value.value));
			}
			public RealExpression defaultValue() { return zero; }
			public Float evaluate(RealExpression value, Evaluator eval) { return value.evaluate(eval); }
			public boolean singletonEncoding() { return intInterpreter.singletonEncoding(); }
			RealExpression phi(Collection<? extends RealExpression> phis) {
				final List<IntExpression> ints = new ArrayList<IntExpression>(phis.size());
				for(RealExpression real : phis) { 
					ints.add(real.toIntExpr());
				}
				return fromIntExpr(intInterpreter.phi(ints));
			}
		};
	}
	
	/**
	 * Returns the set of all reals representable in the 
	 * context of the given bounds.
	 * @return set of all reals representable in the 
	 * context of the given bounds
	 */
	static TupleSet reals(Bounds b) { 
		final TupleSet reals = b.universe().factory().noneOf(1);
		for(TupleSet ibound : b.intBounds().values()) {
			reals.addAll(ibound);
		}
		return reals;
	}
	
	/**
	 * Returns the set of all reals representable with the
	 * given set of integers.
	 * @requires allInts.arity = 1
	 * @return set of all reals representable with the
	 * given set of integers.
	 */
	static TupleSet allReals(TupleSet allInts) { 
		return allInts;
	}

	/**
	 * Returns a real expression that represents a real number
	 * with the same value as the given intExpr.
	 * @return { r: RealExpression | [[r]] = [[intExpr]] }
	 */
	public static RealExpression fromIntExpr(IntExpression intExpr) { 
		assert intExpr != null;
		return new RealExpression(intExpr);
	}
	
	/**
	 * Returns a Formula that evaluates to true if this and 
	 * the given real expression represent the same number.
	 * @return { f: Fromula | [[f]] = ([[this]] = [[other]]) } 
	 */
	public final Formula eq(RealExpression other) {
		return value.eq(other.value);
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is greater than the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] > [[other]]) } 
	 */
	public final Formula gt(RealExpression other){
		return value.gt(other.value);
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is greater than or equal to the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] >= [[other]]) } 
	 */
	public final Formula gte(RealExpression other){
		return value.gte(other.value);
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is less than the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] < [[other]]) } 
	 */
	public final Formula lt(RealExpression other){
		return value.lt(other.value);
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is less than or equal to the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] <= [[other]]) } 
	 */
	public final Formula lte(RealExpression other){
		return value.lte(other.value);
	}
	
	/**
	 * Returns a real expression that evaluates to the sum
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] + [[other]] }
	 */
	public final RealExpression plus(RealExpression other){
		return new RealExpression(value.plus(other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the difference
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] - [[other]] }
	 */
	public final RealExpression minus(RealExpression other){
		return new RealExpression(value.minus(other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the product
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] * [[other]] }
	 */
	public final RealExpression multiply(RealExpression other){
		return new RealExpression(value.multiply(other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the ratio
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] \ [[other]] }
	 */
	public final RealExpression divide(RealExpression other){
		return new RealExpression(value.divide(other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to this modulo
	 * the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] % [[other]] }
	 */
	public final RealExpression modulo(RealExpression other){
		return new RealExpression(value.modulo(other.value));
	}
	
	/**
	 * Returns a real expression that represents the negation of this real expression.
	 * @return { r: RealExpression | [[r]] = -[[this]]  }
	 */
	public final RealExpression negate() { 
		return new RealExpression(value.negate());
	}
	
	/**
	 * Returns the int expression that represents the integer
	 * part of this real value.
	 * @return { i: IntExpression | [[i]] = (int) [[this]] }
	 */
	public final IntExpression toIntExpr() { 
		return value;
	}
	
	/**
	 * Returns the float value of this RealExpression,
	 * as given by the specified evaluator.  
	 * @requires the given evaluator's instance has bindings
	 * for all relations (if any) at the leaves of this real expression.
	 * @return [[ this ]] w.r.t. eval
	 */
	public final float evaluate(Evaluator eval) { 
		return eval.evaluate(value);
	}
	
	/**
	 * Returns the string representation of this real. 
	 * @return string representation of this real.
	 * @see java.lang.Object#toString()
	 */
	public String toString() { 
		return value.toString();
	}
}

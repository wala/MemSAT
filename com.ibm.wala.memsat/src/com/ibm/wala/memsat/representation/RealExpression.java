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
import com.ibm.wala.memsat.math.FloatingPoint;

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
		value = IntConstant.constant(Float.floatToIntBits(real));
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
			final RealExpression zero = new RealExpression(0f);
		
			@Override
      public IRType type() { return IRType.REAL; }
			@Override
      public Expression toObj(RealExpression t) { return intInterpreter.toObj(t.value); }
			@Override
      public RealExpression fromObj(Expression e) { return new RealExpression(intInterpreter.fromObj(e)); }
			@Override
      public Expression defaultObj() { return toObj(zero); }
			@Override
      public RealExpression guardedValue(Formula guard, RealExpression value) {
				return new RealExpression(intInterpreter.guardedValue(guard, value.value));
			}
			@Override
      public RealExpression defaultValue() { return zero; }
			@Override
      public Float evaluate(RealExpression value, Evaluator eval) { return value.evaluate(eval); }
			@Override
      public boolean singletonEncoding() { return intInterpreter.singletonEncoding(); }
			@Override
      RealExpression phi(Collection<? extends RealExpression> phis) {
				final List<IntExpression> ints = new ArrayList<IntExpression>(phis.size());
				if (phis.size() == 1) {
				  return phis.iterator().next();
				} else {
				  return new RealExpression(IntExpression.or(ints));
				}
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
		return new RealExpression(FloatingPoint.intToFloat(intExpr));
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
    return FloatingPoint.floatCompare(value, other.value).gt(IntConstant.constant(0));
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is greater than or equal to the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] >= [[other]]) } 
	 */
	public final Formula gte(RealExpression other){
    return FloatingPoint.floatCompare(value, other.value).gte(IntConstant.constant(0));
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is less than the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] < [[other]]) } 
	 */
	public final Formula lt(RealExpression other){
    return FloatingPoint.floatCompare(value, other.value).lt(IntConstant.constant(0));
	}
	
	/**
	 * Returns a Formula that evaluates to true if the value
	 * of this is less than or equal to the value of the given real expression.
	 * @return { f: Fromula | [[f]] = ([[this]] <= [[other]]) } 
	 */
	public final Formula lte(RealExpression other){
    return FloatingPoint.floatCompare(value, other.value).lte(IntConstant.constant(0));
	}
	
	/**
	 * Returns a real expression that evaluates to the sum
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] + [[other]] }
	 */
	public final RealExpression plus(RealExpression other){
		return new RealExpression(FloatingPoint.floatAdd(value, other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the difference
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] - [[other]] }
	 */
	public final RealExpression minus(RealExpression other){
    return new RealExpression(FloatingPoint.floatMinus(value, other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the product
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] * [[other]] }
	 */
	public final RealExpression multiply(RealExpression other){
    return new RealExpression(FloatingPoint.floatMultiply(value, other.value));
	}
	
	/**
	 * Returns a real expression that evaluates to the ratio
	 * of this and the given real expression.
	 * @return { r: RealExpression | [[r]] = [[this]] \ [[other]] }
	 */
	public final RealExpression divide(RealExpression other){
    return new RealExpression(FloatingPoint.floatDivide(value, other.value));
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
		return FloatingPoint.floatToInt(this);
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
	@Override
  public String toString() { 
		return value.toString();
	}
	
	public IntExpression intBits() {
	  return value;
	}
}

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

import java.util.Collection;

import com.ibm.wala.memsat.frontEnd.IRType;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.engine.Evaluator;

/**
 * Converts between various Kodkod representations of 
 * wala types.  All types can be converted to Expressions
 * (mimicking Java in that all primitives can be auto-boxed
 * into objects of appropriate type).
 * 
 * @specfield type: IRType // wala type represented by T
 * @specfield toObj:  T ->one Expression 
 * @specfield defaultValue: T // default value for the type represented by T; i.e.
 * 
 * @author Emina Torlak
 */
public abstract class Interpreter<T> {
	
	/**
	 * Returns this.type.
	 * @return this.type
	 */
	public abstract IRType type();
	
	/**
	 * Converts the given value to an expression.
	 * @return this.toObj[t]
	 */
	public abstract Expression toObj(T t);
	
	/**
	 * Converts the given expression to its equivalent of type T.
	 * @return this.toObj.e
	 */
	public abstract T fromObj(Expression e);
	
	/**
	 * Returns an instance of T that evaluates to the given value
	 * if the specified guard evaluates to true.  Otherwise, the
	 * returned instance evaluates to an instance of T whose 
	 * corresponding Expression is empty. 
	 * @return { t: T | [[t]] = ( [[guard]] => [[value]] else [[this.empty]] } 
	 */
	abstract T guardedValue(Formula guard, T value);

	/**
	 * Given a collection of guarded values of type T, returns their
	 * phi combination.  In particular, this method  assumes that 
	 * exactly one of the guards for the values in the given collection
	 * evaluates to true. The object returned by this method is equivalent 
	 * to the unique value in the collection whose guard is true.  If no 
	 * such value exists, or more than such value exists, the meaning of the
	 * returned value is undefined.  
	 * @requires !phis.isEmpty()
	 * @return a phi combination of the given collection of guarded values.
	 */
	abstract T phi(Collection<? extends T> phis);
	
	/**
	 * Returns the expression corresponding to this.defaultValue
	 * @return toExpr(this.defalutValue)
	 */
	public abstract Expression defaultObj();
	
	/**
	 * Returns the default value represented by T.
	 * @return this.defalutValue
	 */
	public abstract T defaultValue();
	
	/**
	 * Returns the meaning of the given value, as given
	 * by the specified evaluator.  The actual runtime
	 * type of the returned value is determined by this.type.
	 * Namely, if this.type is BOOLEAN, then the returned value
	 * is Boolean; if this.type is INTEGER, the returned value is 
	 * an Integer; if this.type is OBJECT, the returned value is 
	 * an Object; finally, if this.type is REAL, the returned value
	 * is a Float.
	 * @requires  the given evaluator's instance has bindings
	 * for all relations (if any) at the leaves of the given value
	 * @return  meaning of the given value, as given
	 * by the specified evaluator
	 */
	public abstract Object evaluate(T value, Evaluator eval);
	
	/**
	 * Returns true if a given value of this.type
	 * is encoded using a single atom; otherwise returns true.
	 * @return true if a given value of this.type
	 * is encoded using a single atom; otherwise returns true.
	 */
	public abstract boolean singletonEncoding();
	
}

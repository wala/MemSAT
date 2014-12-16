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
import java.util.List;

import kodkod.ast.Formula;

/**
 * A value, field, or array composed of one or more guarded values.
 * 
 * @specfield size: int
 * @specfield guards: [0..size) ->one Formula
 * @specfield phis: [0..size) ->one E
 * @invariant size >= 0
 * @invariant E in Node + HeapExpression + RealExpression
 * @author Emina Torlak
 */
public abstract class PhiExpression<E> {
	private PhiExpression() {}
	
	/**
	 * Returns the number of guard/value pairs in this phi expression.
	 * @return this.size
	 */
	public abstract int size();
	
	/**
	 * Adds the guard/value pair to this.phis.  This method
	 * assumes that the given may evaluate to true only  
	 * if all the guards seen so far evaluate to false.
	 * @requires value in HeapExpression and some this.phis =>
	 *   value.walaField = this.phis[int].walaField
	 * @effects this.size' = this.size + 1
	 * @effects this.phis' = this.phis + this.size -> value
	 * @effects this.guards' = this.guards + this.size -> guard
	 * @throws IllegalArgumentException - guard/value is not a valid
	 * entry for this phi expression
	 */
	public abstract void add(Formula guard, E value);

	/**
	 * Returns an instance of E that evaluates to the value in
	 * this.phis[int] whose guard evaluates to true.  If no
	 * guard is true, or more than one guard is true, the meaning
	 * of the returned value is undefined.
	 * @return an instance of E that evaluates to the value in
	 * this.phis[int] whose guard evaluates to true. 
	 * @throws IllegalStateException - this.size = 0
	 */
	public abstract E value();
	
	/**
	 * Returns an empty PhiExpression for values of type interpreter.type().
	 * @return an empty PhiExpression for values of type interpreter.type()
	 */
	static <E> PhiExpression<E> valuePhi(final Interpreter<E> interpreter) { 
		return new PhiExpression<E>() {
			private final List<E> phis = new ArrayList<E>();
			private E value;
			
			@Override
			public void add(Formula guard, E value) {
				phis.add(interpreter.guardedValue(guard, value));
				value = null;
			}
				
			@Override
			public E value() {
				if (value==null) { 
					if (phis.isEmpty()) 
						throw new IllegalStateException("cannot compute a phi with no values");
					value = interpreter.phi(phis);
				}
				return value;
			}

			@Override
			public int size() {
				return phis.size();
			}
		};
	}
	
	/**
	 * Returns a PhiExpression initialized with the given
	 * guard / heap value pair.  All subsequent calls to 
	 * {@linkplain #add(Formula, Object)} on the returned
	 * phi expression must be passed a HeapExpression with 
	 * the same <tt>walaField</tt> as the given heap expression.
	 * @return a PhiExpression initialized with the given
	 * guard / heap value pair
	 */
	static <E> PhiExpression<HeapExpression<E>> heapPhi(final Formula guard, final HeapExpression<E> expr) { 
		return new PhiExpression<HeapExpression<E>>() {
			private final List<Formula> guards = new ArrayList<Formula>();
			private final List<HeapExpression<E>> values = new ArrayList<HeapExpression<E>>();
			private HeapExpression<E> value;
			private final boolean isArray = expr.isArray();
			{ add(guard,expr); }
			
			@Override
			public void add(Formula guard, HeapExpression<E> value) {
				assert guard != null && value.isArray()==isArray;
				guards.add(guard);
				values.add(value);
				value = null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public HeapExpression<E> value() {
				if (value == null) { 
					if (isArray) { 
						value = ArrayExpression.phi(guards, (List<ArrayExpression<E>>)(List<?>)values);
					} else {
						value = FieldExpression.phi(guards, (List<FieldExpression<E>>)(List<?>)values);
					}
				}
				return value;
			}

			@Override
			public int size() {
				return guards.size();
			}
			
		};
	}
}

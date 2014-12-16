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

import kodkod.ast.Expression;



/**
 * Wraps a collection of Kodkod expressions that represent the contents of a given field or array.
 * 
 * @specfield walaField: AbstractFieldPointerKey // wala field represented by this heap expression
 * @specfield valueType: IRType // the right-hand ("points to") type of this heap expression
 * @specfield valueInterpreter: Interpreter<T> // used for converting values stored in this object to Expressions and vice versa
 * @invariant this.valueInterpeter.type = this.valueType
 * @author Emina Torlak
 */
public abstract class HeapExpression<T> {
	
	/**
	 * Returns the interpreter used by this heap expression
	 * for converting stored values to Kodkod expressions and vice versa.
	 * @return this.valueInterpreter
	 */
	public abstract Interpreter<T> valueInterpreter();
	
	/**
	 * Returns true if this heap expression represents
	 * the contents of an array.
	 * @return true if this heap expression represents
	 * the contents of an array.
	 */
	public abstract boolean isArray();
	
	/**
	 * Returns the Expression encoding this.walaField.getInstanceKey().
	 * @return Expression encoding this.walaField.getInstanceKey()
	 */
	public abstract Expression instances();
}

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
package com.ibm.wala.memsat.translation;

import com.ibm.wala.memsat.representation.ExpressionFactory;

import kodkod.ast.Formula;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;


/**
 * Stores the translation of a sequential or a concurrent program to various data structures.
 * 
 * @specfield factory: ExpressionFactory // expression factory used for translation
 * @specfield formula: Formula // the formula generated by the translator 
 * @specfield bounds: Bounds // bounds generated by the translator
 * @specfield context: T // translation context
 * 
 * @author Emina Torlak
 */
public interface Translation<T> {
	
	/**
	 * Returns the factory used for allocating relations to 
	 * unknown values (initial heap state, entry method arguments, etc.)
	 * and constants.
	 * @return this.factory
	 */
	public  ExpressionFactory factory() ;
	/**
	 * Returns this.bounds.
	 * @return this.bounds
	 */
	public  Bounds bounds() ;
	/**
	 * Returns the formula constraining the initial and final 
	 * states of this.method's heap.
	 * @return this.formula
	 */
	public  Formula formula() ;
	/**
	 * Returns the translation context for this.formula and this.bounds.  The context
	 * contains additional information about the results of the translation]
	 * process, and it depends on whether this translation object was generated
	 * by a concurrent or a sequential translator. 
	 * @return translation context for this method.
	 */
	public  T context() ;

	public Options getOptions();
}

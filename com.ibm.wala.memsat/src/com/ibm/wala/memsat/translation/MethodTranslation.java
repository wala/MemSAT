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
package com.ibm.wala.memsat.translation;

import java.util.Set;

import com.ibm.wala.memsat.translation.Environment.Frame;

import kodkod.ast.Expression;
import kodkod.ast.Formula;

/**
 * Result of translating a Wala method in a given environment.
 *
 * @specfield frame: Environment.Frame
 * @specfield entryGuard: Formula
 * @specfield code: frame.callInfo.cgNode.getIR().getInstructions[int]
 */
public interface MethodTranslation {
	/**
	 * Returns the warnings generated during translation.
	 * @return warnings generated during translation
	 */
	public abstract Set<TranslationWarning> warnings();
	
	/**
	 * Returns the  translations of the user-level assertions or assumptions found in this.code.
	 * @return  translations of the user-level assertions or assumptions found in this.code.
	 */
	public abstract Set<Formula> assertions();
	
	/**
	 * Returns the translations of the assumptions in this.code, due to loop unrolling.
	 * @return translations of the assumptions in this.code, due to loop unrolling.
	 */
	public abstract Set<Formula> assumptions();
	
	/**
	 * Returns the translation of the exception(s) that could be thrown by 
	 * this.code, if any.  If no exceptions can be thrown thrown, returns null.
	 * @return translation of the exception(s) that could be thrown by this.code, if any.
	 */
	public abstract Expression exceptionValue();
	
	/**
	 * Returns the translation of the value returned by 
	 * this.code, if any.  If no value is returned, returns null.
	 * @return translation of the value returned by this.code, if any
	 */
	public abstract <T> T returnValue();
	
	/**
	 * Returns the normal exit guard for this.code.
	 * @return normal exit guard for this.code
	 */
	public abstract Formula normalExitGuard();
	
	/**
	 * Returns the symbolic execution frame for this MethodTranslation.
	 * @return this.frame
	 */
	public abstract Frame frame();
}
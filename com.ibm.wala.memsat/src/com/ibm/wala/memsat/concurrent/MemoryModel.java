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
package com.ibm.wala.memsat.concurrent;

import java.util.Set;

import com.ibm.wala.types.MethodReference;


/**
 * A <code>MemoryModel</code> generates {@linkplain kodkod.ast.Formula constraints}
 * that describe an ordering over memory actions in a given concurrent execution.  
 * 
 * @specfield memInstructions: set MethodReference
 * 
 * @author Emina Torlak
 */
public interface MemoryModel {
	
	/**
	 * Returns the set of references to methods which, when called, can affect memory ordering.  
	 * The methods must be static and return no values. Their bodies, if any, are ignored 
	 * during translation.
	 * @return this.memInstructions
	 */
	public abstract Set<MethodReference> memoryInstructions();
	
	/**
	 * Returns true if this memory model uses speculative executions to justify a program; otherwise
	 * returns false.
	 * @return true if this memory model uses speculative executions to justify a program; otherwise
	 * returns false.
	 */
	public abstract boolean usesSpeculation();
	
	/**
	 * Returns an {@linkplain Justification} whose formula and bounds form a constraint solving problem
	 * that is satisfiable iff the returned execution is a legal execution of the given program with
	 * respect to this memory model.
	 * @return an {@linkplain Justification} whose formula and bounds form a constraint solving problem
	 * that is satisfiable iff the returned execution is a legal execution of the given program with
	 * respect to this memory model.
	 */
	public abstract Justification justify(Program prog);
	
}

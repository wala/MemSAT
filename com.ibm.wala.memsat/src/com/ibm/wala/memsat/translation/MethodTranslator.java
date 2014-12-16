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


import kodkod.ast.Formula;


/**
 * Translates a Wala CGNode specified by the top frame of a 
 * given environment and a memory instruction handler.
 * 
 * @author Emina Torlak
 */
interface MethodTranslator {

	/**
	 * Returns the result of translating the method specified by
	 * env.top.callInfo, with respect to the given entry guard and
	 * memory instruction handler, after popping off env.top.
	 * @effects env.pop()
	 * @return { r: Result | r.frame = env.top and r.entryGuard = entryGuard and 
	 *  r.code = env.top.callInfo.cgNode.getIR().getInstructions[int] }
	 */
	abstract MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler);
}

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

import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

/**
 * A translator for memory instructions.
 * 
 * @author Emina Torlak
 */
public interface MemoryInstructionHandler {
	/**
	 * Updates env.top.localEnv[inst.getDef()] with the result of translating
	 * the given get instruction.
	 * @requires inst in env.top.cfg().getInstructions()
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @effects updates env.top.localEnv[inst.getDef()] with the result of translating
	 * the given get instruction.
	 */
	public abstract void handleGet(int instIdx, SSAGetInstruction inst, Formula guard, Environment env);
	
	/**
	 * Updates env.top.heapEnv[inst.getDef()] with the result of translating
	 * the given put instruction, conditional on the given guard.
	 * @requires inst in env.top.cfg().getInstructions()
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @effects updates env.top.heapEnv[inst.getDef()] with the result of translating
	 * the given put instruction, conditional on the given guard.
	 */
	public abstract void handlePut(int instIdx, SSAPutInstruction inst, Formula guard, Environment env);
	
	/**
	 * Updates env.top.localEnv[inst.getDef()] with the result of translating
	 * the given array load instruction.
	 * @requires inst in env.top.cfg().getInstructions()
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @effects updates env.top.localEnv[inst.getDef()] with the result of translating
	 * the given array load instruction.
	 */
	public abstract void handleArrayLoad(int instIdx, SSAArrayLoadInstruction inst, Formula guard, Environment env);

	/**
	 * Updates env.top.heapEnv[inst.getDef()] with the result of translating
	 * the given array store instruction, conditional on the given guard.
	 * @requires inst in env.top.cfg().getInstructions()
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @effects updates env.top.heapEnv[inst.getDef()] with the result of translating
	 * the given array store instruction, conditional on the given guard.
	 */
	public abstract void handleArrayStore(int instIdx, SSAArrayStoreInstruction inst, Formula guard, Environment env);
	
	/**
	 * Translates the given monitor instruction in the given environment, conditional on the given guard.
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @effects translates the given monitor instruction in the given environment, conditional on the given guard.
	 */
	public abstract void handleMonitor(int instIdx, SSAMonitorInstruction inst, Formula guard, Environment env);

	/**
	 * Translates the given invoke instruction in the given environment, conditional on the given guard.
	 * @requires env.top.callInfo.cgNode.getIR().getInstructions()[instIdx] = inst
	 * @requires 
	 *   let target = env.factory.info.callGraph().getPossibleTargets(env.top().callInfo().cgNode(), inst.getCallSite()) |
	 *     one target and target.getMethod().getReference() in env.factory.options.memoryModel().memoryInstructions()
	 * @effects translates the given monitor instruction in the given environment, conditional on the given guard.
	 */
	public abstract void handleSpecialInvoke(int instIdx, SSAAbstractInvokeInstruction inst, Formula guard, Environment env);
}

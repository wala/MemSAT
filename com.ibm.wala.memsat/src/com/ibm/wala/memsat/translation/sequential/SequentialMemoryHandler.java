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
package com.ibm.wala.memsat.translation.sequential;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;

import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.PhiExpression;
import com.ibm.wala.memsat.translation.Environment;
import com.ibm.wala.memsat.translation.MemoryInstructionHandler;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.debug.Assertions;

/**
 * Handles memory accesses in a sequential translation.
 * @author etorlak
 */
final class SequentialMemoryHandler implements MemoryInstructionHandler {
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleArrayLoad(com.ibm.wala.ssa.SSAArrayLoadInstruction, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleArrayLoad(int instIdx, SSAArrayLoadInstruction inst, Formula guard, Environment env) {
		final int arrayUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
		final ArrayExpression<?> array = env.arrayUse(arrayUse);
		final Expression ref = env.refUse(inst.getArrayRef());
		final IntExpression idx = env.intUse(inst.getIndex());
		env.localDef(inst.getDef(), array.read(ref,idx));
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleArrayStore(com.ibm.wala.ssa.SSAArrayStoreInstruction, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleArrayStore(int instIdx, SSAArrayStoreInstruction inst, Formula guard, Environment env) {
		final int arrayUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
		final int arrayDef = env.top().callInfo().fieldSSA().getDef(inst, 0);
		final ArrayExpression<Object> array = env.arrayUse(arrayUse);
		final Expression ref = env.refUse(inst.getArrayRef());
		final IntExpression idx = env.intUse(inst.getIndex());
		final Object value = env.localUse(inst.getValue());
		env.heapDef(arrayDef, array.write(ref,idx,value));
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleGet(com.ibm.wala.ssa.SSAGetInstruction, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleGet(int instIdx, SSAGetInstruction inst, Formula guard, Environment env) {
		final int[] uses = env.top().callInfo().fieldSSA().getUses(inst);
		if (inst.isStatic()) { 
			assert uses.length==1;
			env.localDef(inst.getDef(), env.fieldUse(uses[0]).read(null));
		} else {
			final PhiExpression<Object> reads = env.factory().valuePhi(IRType.convert(inst.getDeclaredFieldType()));
			for(int use : uses) { 
				final FieldExpression<Object> field = env.fieldUse(use);
				final Expression ref = env.refUse(inst.getRef());			 
				reads.add( Formula.TRUE, field.read(ref)  );
			}
			env.localDef(inst.getDef(), reads.value());
		}
//		final int fieldUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
//		final FieldExpression<?> field = env.fieldUse(fieldUse);
//		final Expression ref = inst.isStatic() ? null : env.refUse(inst.getRef());
//		env.localDef(inst.getDef(), field.read(ref));
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handlePut(com.ibm.wala.ssa.SSAPutInstruction, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handlePut(int instIdx, SSAPutInstruction inst, Formula guard, Environment env) {
		int uses[] = env.top().callInfo().fieldSSA().getUses(inst);
		int defs[] = env.top().callInfo().fieldSSA().getDefs(inst);
		assert uses.length == defs.length;
		for(int i = 0; i < uses.length; i++) {
			final int fieldUse = env.top().callInfo().fieldSSA().getUse(inst, i);
			final int fieldDef = env.top().callInfo().fieldSSA().getDef(inst, i);
			final FieldExpression<Object> field = env.fieldUse(fieldUse);
			final Expression ref = inst.isStatic() ? null : env.refUse(inst.getRef());
			final Object value = env.localUse(inst.getVal());
			env.heapDef(fieldDef, field.write(ref, value));
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleMonitor(com.ibm.wala.ssa.SSAMonitorInstruction, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleMonitor(int instIdx, SSAMonitorInstruction inst, Formula guard, Environment env) {
		// does nothing in a sequential translation
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleSpecialInvoke(int, com.ibm.wala.ssa.SSAAbstractInvokeInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleSpecialInvoke(int instIdx,
			SSAAbstractInvokeInstruction inst, Formula guard, Environment env) {
		// should never be called
		Assertions.UNREACHABLE();
	}
}

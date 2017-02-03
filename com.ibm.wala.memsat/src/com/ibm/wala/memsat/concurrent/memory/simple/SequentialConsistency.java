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
package com.ibm.wala.memsat.concurrent.memory.simple;

import java.util.Collections;

import com.ibm.wala.memsat.concurrent.Program;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;

/**
 * Implements the sequential consistency specification from [1]
 * @see [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * @author etorlak
 */
public final class SequentialConsistency extends SimpleMemoryModel<Object> {

	/**
	 * Creates a new instance of the sequential consistency memory model.
	 */
	public SequentialConsistency() {}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#consistencyConstraints(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.simple.SimpleExecution)
	 */
	@Override
	protected Formula consistencyConstraints(Program prog, SimpleExecution<Object> exec) {
		final Expression acts = exec.actions(), ord = exec.ordering(prog.info().threads());
		return Formula.and(exec.programOrder(acts, ord), exec.serialization(acts, ord), exec.properLocking(ord));
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#execution(com.ibm.wala.memsat.concurrent.Program)
	 */
	@Override
	protected SimpleExecution<Object> execution(Program prog) { 
		final Relation ord = Relation.binary("ord");
		return new SimpleExecution<Object>(prog, Collections.singletonMap((Object)prog.info().threads(), ord)); 
	}
	
}

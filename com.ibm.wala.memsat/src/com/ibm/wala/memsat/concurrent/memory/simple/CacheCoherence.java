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

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_WRITE;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_WRITE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.memsat.util.Graphs;
import com.ibm.wala.memsat.util.Programs;

/**
 * Implements the cache coherence specification from [1]
 * @see [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * @author etorlak
 */
public final class CacheCoherence extends SimpleMemoryModel<InlinedInstruction> {

	/**
	 * Creates a new instance of the cache coherence memory model.
	 */
	public CacheCoherence() {}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#consistencyConstraints(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.simple.SimpleExecution)
	 */
	@Override
	protected Formula consistencyConstraints(Program prog, SimpleExecution<InlinedInstruction> exec) {
		final Collection<Formula> cc = new ArrayList<Formula>();
		final Expression acts = exec.actions();
		for(InlinedInstruction initWrite : exec.ordered()) { 
			final Expression varActs = exec.restrictVar(acts, exec.locationOf(exec.action(initWrite)));
			cc.add( exec.programOrder(varActs, exec.ordering(initWrite)) );
			cc.add( exec.serialization(varActs, exec.ordering(initWrite)) );
		}
		return Formula.and(cc);
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#execution(com.ibm.wala.memsat.concurrent.Program)
	 */
	@Override
	protected SimpleExecution<InlinedInstruction> execution(Program prog) {
		final Map<InlinedInstruction, Relation> ords = new LinkedHashMap<InlinedInstruction, Relation>();
		final CGNode root = Graphs.root(prog.info().threads());
		final Set<Action> writes = EnumSet.of(NORMAL_WRITE, VOLATILE_WRITE);
		for(InlinedInstruction initWrite : Programs.instructionsOfType(prog.info().concurrentInformation(root).actions(), writes)) { 
			ords.put(initWrite, Relation.binary("ord"+initWrite.instructionIndex()));
		}
		return new SimpleExecution<InlinedInstruction>(prog, ords); 
	}

}

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.util.Graphs;

/**
 * Implements the processor consistency specification from [1]
 * @see [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * @author etorlak
 */
public final class ProcessorConsistency extends SimpleMemoryModel<Object> {

	/**
	 * Creates a new instance of the processor consistency memory model.
	 */
	public ProcessorConsistency() { }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#consistencyConstraints(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.simple.SimpleExecution)
	 */
	@Override
	protected Formula consistencyConstraints(Program prog, SimpleExecution<Object> exec) {
		final Collection<Formula> cc = new ArrayList<Formula>();
		
		final WalaInformation info = prog.info();
		
		final Expression acts = exec.actions();
		final Expression ord = exec.ordering(info.threads());
		
		final Expression initThread = prog.threads(Collections.singleton(Graphs.root(info.threads())));
		final Expression initWrites = Expression.intersection(prog.actionsOf(initThread), prog.allOf(NORMAL_WRITE, VOLATILE_WRITE), acts);
		
		final Variable w = Variable.unary("w");
		cc.add( exec.weakTotalOrder(exec.restrictVarWr(acts, exec.locationOf(w)), ord).forAll(w.oneOf(initWrites)) );
		
		for(CGNode proc : info.threads()) { 
			final Expression procActs = exec.restrictProc(acts, prog.threads(Collections.singleton(proc)));
			final Expression procOrd = exec.ordering(proc);
			cc.add( exec.programOrder(procActs, procOrd) );
			cc.add( exec.serialization(procActs, procOrd) );
			cc.add( exec.mapConstraints(acts, ord, procActs, procOrd) );
		}
		
		return Formula.and(cc);
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#execution(com.ibm.wala.memsat.concurrent.Program)
	 */
	@Override
	protected SimpleExecution<Object> execution(Program prog) {
		final Map<Object, Relation> ords = new LinkedHashMap<Object, Relation>();
		ords.put(prog.info().threads(), Relation.binary("ord"));
		for(CGNode root : prog.info().threads()) { 
			ords.put(root, Relation.binary("ord"+root.getMethod().getName()));
		}
		return new SimpleExecution<Object>(prog, ords); 
	}

}

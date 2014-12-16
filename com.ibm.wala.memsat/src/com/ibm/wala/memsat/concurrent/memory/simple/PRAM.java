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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Program;

/**
 * Implements the PRAM specification from [1]
 * @see [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * @author etorlak
 */
public final class PRAM extends SimpleMemoryModel<CGNode> {
	
	/**
	 * Creates a new instance of the PRAM memory model.
	 */
	public PRAM() { }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#consistencyConstraints(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.simple.SimpleExecution)
	 */
	@Override
	protected Formula consistencyConstraints(Program prog, SimpleExecution<CGNode> exec) {
		final Collection<Formula> cc = new ArrayList<Formula>();
		final Expression acts = exec.actions();
		for(CGNode proc : exec.ordered()) { 
			final Expression procActs = exec.restrictProc(acts, prog.threads(Collections.singleton(proc)));
			cc.add( exec.programOrder(procActs, exec.ordering(proc)) );
			cc.add( exec.serialization(procActs, exec.ordering(proc)) );
		}
		return Formula.and(cc);
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.memory.simple.SimpleMemoryModel#execution(com.ibm.wala.memsat.concurrent.Program)
	 */
	@Override
	protected SimpleExecution<CGNode> execution(Program prog) {
		final Map<CGNode, Relation> ords = new LinkedHashMap<CGNode, Relation>();
		for(CGNode root : prog.info().threads()) { 
			ords.put(root, Relation.binary("ord"+root.getMethod().getName()));
		}
		return new SimpleExecution<CGNode>(prog, ords); 
	}

}

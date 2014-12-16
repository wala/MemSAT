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

import static com.ibm.wala.memsat.util.Programs.executionOrder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;

import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Justification;
import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.concurrent.Program.BoundsBuilder;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.util.Programs;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

/**
 * A base implementation of the {@linkplain MemoryModel} interface,
 * suitable for the specification of simple (non-hybrid) memory models 
 * defined by Yang et al [1].
 * 
 * @see  [1] Y.�Yang, G.�Gopalakrishnan, G.�Lindstrom, and K.�Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS �04, pages 26�30, 2004.
 * 
 * @specfield memInstructions: set MethodReference
 * 
 * @author etorlak
 */
abstract class SimpleMemoryModel<T> implements MemoryModel {
	private final Set<MethodReference> memInsts;

	/**
	 * Constructs an AbstractMemoryModel with no special memory instructions.
	 * @effects this.memInstructions' = memInsts
	 */
	protected SimpleMemoryModel() { memInsts = Collections.emptySet(); }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.MemoryModel#memoryInstructions()
	 */
	public final Set<MethodReference> memoryInstructions() { return memInsts; }

	/**
	 * Returns false.
	 * @return false
	 */
	public final boolean usesSpeculation() { return false; }
	
	/**
	 * Returns the main execution that will be used in the justification of the given program.
	 * @return main execution that will be used in the justification of the given program.
	 */
	protected abstract SimpleExecution<T> execution(Program prog);
	
	/**
	 * Returns the consistency constraints for the given execution of the given program.
	 * @requires exec.prog = prog
	 * @return consistency constraints for the given execution of the given program.
	 */
	protected abstract Formula consistencyConstraints(Program prog, SimpleExecution<T> exec);
	
	/**
	 * Returns the bounds for the relations used in the specification of the given program
	 * and execution. The default implementation returns a bounds built by applying a 
	 * {@linkplain Program#builder() prog.builder} to exec and to each ordering in exec.  
	 * The upper bound on the orderings is given by {@linkplain Programs#executionOrder(com.ibm.wala.memsat.frontEnd.WalaInformation) 
	 * executionOrder(prog.info)}.
	 * @requires exec.prog = prog
	 * @return bounds for the given execution of the given program.
	 */
	protected Bounds bounds(Program prog, SimpleExecution<T> exec) {
		final BoundsBuilder builder = prog.builder();
		builder.boundExecution(exec);
		final Graph<InlinedInstruction> execOrd = executionOrder(prog.info());
		for(Relation ord : exec.orderings()) {
			builder.boundOrdering(ord, execOrd);
		}
		return builder.build();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.MemoryModel#justify(com.ibm.wala.memsat.concurrent.Program)
	 */
	public final Justification justify(final Program prog) {
		final SimpleExecution<T> exec = execution(prog);
		final Formula formula = prog.sequentiallyValid(exec).and(consistencyConstraints(prog, exec));
		final Bounds bounds = bounds(prog, exec);
		return new Justification() {
			public Execution execution() { return exec; }
			public Formula formula() { return formula; }
			public Bounds bounds() { return bounds; }
			public Program program() { return prog; }
			public List<? extends Execution> speculations() { return Collections.emptyList(); }
			public List<? extends Expression> commits() { return Collections.emptyList(); }
			
		};
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() { return getClass().getSimpleName(); }

}

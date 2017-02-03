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
package com.ibm.wala.memsat.translation.concurrent;

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.LOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_WRITE;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.UNLOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_WRITE;
import static com.ibm.wala.memsat.util.Programs.instructions;
import static com.ibm.wala.memsat.util.Programs.instructionsOfType;
import static com.ibm.wala.memsat.util.Programs.visibleWrites;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Program.BoundsBuilder;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.util.graph.Graph;

import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

/**
 * Implementation of the {@linkplain BoundsBuilder} interface based on the
 * Miniatur translation.
 * @author etorlak
 */
final class ConcurrentBoundsBuilder implements BoundsBuilder {
	private final Bounds bounds;
	private final TupleFactory tuples;
	private final ConcurrentFactory factory;
	private final Set<InlinedInstruction> all, writes, memoryAccesses, monitorAccesses;
	private final Graph<InlinedInstruction> visibleWrites;
	/**
	 * Constructs a new bounds builder with the given expression factory, initial bounds, and 
	 * action factory.
	 */
	public ConcurrentBoundsBuilder(Bounds bounds, ConcurrentFactory acts) {
		this.bounds = bounds;
		this.tuples = bounds.universe().factory();
		this.factory = acts;
		
		final WalaInformation info = acts.base().info();
		this.visibleWrites = visibleWrites(info);
		this.all = instructions(info);
		this.writes = instructionsOfType(all, EnumSet.of(NORMAL_WRITE,VOLATILE_WRITE));
		this.memoryAccesses = instructionsOfType(all, EnumSet.of(NORMAL_WRITE,VOLATILE_WRITE,NORMAL_READ,VOLATILE_READ));
		this.monitorAccesses = instructionsOfType(all, EnumSet.of(LOCK, UNLOCK));
	}
	

	/**
	 * Returns the upper bound on the actions that can be performed by the given instructions.
	 * @return bounds.upperBound(instructions.get(inst))
	 */
	private TupleSet actionAtoms(InlinedInstruction inst) { return factory.actionAtoms(tuples, inst); }
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program.BoundsBuilder#boundExecution(com.ibm.wala.memsat.concurrent.Execution)
	 */
	public void boundExecution(Execution exec) {
		for(InlinedInstruction inst : all) { 
			bounds.bound(exec.action(inst), actionAtoms(inst));
		}
		boundValues(exec);
		boundLocations(exec);
		boundMonitors(exec);
		boundOrdering(exec.w(), visibleWrites);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program.BoundsBuilder#boundActions(kodkod.ast.Relation, java.util.Set)
	 */
	public void boundActions(Relation r, Set<InlinedInstruction> insts) {
		final TupleSet u = tuples.noneOf(1);
		for(InlinedInstruction inst : insts) { 
			u.addAll( actionAtoms(inst) );
		}
		bounds.bound(r,u);
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program.BoundsBuilder#boundOrdering(kodkod.ast.Relation, com.ibm.wala.util.graph.Graph)
	 */
	public void boundOrdering(Relation r, Graph<InlinedInstruction> insts) {
		final TupleSet u = tuples.noneOf(2);
		for(InlinedInstruction inst : insts) { 
			for(Iterator<? extends InlinedInstruction> succs = insts.getSuccNodes(inst); succs.hasNext(); ) { 
				final InlinedInstruction succ = succs.next();
				u.addAll( actionAtoms(inst).product(actionAtoms(succ)) );
			}
		}
		bounds.bound(r,u);
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.concurrent.Program.BoundsBuilder#build()
	 */
	public Bounds build() { return bounds; }
	
	/**
	 * Adds bounds for exec.v to this.bounds.
	 * @effects this.bounds.relations' = this.bounds.relations + exec.v
	 */
	private void boundValues(Execution exec) {
		final TupleSet u = tuples.noneOf(2);
		for(InlinedInstruction inst : writes) { 
			u.addAll( actionAtoms(inst).product(factory.valueAtoms(tuples, inst)) );
		}
		bounds.bound(exec.v(),u);
	}

	/**
	 * Adds bounds for exec.location to this.bounds.
	 * @effects this.bounds.relations' = this.bounds.relations + exec.location
	 */
	private void boundLocations(Execution exec) {
		final TupleSet l = tuples.noneOf(2), u = tuples.noneOf(2);

		for(InlinedInstruction inst : memoryAccesses) { 
			final TupleSet instAtoms = actionAtoms(inst);
			final TupleSet locAtoms = factory.locationAtoms(tuples, inst);
			u.addAll( instAtoms.product(locAtoms) );
			if (inst.instruction() instanceof SSAFieldAccessInstruction) {
				if (locAtoms.size()==1 || (locAtoms.size()==2 && instAtoms.size()==1)) // statically known location, bound exactly
					l.addAll( instAtoms.product(locAtoms) );
			}
		}
		bounds.bound(exec.location(), l, u);
	}

	/**
	 * Adds bounds for exec.monitor to this.bounds.
	 * @effects this.bounds.relations' = this.bounds.relations + exec.monitor
	 */
	private void boundMonitors(Execution exec) {
		final TupleSet l = tuples.noneOf(2), u = tuples.noneOf(2);

		for(InlinedInstruction inst : monitorAccesses) { 
			final TupleSet instAtoms = actionAtoms(inst);
			final TupleSet monitorAtoms = factory.monitorAtoms(tuples, inst);
			u.addAll( instAtoms.product(monitorAtoms) );
			if (instAtoms.size()==1 && monitorAtoms.size()==1) { // statically known monitor, bound exactly
				l.addAll( instAtoms.product(monitorAtoms) );
			}
		}
		bounds.bound(exec.monitor(), l, u);
	}



}

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
package com.ibm.wala.memsat.translation.concurrent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.util.collections.CacheSet;
import kodkod.util.collections.Stack;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.Interpreter;
import com.ibm.wala.memsat.translation.Environment;
import com.ibm.wala.memsat.translation.MemoryInstructionHandler;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

/**
 * Handles memory acesses in a concurrent translation.
 * @author Emina Torlak
 */
final class ConcurrentMemoryHandler implements MemoryInstructionHandler {
	
	final ConcurrentFactory factory;
	
	private final Map<InlinedInstruction, Expression> locations;
	private final Map<InlinedInstruction, Expression> monitors;
	
	private final CacheSet<InlinedInstruction> insts;
	private final Map<InlinedInstruction, Formula> guards;
	private final Map<InlinedInstruction, Relation> reads;
	private final Map<InlinedInstruction, Expression> writes;
	
	/**
	 * Constructs a new concurrent memory handler for the given info and 
	 * options, set to translate the thread with id 0.
	 * @effects this.info' = info and this.options' = options and this.thread' = 0 and 
	 * this.factory' = new ExpressionFactory(info, options) no this.actions'
	 * and no this.guards'
	 */
	public ConcurrentMemoryHandler(WalaInformation info, Options options) {
		this.factory = new ConcurrentFactory(new ExpressionFactory(info, options));
		
		this.insts = new CacheSet<InlinedInstruction>();
		this.guards = new LinkedHashMap<InlinedInstruction, Formula>();
		for(CGNode thread : info.threads()) { 
			final WalaConcurrentInformation tInfo = info.concurrentInformation(thread);
			insts.addAll( tInfo.actions() );
			guards.put( tInfo.start(), Formula.TRUE);
			guards.put( tInfo.end(), Formula.TRUE);
		}	
		this.locations = new LinkedHashMap<InlinedInstruction, Expression>();
		this.monitors = new LinkedHashMap<InlinedInstruction, Expression>();
		this.reads = new LinkedHashMap<InlinedInstruction, Relation>();
		this.writes = new LinkedHashMap<InlinedInstruction, Expression>();
	}
	
	/**
	 * Returns the guard for the execution of the given instruction, as given to this 
	 * memory handler during translation.
	 * @return guard for the execution of the given instruction
	 */
	Formula guardFor(InlinedInstruction inst) { 
		return guards.get(inst);
	}
	
	/**
	 * Returns an expression that describes the location that is read/written by the given 
	 * instruction, or null if the instruction is not a read or a write. 
	 * The read/write location for a static field is the empty set; the location for
	 * a member field is the instance containing the accessed field; and the location for an array access
	 * is the union of the accessed array instance and the accessed index.
	 * @return an expression that describes the location that is read/written by the given 
	 * instruction, or null if the instruction is not a read or a write. 
	 */
	Expression locationOf(InlinedInstruction inst) { 
		return locations.get(inst);
	}
	
	/**
	 * Returns an expression that evaluates to the object that is locked or unlocked by the given 
	 * instruction, or null if the instruction is not a monitor access. 
	 * @return an expression that evaluates to the object that is locked or unlocked by the given 
	 * instruction, or null if the instruction is not a monitor access. 
	 */
	Expression monitorOf(InlinedInstruction inst) { 
		return monitors.get(inst);
	}
	
	/**
	 * Returns an expression that evaluates to the object that is written by the given 
	 * instruction, or null if the instruction is not a write. 
	 * @return an expression that evaluates to the object that is written by the given 
	 * instruction, or null if the instruction is not a write. 
	 */
	Expression valueWritten(InlinedInstruction inst) { 
		return writes.get(inst);
	}
	
	/**
	 * Returns a map from the translated read instructions to free variables that represent the values
	 * read by those instructions. 
	 * @return a map from the translated read instructions to free variables that represent the values
	 * read by those instructions. 
	 */
	Map<InlinedInstruction,Relation> valuesRead() { 
		return reads;
	}

	/**
	 * Returns an action corresponding to the given
	 * instruction index in the given environment, if one exists in
	 * any of the threads in this.info.  Otherwise returns null.
	 * @return { inst: InlinedInstruction | inst.instructionIndex() = instIdx &&
	 *           inst.cgNode = env.top.callInfo.cgNode && 
	 *           inst.callStack = env.callStack() }
	 */
	private InlinedInstruction action(int instIdx, Environment env) { 
		final Stack<CallSite> callStack = env.callStack();
		final CGNode cgNode = env.top().callInfo().cgNode();
		final int hash = instIdx + callStack.hashCode() + cgNode.hashCode();
		
//		System.out.println("-----------------------");
//		System.out.println("HASH: " + hash);
//		System.out.println("cgNode: " + cgNode);
//		System.out.println("callstack: [");
//		for(CallSite n : callStack) { 
//			System.out.println(" (" + n.getNode() + " ; " + n.getSite() + "),");
//		}
//		System.out.println("]");
//		System.out.println("inst index: " + instIdx);
		
		for(Iterator<InlinedInstruction> itr = insts.get(hash); itr.hasNext(); ) { 
			InlinedInstruction inst = itr.next();
			if (inst.instructionIndex()==instIdx && 
				inst.cgNode().equals(cgNode) &&
				inst.callStack().equals(callStack)) {
//				System.out.println("RETURNING INST: " + inst);
				return inst;
			}
		}
//		System.out.println("RETURNING NULL!");
		return null;
	}
	
	/**
	 * Looks up the value read by the given field or array read instruction.
	 * @requires inst.instruction in SSAGetInstruction + SSAArrayLoadInstruction
	 * @return value read by the given field or array read instruction
	 */
	private Object value(InlinedInstruction inst) { 
		final WalaCGNodeInformation nodeInfo = factory.base().info().cgNodeInformation(inst.cgNode());
		final Interpreter<?> interpreter = factory.base().constants().interpreter(nodeInfo.typeOf(inst.instruction().getDef()));
		Relation val = reads.get(inst);		
		if (val==null) {			
			val = Relation.nary("val@"+inst.instruction(), interpreter.defaultObj().arity());
			reads.put(inst, val);
		}
		return interpreter.fromObj(val);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleArrayLoad(int, com.ibm.wala.ssa.SSAArrayLoadInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleArrayLoad(int instIdx, SSAArrayLoadInstruction inst, Formula guard, Environment env) {
		final Expression ref = env.refUse(inst.getArrayRef());
		final IntExpression idx = env.intUse(inst.getIndex());
		
		final InlinedInstruction action = action(instIdx, env);
		
		if (action==null) {
			final int arrayUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
			final ArrayExpression<?> array = env.arrayUse(arrayUse);
			env.localDef(inst.getDef(), array.read(ref,idx));
		} else {
			env.localDef(inst.getDef(), value(action));
			guards.put(action, guard);
			locations.put(action, ref.union(factory.base().constants().intInterpreter().toObj(idx)));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleArrayStore(int, com.ibm.wala.ssa.SSAArrayStoreInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleArrayStore(int instIdx, SSAArrayStoreInstruction inst, Formula guard, Environment env) {
		final int arrayUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
		final int arrayDef = env.top().callInfo().fieldSSA().getDef(inst, 0);
		final ArrayExpression<Object> array = env.arrayUse(arrayUse);
		final Expression ref = env.refUse(inst.getArrayRef());
		final IntExpression idx = env.intUse(inst.getIndex());
		final Object value = env.localUse(inst.getValue());
		env.heapDef(arrayDef, array.write(ref,idx,value));
		
		final InlinedInstruction action = action(instIdx, env);
		if (action != null) { 
			guards.put(action, guard);
			locations.put(action, ref.union(array.indexInterpreter().toObj(idx)));
			writes.put(action, array.valueInterpreter().toObj(value));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleGet(int, com.ibm.wala.ssa.SSAGetInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleGet(int instIdx, SSAGetInstruction inst, Formula guard, Environment env) {
		
		final Expression ref = inst.isStatic() ? null : env.refUse(inst.getRef());	
		final InlinedInstruction action = action(instIdx, env);
		
		if (action==null) {
			final int fieldUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
			final FieldExpression<?> field = env.fieldUse(fieldUse);
			env.localDef(inst.getDef(), field.read(ref));
		} else {
			env.localDef(inst.getDef(), value(action));
			guards.put(action, guard);
			locations.put(action, ref==null ? factory.fieldOf(action) : ref.union(factory.fieldOf(action)));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handlePut(int, com.ibm.wala.ssa.SSAPutInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handlePut(int instIdx, SSAPutInstruction inst, Formula guard, Environment env) {
		final int fieldUse = env.top().callInfo().fieldSSA().getUse(inst, 0);
		final int fieldDef = env.top().callInfo().fieldSSA().getDef(inst, 0);
		final FieldExpression<Object> field = env.fieldUse(fieldUse);
		final Expression ref = inst.isStatic() ? null : env.refUse(inst.getRef());
		final Object value = env.localUse(inst.getVal());
		env.heapDef(fieldDef, field.write(ref, value));
		
		final InlinedInstruction action = action(instIdx, env);
		if (action != null) { 
			guards.put(action, guard);
			locations.put(action, ref==null ? factory.fieldOf(action) : ref.union(factory.fieldOf(action)));
			writes.put(action, field.valueInterpreter().toObj(value));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleMonitor(int, com.ibm.wala.ssa.SSAMonitorInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleMonitor(int instIdx, SSAMonitorInstruction inst, Formula guard, Environment env) {
		final InlinedInstruction action = action(instIdx, env);
		assert (action!=null); 
		guards.put(action, guard);
		monitors.put(action, env.refUse(inst.getRef()));
	}

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MemoryInstructionHandler#handleSpecialInvoke(int, com.ibm.wala.ssa.SSAAbstractInvokeInstruction, kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment)
	 */
	public void handleSpecialInvoke(int instIdx,
			SSAAbstractInvokeInstruction inst, Formula guard, Environment env) {
		final InlinedInstruction action = action(instIdx, env);
		assert (action!=null); 
		guards.put(action, guard);
	}
	
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append("INSTRUCTIONS: \n");
		for(InlinedInstruction inst : insts) { 
			s.append("[[" + System.identityHashCode(inst) + ", " + inst.hashCode() + "]] " + inst + "\n");
		}
		s.append("GUARDS: \n");
		for(Map.Entry<InlinedInstruction, Formula> entry : guards.entrySet()) { 
			s.append("[[" + System.identityHashCode(entry.getKey()) + "]] ");
			s.append(entry.getKey() + " GUARDED BY " + entry.getValue());
			s.append("\n");
		}
		s.append("READS: \n");
		for(Map.Entry<InlinedInstruction, Relation> entry : reads.entrySet()) { 
			s.append("[[" + System.identityHashCode(entry.getKey()) + "]] ");
			s.append(entry.getKey() + " REPRESENTED BY " + entry.getValue());
			s.append("\n");
		}
		return s.toString();
	}
}
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

import static com.ibm.wala.memsat.util.Graphs.root;
import static com.ibm.wala.memsat.util.Nodes.tupleset;
import static com.ibm.wala.memsat.util.Programs.instructions;
import static com.ibm.wala.memsat.util.Programs.referencedArray;
import static com.ibm.wala.memsat.util.Programs.referencedFields;
import static com.ibm.wala.memsat.util.Programs.referencedInstance;
import static com.ibm.wala.memsat.util.Programs.referencedMonitor;
import static com.ibm.wala.memsat.util.Programs.thread;
import static com.ibm.wala.memsat.util.Programs.valueFor;
import static com.ibm.wala.memsat.util.Strings.fieldNames;
import static com.ibm.wala.memsat.util.Strings.instructionNames;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.util.collections.Containers;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.memsat.representation.ConstantFactory;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * A factory for generating atoms that represent actions that may be
 * performed by a given concurrent program and for generating
 * atoms and expressions corresponding to locations / monitors that may be 
 * read / written / locked / unlocked by that program.
 * 
 * @specfield base: ExpressionFactory
 * @specfield insts: set InlinedInstruction
 * @specfield fields: insts ->lone IField
 * @specfield exprs: (Action + insts.fields) ->one Relation
 * @specfield threads, thread: Expression // expressions representing all threads in base.info and the mapping from actions to those threads
 * @invariant insts = { inst: InlinedInstruction | some n: nodes(base.info.threads()) | base.info.concurrentInformation(n).actions().contains(inst) }
 * @invariant fields = { i: insts, f: IField | i.instruction instanceof SSAFieldAccessInstruction && f = base.info.callGraph().getClassHierarchy().resolveField( i.getDeclaredField() ) }
 * @invariant base.info.threads.getNumberOfNodes() > 1
 * @author etorlak
 */
final class ConcurrentFactory {
	private final ExpressionFactory base;
	private final Map<InlinedInstruction, Set<?>> instAtoms;
	private final Map<InlinedInstruction, IField> instFields;
	private final Map<IField, Relation> fieldExprs;
	private final Map<Action, Relation> actExprs;
	
	private static final boolean DEBUG = true;
	
	/**
	 * Constructs a new action factory using the given base expression factory.
	 * @requires base.options.memoryModel != null
	 * @effects this.base' = base
	 */
	public ConcurrentFactory(ExpressionFactory base) {
		this.base = base;

		this.actExprs = new EnumMap<Action, Relation>(Action.class);
		for(Action a : Action.values()) { 
			this.actExprs.put(a, Relation.unary(a.toString()));
		}
		
		this.instAtoms =  new LinkedHashMap<InlinedInstruction, Set<?>>();
		if (base.options().memoryModel().usesSpeculation()) {
			initSpeculativeAtoms();
		} else {
			initNonSpeculativeAtoms();
		}	
		
//		System.out.println("INSTRUCTION ATOMS: ");
//		
//		for(InlinedInstruction inst : instAtoms.keySet()) { 
//			System.out.println("[[" + System.identityHashCode(inst) + ", " + inst.hashCode() + "]] " + inst + " = " + instAtoms.get(inst));
//		}
		
		this.instFields = referencedFields(base.info());
		this.fieldExprs = new LinkedHashMap<IField, Relation>();
		for(Map.Entry<IField, String> named : fieldNames(new LinkedHashSet<IField>(instFields.values())).entrySet()) { 
			fieldExprs.put( named.getKey(), Relation.unary(named.getValue()) );
		}
	}
	
	/** Initializes the mapping from instructions to sets of atoms for a non-speculative memory model. */
	private void initNonSpeculativeAtoms() {
		for(Map.Entry<InlinedInstruction,String> named : instructionNames(instructions(base.info())).entrySet()) { 
			instAtoms.put(named.getKey(), Collections.singleton(new ActionAtom(named.getValue())));
		}
	}
	
	/** Initializes the mapping from instructions to sets of atoms for a speculative memory model. */
	private void initSpeculativeAtoms() {
		final WalaInformation info = base.info();
	
		final Graph<CGNode> threads = info.threads();
		
		if (DEBUG) System.out.println(threads);
		
		int atomIdx = 0;

		for(Iterator<CGNode> tItr = DFS.iterateDiscoverTime(threads, root(threads)); tItr.hasNext(); ) { 
			final CGNode thread = tItr.next();
			final WalaConcurrentInformation tInfo = info.concurrentInformation(thread);
			final Map<InlinedInstruction, Effects> effects = effects(tInfo);
			final Map<Effects, Set<InlinedInstruction>> maxEffects = maxEffects(tInfo, effects);
			
			if (DEBUG) print(tInfo, effects, maxEffects);
		
			// allocate atoms in depth-first order to get a nice numbering
			final Map<InlinedInstruction, ActionAtom> atoms = new LinkedHashMap<InlinedInstruction, ActionAtom>();
			final Graph<InlinedInstruction> to = tInfo.threadOrder();
			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(to, tInfo.start()); insts.hasNext(); ) { 
				final InlinedInstruction inst = insts.next();
				if (maxEffects.get(effects.get(inst)).contains(inst)) { 
					atoms.put(inst, new ActionAtom(atomIdx++));
				}
			}
			
		
			// compute the upper bound B on the actions for each instruction i as follows:
			// for each instruction r such that atoms.containsKey(r) and effects.get(i).overlaps(effects.get(r)),
			// add atoms.get(r) to the upper bound B of i
			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(to, tInfo.start()); insts.hasNext(); ) { 
				final InlinedInstruction inst = insts.next();
				final Effects instEffects = effects.get(inst);
				final Set<ActionAtom> upper = new LinkedHashSet<ActionAtom>(4);
				//if (atoms.containsKey(inst)) { upper.add(atoms.get(inst)); }
				for(InlinedInstruction rep : atoms.keySet()) { 
					if (instEffects.overlaps(effects.get(rep)))
						upper.add(atoms.get(rep));
				}
				
				assert !upper.isEmpty();
				instAtoms.put(inst, upper);
			}
			
		}
		for(Map.Entry<InlinedInstruction, Set<?>> entry : instAtoms.entrySet()) { 
			System.out.println(entry);
		}
//		throw new AssertionError();
	}
	
	/** @effects prints debug info to standard out */
	private void print(WalaConcurrentInformation tInfo, Map<InlinedInstruction, Effects> effects, Map<Effects, Set<InlinedInstruction>> maxEffects) { 
		System.out.println("\n*****WALA REPRESENTATION OF " + tInfo.root().getMethod().getSignature() + "*****");
		System.out.println(tInfo);
		final Map<Effects,Set<InlinedInstruction>> equivalences = new LinkedHashMap<Effects, Set<InlinedInstruction>>();
		for(Map.Entry<InlinedInstruction, Effects> entry : effects.entrySet()) { 
			Set<InlinedInstruction> insts = equivalences.get(entry.getValue());
			if (insts==null) { 
				insts = new LinkedHashSet<InlinedInstruction>();
				equivalences.put(entry.getValue(), insts);
			}
			insts.add(entry.getKey());
		}
		System.out.println("\n*****EQUIVALENCE CLASSES OF " + tInfo.root().getMethod().getSignature() + "*****");
		for(Map.Entry<Effects, Set<InlinedInstruction>> eq : equivalences.entrySet()) { 
			System.out.println(" CLASS =");
			System.out.println("    members: " + eq.getValue());
			System.out.println("    maximal path: " + maxEffects.get(eq.getKey()));
		}
		
	}
	
	/**
	 * Returns a map from each instruction in tInfo.actions to an Effects object that represents 
	 * the effects that it may have when executed.
	 * @requires some n: this.base.info.thread.nodes | this.base.info.concurrentInformation(n) = tInfo
	 * @return a map from each instruction in tInfo.actions to an Effects object that represents 
	 * the effects that it may have when executed.
	 */
	private final Map<InlinedInstruction, Effects> effects(WalaConcurrentInformation tInfo) { 
		final Map<InlinedInstruction, Effects> effects = new LinkedHashMap<InlinedInstruction, Effects>();
		final Map<Effects, Effects> cache = new LinkedHashMap<Effects, Effects>();
		for(InlinedInstruction inst : tInfo.actions()) { 
			final Effects instEffects = new Effects(base.info(), inst);
			final Effects cached = cache.get(instEffects);
			if (cached==null) { 
				effects.put(inst, instEffects);
				cache.put(instEffects, instEffects);
			} else {
				effects.put(inst, cached);
			}
		}
		return effects;
	}
	
	/**
	 * Returns a map from each E in effectEquivalences.values to the subset S of instructions
	 * mapped to E such that: (1) there is a path in tInfo.threadOrder that contains all of the 
	 * instructions in S, and (2) there is no path in tInfo.threadOrder that contains a subset 
	 * of instructions mapped to E that is larger than S.
	 * @requires effects = effects(tInfo)
	 * @return a map, as described above
	 */
	private final Map<Effects, Set<InlinedInstruction>> maxEffects(WalaConcurrentInformation tInfo, Map<InlinedInstruction, Effects> effects) {
		
		final Map<InlinedInstruction, Map<Effects, Set<InlinedInstruction>>> results = new LinkedHashMap<InlinedInstruction, Map<Effects, Set<InlinedInstruction>>>();
		final Graph<InlinedInstruction> threadOrder = tInfo.threadOrder();
		
		for(Iterator<InlinedInstruction> insts = DFS.iterateFinishTime(threadOrder, Containers.iterate(tInfo.start())); insts.hasNext(); ) { 
			final InlinedInstruction inst = insts.next();
			final Effects effect = effects.get(inst);
			final Map<Effects, Set<InlinedInstruction>> result = new LinkedHashMap<Effects, Set<InlinedInstruction>>();
			for(Iterator<? extends InlinedInstruction> itr = threadOrder.getSuccNodes(inst); itr.hasNext(); ) { 
				final Map<Effects, Set<InlinedInstruction>> succ = results.get(itr.next());
				for(Map.Entry<Effects, Set<InlinedInstruction>> succEntry : succ.entrySet()) { 
					final Effects succEq = succEntry.getKey();
					final Set<InlinedInstruction> succInsts = succEntry.getValue();
					final Set<InlinedInstruction> currInsts = result.get(succEq);
					if (currInsts==null || currInsts.size()<succInsts.size())
						result.put(succEq, succInsts);
				}
			}
			if (result.containsKey(effect)) { 
				final Set<InlinedInstruction> currInsts = new LinkedHashSet<InlinedInstruction>(result.get(effect));
				currInsts.add(inst);
				result.put(effect, currInsts);
			} else {
				result.put(effect, Collections.singleton(inst));
			}
			results.put(inst, result);
		}
		
		return results.get(tInfo.start());
	}
	
	/**
	 * Returns the base expression factory.
	 * @return this.base
	 */
	public final ExpressionFactory base() { return base; }
	
	/**
	 * Returns the constant unary expression that evaluates to all atoms of the given kind.
	 * @return this.exprs[kind]
	 */
	public Expression valueOf(Action kind) { return actExprs.get(kind); }
	
	/**
	 * Returns the singleton unary expression that represents the field that is 
	 * accessed by the given read or write instruction.
	 * @requires inst in this.instructions
	 * @requires inst.instruction instanceof SSAFieldAccessInstruction
	 * @return this.exprs[this.fields[inst]]
	 */
	public Expression fieldOf(InlinedInstruction inst) { return fieldExprs.get(instFields.get(inst)); }
	
	/**
	 * Returns true if the upper bounds on the actions executable by inst1 and inst2
	 * (as given by {@linkplain #actionAtoms(TupleFactory, InlinedInstruction)} } intersect.
	 * @requires inst1 + inst2 in this.insts
	 * @return true if the upper bounds on the actions executable by inst1 and inst2 intersect.
	 */
	public final boolean mayShareActions(InlinedInstruction inst1, InlinedInstruction inst2) { 
		return intersects( instAtoms.get(inst1), instAtoms.get(inst2) );
	}
	
	/**
	 * Returns a tupleset containing all actions that the given instruction may perform.
	 * @requires inst in this.insts 
	 * @requires this.atoms() in factory.universe.atoms[int]
	 * @return tupleset all actions that the given instruction may perform.
	 */
	public final TupleSet actionAtoms(TupleFactory tuples, InlinedInstruction inst) { 
		if (instAtoms.get(inst)==null)
			System.out.println("\nNO ACTION FOR " /*+ "[[" + System.identityHashCode(inst) + ", " + inst.hashCode() + "]] "*/ + inst);
		return tupleset(tuples, instAtoms.get(inst));
	}
	
	/**
	 * Returns a tupleset containing the set of atoms that describe the locations
	 * that the given read/write instruction may access.
	 * @requires inst in this.insts
	 * @requires this.atoms() in factory.universe.atoms[int]
	 * @requires inst.action in NORMAL_READ + NORMAL_WRITE + VOLATILE_READ + VOLATILE_WRITE
	 * @return a tupleset containing the set of atoms that describe the locations
	 * that the given read/write instruction may access.
	 */
	public final TupleSet locationAtoms(TupleFactory tuples, InlinedInstruction inst) { 
		final SSAInstruction obj = inst.instruction();
		final TupleSet loc = tuples.noneOf(1);
		final ConstantFactory constants = base.constants();
		if (obj instanceof SSAFieldAccessInstruction) { 
			loc.add( tuples.tuple(fieldExprs.get(instFields.get(inst))) );
			final Set<InstanceKey> ref = referencedInstance(base.info(), inst);
			if (!ref.isEmpty()) { 
				loc.addAll( constants.instanceAtoms(tuples, ref)) ;
			} 
		} else {
			assert obj instanceof SSAArrayReferenceInstruction;
			final Set<InstanceKey> ref = referencedArray(base.info(), inst);
			loc.addAll( constants.instanceAtoms(tuples, ref) );
			loc.addAll( constants.constantAtoms(tuples, IRType.INTEGER) );
		} 
		return loc;
	}
	
	/**
	 * Returns a tupleset containing the set of atoms that describe the monitors that the
	 * given lock/unlock instruction may access.
	 * @requires inst in this.insts
	 * @requires this.atoms() in factory.universe.atoms[int]
	 * @requires inst.action in LOCK + UNLOCK
	 * @return a tupleset containing the set of atoms that describe the monitors that the
	 * given lock/unlock instruction may access.
	 */
	public final TupleSet monitorAtoms(TupleFactory tuples, InlinedInstruction inst) {
		return base.constants().instanceAtoms(tuples, referencedMonitor(base.info(), inst));
	}
	
	/**
	 * Returns a tupleset containing the set of atoms that describe the values that the
	 * given read/write instruction may read or write.
	 * @requires inst in this.insts
	 * @requires this.atoms() in factory.universe.atoms[int]
	 * @requires inst.action in NORMAL_READ + NORMAL_WRITE + VOLATILE_READ + VOLATILE_WRITE
	 * @return a tupleset containing the set of atoms that describe the values that the
	 * given read/write instruction may read or write.
	 */
	public final TupleSet valueAtoms(TupleFactory tuples, InlinedInstruction inst) { 	
		final ConstantFactory constants = base.constants();
		final Set<InstanceKey> rangeKeys = valueFor(base.info(), inst);
		if (!rangeKeys.isEmpty()) { 
			final TupleSet val = constants.instanceAtoms(tuples, rangeKeys);
			val.add(tuples.tuple(constants.nil()));
			return val;
		} else {
			final IRType type;
			final SSAInstruction delegate = inst.instruction();
			if (delegate instanceof SSAFieldAccessInstruction) { 
				type = IRType.convert(((SSAFieldAccessInstruction) delegate).getDeclaredFieldType());
			} else {
				type = IRType.convert(((SSAArrayReferenceInstruction) delegate).getElementType());
			}
			if (type.equals( IRType.OBJECT )) {
				// empty range for object type, which must
				// mean these objects are never created.  thus
				// null is the only valid object
				return tuples.setOf(constants.nil());
			} else {
				return constants.constantAtoms(tuples, type);
			}
		}
	}
	
	/**
	 * Returns an ordered set of atoms needed to represent all actions
	 * and fields generated by this factory and this.base.
	 * @return an ordered set of atoms needed to represent all actions
	 * and fields generated by this factory and this.base
	 */
	public final Set<?> atoms() { 
		final Set<Object> atoms = new LinkedHashSet<Object>(base.atoms());
		for(Set<?> s : instAtoms.values()) { atoms.addAll(s); }
		atoms.addAll(fieldExprs.values());
		return atoms;
	}
	
	/**
	 * Updates the given Bounds instance with 
	 * upper/lower bounds for the relations that make up
	 * the expressions generated by this factory and this.base.
	 * @requires this.atoms() in bounds.universe.atoms[int]
	 * @effects updates the given Bounds instance with 
	 * upper/lower bounds for the relations that make up
	 * the expressions generated by this factory and this.base.
	 */
	public void boundAll(Bounds bounds) { 
		base.boundAll(bounds);
		
		final TupleFactory tuples = bounds.universe().factory();
		final Map<Action,TupleSet> actBounds = new EnumMap<Action, TupleSet>(Action.class);
		for(Action a : Action.values()) { 
			actBounds.put(a, tuples.noneOf(1));
		}
		for(Map.Entry<InlinedInstruction, Set<?>> entry : instAtoms.entrySet()) { 
			actBounds.get(entry.getKey().action()).addAll(tupleset(tuples, entry.getValue()));
		}
		for(Map.Entry<Action, TupleSet> entry : actBounds.entrySet()) {
			bounds.boundExactly(actExprs.get(entry.getKey()), entry.getValue());
		}
		for(Map.Entry<IField, Relation> entry : fieldExprs.entrySet()) { 
			bounds.boundExactly(entry.getValue(), tuples.setOf(entry.getValue()));
		}
	}
	
	/**
	 * Returns a formula that expresses the representation
	 * invariants over all Kodkod relations that make up 
	 * the expressions generated by this factory and this.base.
	 * @return a formula that expresses the representation
	 * invariants over all Kodkod relations that make up 
	 * the expressions generated in this factory and this.base.
	 */
	public Formula invariants() { return base.invariants(); }
	
	/**
	 * Returns true if s1 and s2 intersect. Otherwise returns false.
	 * @return true if s1 and s2 intersect. Otherwise returns false.
	 */
	private static boolean intersects(Set<?> s1, Set<?> s2) { 
		final Set<?> small, big;
		if (s1.size()<=s2.size()) { 
			small = s1;
			big = s2;
		} else {
			small = s2;
			big = s1;
		}
		for(Object o : small) { 
			if (big.contains(o))
				return true;
		}
		return false;
	}
	
	/**
	 * An object that represents a particular action.
	 * @author etorlak
	 */
	private static final class ActionAtom {
		private final String name;
		
		ActionAtom(int idx) { this.name = "a"+idx; }
		ActionAtom(String name) { this.name = name; }
		public String toString() { return name; }
	}
	
	/**
	 * Describes the entities (objects, fields, methods, etc) that may be affected
	 * when a given instruction is executed.
	 * @specfield info: WalaInformation
	 * @specfield thread: info.threads.nodes // thread that contains the instruction
	 * @specfield instruction: info.concurrentInformation(thread).actions // the instruction
	 * @specfield action: Action // the kind of action performed by the instruction
	 * @specfield location: Object // affected "location": field, method reference, or cg node
	 * @specfield instances: set InstanceKey // affected objects
	 * @author etorlak
	 */
	private static final class Effects {
		final CGNode thread;
		final Action action;
		final Object location;
		final Set<InstanceKey> instances;
		
		/**
		 * Constructs an effects descriptor for the given instruction. 
		 * In particular, a field access instruction is mapped to the field that is accessed and to the set of InstanceKeys
		 * representing the objects whose field may be being read/written.  An array access instruction is mapped to its thread
		 * and to the set of InstanceKeys representing the array objects that may be read/written. A monitor instruction is mapped 
		 * to its cgNode and to the set of InstanceKeys representing the objects that may be locked/unlocked.  A special instruction 
		 * is mapped to the method reference describing the invoked method and to the empty set.  Start/end instructions are mapped to
		 * their thread and to the empty set.
		 * @effects this.instruction' = inst
		 */
		Effects(WalaInformation info, InlinedInstruction inst) {
			final IClassHierarchy cha = info.callGraph().getClassHierarchy();
			this.action = inst.action();
			this.thread = thread(inst);
			switch(action) { 
			case START : case END : 
				location = thread;
				instances = Collections.emptySet();
				break;
			case SPECIAL :
				location = cha.resolveMethod(((SSAAbstractInvokeInstruction)inst.instruction()).getDeclaredTarget()).getReference();
				instances = Collections.emptySet();
				break;
			case LOCK : case UNLOCK :
				location = inst.cgNode();
				instances = referencedMonitor(info, inst);
				break;
			case NORMAL_READ : case VOLATILE_READ : case NORMAL_WRITE : case VOLATILE_WRITE :
				if (inst.instruction() instanceof SSAFieldAccessInstruction) { 
					location = cha.resolveField(((SSAFieldAccessInstruction)inst.instruction()).getDeclaredField());
					instances = referencedInstance(info, inst);
				} else {
					assert inst.instruction() instanceof SSAArrayReferenceInstruction;
					location = thread;
					instances = referencedArray(info, inst);
				}
				break;
			default : throw new AssertionError("unreachable");
			}
		}

		/**
		 * Returns true if the effects of this.instruction may overlap with the effects of other.instruction.
		 * @return true if the effects of this.instruction may overlap with the effects of other.instruction.
		 */
		boolean overlaps(Effects other) { 

			if (this==other) 
				return true;
			else 
				return action.equals(other.action) && thread.equals(other.thread) && 
				       location.equals(other.location) && 
				       ((instances.isEmpty() && other.instances.isEmpty()) || intersects(instances, other.instances));
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + action.hashCode();
			result = prime * result + instances.hashCode();
			result = prime * result + location.hashCode();
			result = prime * result + thread.hashCode();
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Effects other = (Effects) obj;
			return action.equals(other.action) && thread.equals(other.thread) && 
				   location.equals(other.location) && instances.equals(other.instances);
		}
		
	}
}

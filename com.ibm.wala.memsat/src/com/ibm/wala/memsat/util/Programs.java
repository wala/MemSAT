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
package com.ibm.wala.memsat.util;

import static com.ibm.wala.memsat.util.Graphs.reflexiveTransitiveClosure;
import static com.ibm.wala.memsat.util.Graphs.transitiveClosure;
import static com.ibm.wala.memsat.util.Graphs.union;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Provides a set of utility methods for working with Wala data structures
 * for concurrent programs.
 * 
 * @see InlinedInstruction
 * @see WalaInformation
 * @see WalaConcurrentInformation
 * 
 * @author etorlak
 */
public final class Programs {
	private Programs() {}
	
	/**
	 * Returns a set of instance keys that represent the value that is read or written by the given instruction
	 * or the empty set if the value is a primitive.
	 * @requires some n : info.threads | info.concurrentInformation(n).actions().contains(inst)
	 * @requires inst.action in NORMAL_READ + VOLATILE_READ + NORMAL_WRITE + VOLATILE_WRITE
	 * @return set of instance keys that represent the value that is read or written by the given instruction
	 * or the empty set if the value is a primitive.
	 */
	public static Set<InstanceKey> valueFor(WalaInformation info, InlinedInstruction inst) {
		final int use;
		final SSAInstruction obj = inst.instruction();
		if (obj instanceof SSAGetInstruction || obj instanceof SSAArrayLoadInstruction) { 
			use = obj.getDef();
		} else if (obj instanceof SSAPutInstruction) {
			use = ((SSAPutInstruction)obj).getVal();
		} else if (obj instanceof SSAArrayStoreInstruction) { 
			use = ((SSAArrayStoreInstruction)obj).getValue();
		} else {
			throw new IllegalArgumentException(inst + " is not a read or write instruction.");
		}
		return info.pointsTo(info.cgNodeInformation(inst.cgNode()).pointerKeyFor(use));
	}
	
	/**
	 * Returns a set of instance keys that represent the object locked or unlocked by the given  instruction.
	 * @requires some n : info.threads | info.concurrentInformation(n).actions().contains(inst)
	 * @requires inst.action instanceof SSAMonitorInstruction
	 * @return a set of instance keys that represent the object locked or unlocked by the given  instruction.
	 */
	public static Set<InstanceKey> referencedMonitor(WalaInformation info, InlinedInstruction inst) { 
		final int use = ((SSAMonitorInstruction)inst.instruction()).getRef();
		return info.pointsTo(info.cgNodeInformation(inst.cgNode()).pointerKeyFor(use));
	}
	
	/**
	 * Returns a set of instance keys that represent the object whose field is being read or written 
	 * by the instruction, or the empty set if the accessed field is static.
	 * @requires some n : info.threads | info.concurrentInformation(n).actions().contains(inst)
	 * @requires inst.instruction instanceof SSAFieldAccessInstruction
	 * @return a set of instance keys that represent the object whose field is being read or written 
	 * by the instruction, or the empty set if the accessed field is static.
	 */
	public static Set<InstanceKey> referencedInstance(WalaInformation info, InlinedInstruction inst) { 
		final int use = ((SSAFieldAccessInstruction)inst.instruction()).getRef();
		if (use<1)
			return Collections.emptySet();
		else 
			return info.pointsTo(info.cgNodeInformation(inst.cgNode()).pointerKeyFor(use));
	}
	
	/**
	 * Returns a set of instance keys that represent the array instance being read or written by the given instruction.
	 * @requires some n : info.threads | info.concurrentInformation(n).actions().contains(inst)
	 * @requires inst.instruction instanceof SSAArrayReferenceInstruction
	 * @return a set of instance keys that represent the array instance being read or written by the given instruction.
	 */
	public static Set<InstanceKey> referencedArray(WalaInformation info, InlinedInstruction inst) { 
		final int use = ((SSAArrayReferenceInstruction)inst.instruction()).getArrayRef();
		return info.pointsTo(info.cgNodeInformation(inst.cgNode()).pointerKeyFor(use));
	}
	

	/**
	 * Returns the set of all methods m such that m is the governing node for one of given instructions or 
	 * m is in the call stack for one the given instructions.
	 * @return set of methods as described above
	 */
	public static Set<CGNode> relevantMethods(Set<InlinedInstruction> insts) { 
		final Set<CGNode> methods = new LinkedHashSet<CGNode>();
		for(InlinedInstruction inst : insts) { 
			methods.add(inst.cgNode());
			for(CallSite cs : inst.callStack()) { 
				methods.add(cs.getNode());
			}
		}
		return methods;
	}
	
	/**
	 * Returns a map from each read/write instruction in the given info to the field it accesses.
	 * @return { i: InlinedInstruction, f: IField | some t: info.threads() | 
	 * 			info.concurrentInformation(t).actions().contains(i) and
	 * 			i.instruction instanceof SSAFieldAccessInstruction and 
	 * 			f = base.info.callGraph().getClassHierarchy().resolveField( i.getDeclaredField() ) }
	 */
	public static Map<InlinedInstruction,IField> referencedFields(WalaInformation info) { 
		final Map<InlinedInstruction,IField> fields = new LinkedHashMap<InlinedInstruction, IField>();
		final IClassHierarchy cha = info.callGraph().getClassHierarchy();
		for(CGNode thread : info.threads()) { 	
			for(InlinedInstruction inst : info.concurrentInformation(thread).actions()) { 
				if (inst.instruction() instanceof SSAFieldAccessInstruction) {
					final SSAFieldAccessInstruction access = (SSAFieldAccessInstruction) inst.instruction();
					final IField field = cha.resolveField(access.getDeclaredField());
					assert field != null;
					fields.put(inst, field);
				}
			}
		}
		return fields;
	}
	
	/**
	 * Returns the entry method of the thread that contains the given inlined instruction.
	 * @return inst.cgNode if inst.callStack is empty, otherwise returns the bottom of inst.callStack
	 */
	public static CGNode thread(InlinedInstruction inst) {
		if (inst.callStack().empty())
			return inst.cgNode();
		else {
			CallSite last = null;
			for(CallSite cs : inst.callStack()) { 
				last = cs;
			}
			return last.getNode();
		}
	} 
	
	
	/**
	 * Returns the set of all inlined instructions in info.threads.  The instructions
	 * are ordered in the DFS discovery order (both with respect to the threads
	 * graph and the individual threadOrder graphs).
	 * @requires info.threads.size() > 1
	 * @return info.threads.actions
	 */
	public static Set<InlinedInstruction> instructions(WalaInformation info) {
		final Set<InlinedInstruction> ret = new LinkedHashSet<InlinedInstruction>();
		final Graph<CGNode> threads = info.threads();
		for(Iterator<CGNode> tItr = DFS.iterateDiscoverTime(threads, Graphs.root(threads)); tItr.hasNext(); ) { 
			final WalaConcurrentInformation cInfo = info.concurrentInformation(tItr.next());
			for(Iterator<InlinedInstruction> insts = DFS.iterateDiscoverTime(cInfo.threadOrder(), cInfo.start()); insts.hasNext(); ) { 
				ret.add(insts.next());
			}
		}
		return ret;
	}
	
	/**
	 * Returns a subset of the given set that contains only the instructions that produce the given kind of action.
	 * @return a subset of the given set that contains only the instructions that produce the given kind of action.
	 */
	public static Set<InlinedInstruction> instructionsOfType(Set<InlinedInstruction> insts, Set<Action> actions) { 
		final Set<InlinedInstruction> ret = new LinkedHashSet<InlinedInstruction>();
		for(InlinedInstruction inst : insts) { 
			if (actions.contains(inst.action()))
				ret.add(inst);
		}
		return ret;
	}
	
	/**
	 * Returns the transitive closure of the union of the thread orders of all threads in info.threads.
	 * @return the transitive closure of the union of the thread orders of all threads in info.threads.
	 */
	public static Graph<InlinedInstruction> programOrder(WalaInformation info) { 
		final Graph<CGNode> threads = info.threads();
		final Collection<Graph<InlinedInstruction>> ords = new ArrayList<Graph<InlinedInstruction>>(threads.getNumberOfNodes());
		for(CGNode t : threads) { 
			ords.add(info.concurrentInformation(t).threadOrder());
		}
		return transitiveClosure(union(ords));
	}
	
	/**
	 * Returns a filter that accepts instructions that produce one of the given actions.
	 * @return a filter that accepts instructions that produce one of the given actions.
	 */
	public static final Predicate<InlinedInstruction> filter(final Action act, final Action...acts) { 
		return new Predicate<InlinedInstruction>() {
			final Set<Action> accepted = EnumSet.of(act, acts);
			public boolean test(InlinedInstruction o) { 
				return accepted.contains(o.action()); 
			}
		};
	}
	
//	/**
//	 * Returns a graph that represents the upper bound on the 
//	 * synchronization order over the specified synchronization actions of info.threads.
//	 * Synchronization actions must always include the start and end actions.  The synchronization
//	 * order graph is obtained by taking the 
//	 * the {@linkplain #programOrder(WalaInformation) program order} of info.threads, restricting
//	 * it to synchronization instructions, and then adding appropriate edges between synchronization
//	 * instructions in different threads.  In particular, given two instructions i1 and i2 from threads 
//	 * t1 and t2, the edge i1->i2 is present iff there is no path from t2 to t1 in the info.threads graph.
//	 * @requires START + END in syncs
//	 * @return a graph that represents the upper bound on the 
//	 * synchronization order over the specified synchronization actions of info.threads.
//	 */
//	public static Graph<InlinedInstruction> syncOrder(final WalaInformation info, final Set<Action> syncs) { 
//		assert syncs.contains(Action.START) && syncs.contains(Action.END);
//		final Filter<InlinedInstruction> syncFilter = new Filter<InlinedInstruction>() {
//			public boolean accepts(InlinedInstruction o) { return syncs.contains(o.action()); }
//		};
//		
//		final Map<CGNode, Graph<InlinedInstruction>> threadSync = new LinkedHashMap<CGNode, Graph<InlinedInstruction>>();
//		for(CGNode n : info.threads()) { 
//			final Graph<InlinedInstruction> po = reflexiveTransitiveClosure(info.concurrentInformation(n).threadOrder());
//			threadSync.put(n, Graphs.restrict(po, syncFilter));
//		}
//		
//		final Graph<InlinedInstruction> syncOrd = union(threadSync.values());
//		final Graph<CGNode> threadOrd = reflexiveTransitiveClosure(info.threads());
//		for(CGNode n1 : threadOrd) { 		
//			final Graph<InlinedInstruction> s1 = threadSync.get(n1);
//			for(CGNode n2 : threadOrd) { 
//				if (!threadOrd.hasEdge(n2, n1)) { 
//					for(InlinedInstruction i1 : s1) { 
//						for(InlinedInstruction i2 : threadSync.get(n2)) { 
//							syncOrd.addEdge(i1, i2);
//						}
//					}	
//				} 
//			}
//		}
//		return syncOrd;
//	}
	
	
	/**
	 * Returns a graph that represents the upper bound on the execution order of the instructions in info.threads.
	 * The execution order graph is obtained by taking the {@linkplain #programOrder(WalaInformation) program order} 
	 * of info.threads and then adding appropriate edges between 
	 * instructions in different threads.  In particular, given two instructions i1 and i2 from threads 
	 * t1 and t2, the edge i1->i2 is present iff there is no path from t2 to t1 in the info.threads graph.
	 * @param info
	 * @return a graph that represents the upper bound on the execution order of the instructions in info.threads.
	 */
	public static Graph<InlinedInstruction> executionOrder(final WalaInformation info) { 
		final Graph<InlinedInstruction> exec = programOrder(info);
		final Graph<CGNode> threadOrd = reflexiveTransitiveClosure(info.threads());
		for(CGNode n1 : threadOrd) { 		
			final Set<InlinedInstruction> s1 = info.concurrentInformation(n1).actions();
			for(CGNode n2 : threadOrd) { 
				if (!threadOrd.hasEdge(n2, n1)) { 
					final Set<InlinedInstruction> s2 = info.concurrentInformation(n2).actions();
					for(InlinedInstruction i1 : s1) { 
						for(InlinedInstruction i2 : s2) { 
							exec.addEdge(i1, i2);
						}
					}	
				} 
			}
		}
		return exec;
	}
	
	/**
	 * Returns a graph that maps each read in info.threads to the set of writes that it may see.
	 * @return a graph that maps each read in info.threads to the set of writes that it may see.
	 */
	public static Graph<InlinedInstruction> visibleWrites(WalaInformation info) { 
		final Graph<InlinedInstruction> ret = new LinkedHashGraph<InlinedInstruction>();
		for(CGNode n : info.threads()) { 
			final WalaConcurrentInformation cinfo = info.concurrentInformation(n);
			for(InlinedInstruction inst : cinfo.actions()) { 
				if (inst.action()==Action.NORMAL_READ || inst.action()==Action.VOLATILE_READ) { 
					System.out.println("VISIBLE WRITES FOR " + inst + ":");
					for(InlinedInstruction write : cinfo.visibleWrites(inst)) { 
						System.out.println(" " + write.action() + " " + write);
						ret.addEdge(inst, write);
					}
				}
			}
		}
		return ret;
	}
}

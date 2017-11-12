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
package com.ibm.wala.memsat.frontEnd.core;

import static com.ibm.wala.memsat.util.Strings.prettyPrint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.DependenceGraph;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.frontEnd.engine.CallGraphCreation;
import com.ibm.wala.memsat.frontEnd.engine.MiniaturAnalysisEngine;
import com.ibm.wala.memsat.frontEnd.fieldssa.FieldAccesses;
import com.ibm.wala.memsat.frontEnd.fieldssa.FieldAccessesFactory;
import com.ibm.wala.memsat.frontEnd.fieldssa.FieldNameSSAConversion;
import com.ibm.wala.memsat.frontEnd.fieldssa.IPFieldAccessAnalysis;
import com.ibm.wala.memsat.frontEnd.fieldssa.IPFieldAccessesResult;
import com.ibm.wala.memsat.frontEnd.slicer.PartialSlice;
import com.ibm.wala.memsat.frontEnd.slicer.PartialSlice.InstructionEntry;
import com.ibm.wala.memsat.frontEnd.types.MiniaturTypeData;
import com.ibm.wala.memsat.frontEnd.types.MiniaturTypeDataFactory;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.SlowDFSFinishTimeIterator;
import com.ibm.wala.util.intset.OrdinalSet;

import kodkod.util.collections.LinkedStack;
import kodkod.util.collections.Stack;
import kodkod.util.ints.IndexedEntry;

public class WalaInformationImpl implements WalaInformation {

	private final Graph<CGNode> threadRoots;

	private final IPFieldAccessesResult ipFieldAccesses;

	private final MiniaturTypeDataFactory typeDataFactory;

	private final SDG sdg;

	private final Collection<Statement> slice;

	private final PointerAnalysis pointerAnalysis;

	private final IClassHierarchy cha;

	private final int initialHeapSize;

	private final AnalysisOptions analysisOptions;

	private final CallGraph callGraph;

	private final Set possiblyAccessedFields = new HashSet();

	private final Map allocatedObjectCounts = new HashMap();

	private final Set openWorldTypes = new HashSet();

	private final Options opt;
	
	public WalaInformationImpl(Options opt, MiniaturAnalysisEngine engine,
			Graph<MethodReference> rootReferences)
			throws com.ibm.wala.util.CancelException,
			java.io.IOException {
		CallGraphCreation cgEngine = new CallGraphCreation(engine);
		CallGraph fullCG = cgEngine.createCallGraph();

		this.opt = opt;
		
		this.threadRoots = SlowSparseNumberedGraph.make();
		for (MethodReference m : rootReferences) {
			assert fullCG.getNodes(m).size() == 1;
			CGNode root = fullCG.getNodes(m).iterator().next();
			threadRoots.addNode(root);
		}
		for (MethodReference m : rootReferences) {
			CGNode root = fullCG.getNodes(m).iterator().next();
			Iterator<? extends MethodReference> ss = rootReferences
					.getSuccNodes(m);
			while (ss.hasNext()) {
				MethodReference s = ss.next();
				assert fullCG.getNodes(s).size() == 1;
				CGNode succ = fullCG.getNodes(s).iterator().next();
				threadRoots.addEdge(root, succ);
			}
		}
		if (threadRoots.getNumberOfNodes() > 1) {
			for (MethodReference m : rootReferences) {
				assert fullCG.getNodes(m).size() == 1;
				CGNode root = fullCG.getNodes(m).iterator().next();
				IClass cls = root.getMethod().getDeclaringClass();
				IMethod cctor = cls.getClassInitializer();
				if (cctor != null) {
					CGNode cctorNode = fullCG.getNodes(cctor.getReference()).iterator().next();
					if (! threadRoots.containsNode(cctorNode)) {
						threadRoots.addNode(cctorNode);
					}
					threadRoots.addEdge(cctorNode, root);
				}
			}
		}

		Pair<Collection<Statement>, SDG<InstanceKey>> sliceData = PartialSlice
				.computeAssertionSlice(fullCG, engine.getPointerAnalysis(),
						Iterator2Collection.toList(threadRoots.iterator()),
						threadRoots.getNumberOfNodes() > 1);
		this.sdg = sliceData.snd;
		this.slice = new HashSet<Statement>(sliceData.fst);
		for (Statement s : sliceData.fst) {
			if (s.getKind() == Statement.Kind.PARAM_CALLER) {
				ParamCaller x = ((ParamCaller) s);
				NormalStatement inst = new NormalStatement(x.getNode(), x
						.getInstructionIndex());
				this.slice.add(inst);
			}
		}

		this.callGraph = PartialCallGraph.make(fullCG, Iterator2Collection
				.toSet(threadRoots.iterator()));

		this.cha = engine.getClassHierarchy();

		this.pointerAnalysis = engine.getPointerAnalysis();

		this.analysisOptions = cgEngine.getOptions();

		this.typeDataFactory = new MiniaturTypeDataFactory(pointerAnalysis, cha);

		this.initialHeapSize = opt.openWorldScopeSize();

		this.ipFieldAccesses = (new IPFieldAccessAnalysis(callGraph,
				new FieldAccessesFactory() {
					public FieldAccesses get(CGNode node) {
						return new FieldNameSSAConversion.LocalFieldAccesses(
								true, node, pointerAnalysis, cha);
					}
				}) {

			protected Iterator getInstructions(final CGNode node) {
				final PDG pdg = sdg.getPDG(node);
				final IR ir = node.getIR();
				final Map indicesMap = PDG.computeInstructionIndices(ir);
				return new FilterIterator(
						node.getIR().iterateAllInstructions(), new Predicate() {
							private boolean sliceContainsInstruction(
									SSAInstruction s) {
								return slice.contains(PDG
										.ssaInstruction2Statement(node, s,
												indicesMap, ir));
							}

							public boolean test(Object o) {
								return sliceContainsInstruction((SSAInstruction) o);
							}
						});
			}
		}).solve();

		computeRelevantStuff();
	}

	public AnalysisOptions analysisOptions() {
		return analysisOptions;
	}

	public CallGraph callGraph() {
		return callGraph;
	}

	public boolean concurrent() {
		return threadRoots.getNumberOfNodes() > 1;
	}

	public Graph<CGNode> threads() {
		return threadRoots;
	}

	private boolean isSpecialAtomic(IMethod node) {
		return node.toString().indexOf("java/util/concurrent/lock") != -1;
	}

	private OrdinalSet<? extends InstanceKey> getPointsTo(PointerKey ptrKey) {
		return pointerAnalysis.getPointsToSet(ptrKey);
	}

	public Set<InstanceKey> pointsTo(PointerKey ptrKey) {
		Set<InstanceKey> result = new HashSet<InstanceKey>();
		for (Iterator<? extends InstanceKey> x = getPointsTo(ptrKey).iterator(); x
				.hasNext();) {
			result.add(x.next());
		}
		return result;
	}

	private Set computeOpenWorldParameters() {
		Set openWorldParameters = new HashSet();

		PointerKeyFactory pkf = pointerAnalysis.getHeapModel();
		for (Iterator roots = threadRoots.iterator(); roots.hasNext();) {
			CGNode root = (CGNode) roots.next();
			for (int i = 0; i < root.getMethod().getNumberOfParameters(); i++) {
				PointerKey pk = pkf.getPointerKeyForLocal(root, i + 1);
				openWorldParameters.add(pk);
			}
		}

		return openWorldParameters;
	}

	private void computeTypesPossiblyReachedFromInitialHeap() {
		final HeapGraph HG = pointerAnalysis.getHeapGraph();
		Iterator allKeys = new SlowDFSFinishTimeIterator(HG,
				computeOpenWorldParameters().iterator()) {
			protected Iterator getConnected(final Object src) {
				return new FilterIterator(HG.getSuccNodes(src), new Predicate() {
					public boolean test(Object dst) {
						if (src instanceof InstanceKey) {
							if (dst instanceof AbstractFieldPointerKey) {
								if (dst instanceof InstanceFieldKey) {
									if (!possiblyAccessedFields.contains(dst)) {
										return false;
									}

								} else if (dst instanceof ArrayContentsKey) {
									if (!possiblyAccessedFields.contains(dst)) {
										return false;
									}

								} else {
									Assertions.UNREACHABLE();
								}
							}
						}

						return true;
					}
				});
			}
		};

		while (allKeys.hasNext()) {
			Object k = allKeys.next();
			if (k instanceof InstanceKey) {
				openWorldTypes.add(k);
				if (!allocatedObjectCounts.containsKey(k)) {
					allocatedObjectCounts.put(k, new Integer(initialHeapSize));
				} else {
					allocatedObjectCounts.put(k, new Integer(initialHeapSize
							+ ((Integer) allocatedObjectCounts.get(k))
									.intValue()));
				}
			}
		}
	}

	private void computeRelevantStuffInSlice() {
		HeapModel HM = pointerAnalysis.getHeapModel();
		for (CGNode threadRoot : threadRoots) {
			concurrentInformation(threadRoot).computeRelevantStuffInSlice();
		}
	}
	
	private void computeRelevantStuff() {
		computeRelevantStuffInSlice();
		computeTypesPossiblyReachedFromInitialHeap();
	}

	public Set<InstanceKey> relevantClasses() {
		return allocatedObjectCounts.keySet();
	}

	public int cardinality(InstanceKey eqClass) {
		if (allocatedObjectCounts.containsKey(eqClass)) {
			return ((Integer) allocatedObjectCounts.get(eqClass)).intValue();
		} else {
			return 0;
		}
	}

	public boolean openWorldType(InstanceKey ik) {
		return openWorldTypes.contains(ik);
	}

	public Set<PointerKey> relevantFields() {
		return possiblyAccessedFields;
	}

	public WalaCGNodeInformation cgNodeInformation(final CGNode node) {
		return new WalaCGNodeInformation() {
			private FieldSSATable fieldSSA;

			private MiniaturTypeData typeData;

			public CGNode cgNode() {
				return node;
			}

			public PointerKey pointerKeyFor(int valueNumber) {
				return new LocalPointerKey(node, valueNumber);
			}

			public FieldSSATable fieldSSA() {
				if (fieldSSA == null) {
					fieldSSA = PartialSlice.sliceFieldSSA(node,
							pointerAnalysis, cha, ipFieldAccesses, sdg, slice);
				}

				return fieldSSA;
			}

			public Iterator<? extends IndexedEntry<SSAInstruction>> relevantInstructions() {
				return PartialSlice.relevantInstructions(node, fieldSSA(), sdg,
						slice);
			}

			public DependenceGraph<SSACFG.BasicBlock> controlDependences() {
				final ControlDependenceGraph cdg = new ControlDependenceGraph(
						node.getIR().getControlFlowGraph(), true);

				class CDG extends SlowSparseNumberedGraph<SSACFG.BasicBlock>
						implements DependenceGraph<SSACFG.BasicBlock> {
					{
						for (Iterator ns = cdg.iterator(); ns.hasNext();) {
							addNode((SSACFG.BasicBlock) ns.next());
						}

						for (Iterator ns = cdg.iterator(); ns.hasNext();) {
							SSACFG.BasicBlock n = (SSACFG.BasicBlock) ns.next();
							for (Iterator ss = cdg.getSuccNodes(n); ss
									.hasNext();) {
								addEdge(n, (SSACFG.BasicBlock) ss.next());
							}
						}
					}

					public Set<SSACFG.BasicBlock> edgeLabels(
							SSACFG.BasicBlock source, SSACFG.BasicBlock sink) {
						Set<SSACFG.BasicBlock> result = new HashSet();
						for (Iterator els = cdg.getEdgeLabels(source, sink)
								.iterator(); els.hasNext();) {
							result.add((SSACFG.BasicBlock) els.next());
						}
						return result;
					}
				}
				;

				return new CDG();
			}

			public IRType typeOf(int valueNumber) {
				if (typeData == null) {
					typeData = typeDataFactory.get(node);
					
//					Trace.println("type data for " + node);
//					Trace.println(node.getIR().toString());
//					Trace.println(typeData.toString());

				}

				return typeData.typeOf(valueNumber);
			}
		};
	}

	enum Recurse {
		Pre, Post, None
	};

	interface Reduce<X, Y> {
		Y initial();

		Y reduce(X x, Y y);
	}

	interface WalaConcurrentInformationInternal extends WalaConcurrentInformation {
		
		void computeRelevantStuffInSlice();
		
	}
	
	public WalaConcurrentInformationInternal concurrentInformation(
			final CGNode threadRoot) {
		return new WalaConcurrentInformationInternal() {

			private final Map instructionMaps = new HashMap();

			class InlinedInstructionImpl implements InlinedInstruction {
				private final CGNode node;

				private final int index;

				private final SSAInstruction inst;

				private final SSACFG.BasicBlock bb;
				
				private final Stack<CallSite> base;

				private final Action action;

				InlinedInstructionImpl(CGNode node, int index, SSACFG.BasicBlock bb, SSAInstruction inst, Stack<CallSite> base) {
					this.node = node;
					this.index = index;
					this.inst = inst;
					this.base = base;
					this.bb = bb;
					if (index == Integer.MIN_VALUE)
						this.action = Action.START;
					else if (index == Integer.MAX_VALUE)
						this.action = Action.END;
					else if (inst instanceof SSAArrayLoadInstruction)
						this.action = Action.NORMAL_READ;
					else if (inst instanceof SSAArrayStoreInstruction)
						this.action = Action.NORMAL_WRITE;
					else if (inst instanceof SSAAbstractInvokeInstruction) {
						MethodReference target = ((SSAAbstractInvokeInstruction)inst).getDeclaredTarget();
						if (concurrent() && opt.memoryModel().memoryInstructions().contains(target)) {
							this.action = Action.SPECIAL;
						} else {
							this.action = null;
						}
					} else if (inst instanceof SSAMonitorInstruction) {
						if (((SSAMonitorInstruction)inst).isMonitorEnter()) {
							this.action = Action.LOCK;
						} else {
							this.action = Action.UNLOCK;
						}
					} else if (inst instanceof SSAFieldAccessInstruction) { 
						final SSAFieldAccessInstruction access = (SSAFieldAccessInstruction) inst;
						final IField f = node.getClassHierarchy().resolveField(access.getDeclaredField());
						final boolean isVolatile = f.isVolatile();
						if (! (f.isStatic() && f.isFinal())) {
							if (access instanceof SSAGetInstruction) {
								this.action = isVolatile ? Action.VOLATILE_READ
										: Action.NORMAL_READ;
							} else {
								this.action = isVolatile ? Action.VOLATILE_WRITE
										: Action.NORMAL_WRITE;
							}
						} else {
							this.action = null;
						}
					} else {
						this.action = null;
					}
				}

				public CGNode cgNode() {
					return node;
				}

				public int instructionIndex() {
					return index;
				}

				public SSAInstruction instruction() {
					return inst;
				}

				public Stack<CallSite> callStack() {
					return base;
				}

				public Action action() {
					return action;
				}

				public InlinedInstruction related(int index, SSAInstruction inst) {
					return new InlinedInstructionImpl(node, index, bb, inst, base);
				}
				
				public boolean equals(Object o) {
					if (o==this) return true;
					else if (o==null) return false;
					else if (o instanceof InlinedInstruction) { 
						final InlinedInstruction inlined = (InlinedInstruction)o;
						return 	node.equals(inlined.cgNode()) &&
								index == inlined.instructionIndex() &&
								(inst == null ? inlined.instruction() == null : inst.equals(inlined.instruction())) && 
								base.equals(inlined.callStack());
					} else return false;
				}

				public int hashCode() {
					int code = node.hashCode();
					code += index;
					for (CallSite ref : base) {
						code += ref.hashCode();
					}

					return code;
				}
				
				public String toString() {
					final StringBuilder b = new StringBuilder();
					for(CallSite ref : base) { 
						b.insert(0, ref.getNode().getMethod().getSignature() + "_");
					}
					b.append(node.getMethod().getSignature()+"::");
					if (action==Action.START || action==Action.END) { 
						b.append(action.toString().toLowerCase());
					} else {
						b.append(index);
					}
					return b.toString();
				}
			}

			private <E, R> R instructions(
					Function<InlinedInstructionImpl, E> f, Reduce<E, R> r,
					Recurse recurse) {
				return instructions(threadRoot, new LinkedStack<CallSite>(), f,
						r, recurse, r.initial());
			}

			private Stack<CallSite> makeCalleeStack(Stack<CallSite> current, CallSiteReference site, CGNode node) {
				//
				// fix this!
				//
				// must copy because references to the stack are captured,
				// so
				// mutating rather than copying will break everything.
				//
				Stack<CallSite> calleeStack = new LinkedStack<CallSite>();
				for (CallSite ref : current) {
					calleeStack.push(ref);
				}
				calleeStack.push(new CallSite(site, node));
				return calleeStack;
			}
			
			private <E, R> R call(SSAInstruction inst, CGNode node,
					Stack<CallSite> base,
					Function<InlinedInstructionImpl, E> f, Reduce<E, R> r,
					Recurse recurse, Recurse test, R result) {
				if (recurse == test
						&& inst instanceof SSAAbstractInvokeInstruction) {
					CallSiteReference x = ((SSAAbstractInvokeInstruction) inst)
							.getCallSite();

					// these are not really method calls
					if (concurrent() && opt.memoryModel().memoryInstructions().contains(x.getDeclaredTarget())) {
						return result;
					}
					
					Stack<CallSite> calleeStack = makeCalleeStack(base, x, node);
					
					for (CGNode target : callGraph.getPossibleTargets(node, x)) {
						result = instructions(target, calleeStack, f, r,
								recurse, result);
					}

				}

				return result;
			}

			private <E, R> R instructions(final CGNode node,
					final Stack<CallSite> base,
					Function<InlinedInstructionImpl, E> f, Reduce<E, R> r,
					Recurse recurse, R result) {
				WalaCGNodeInformation nodeInfo = cgNodeInformation(node);
				for (Iterator<? extends IndexedEntry<SSAInstruction>> it = nodeInfo
						.relevantInstructions(); it.hasNext();) {
					InstructionEntry I = (InstructionEntry) it
							.next();
					final int index = I.index();
					final SSAInstruction inst = I.value();

					result = call(inst, node, base, f, r, recurse, Recurse.Pre,
							result);
					result = r.reduce(f.apply(new InlinedInstructionImpl(node,
							index, I.bb(), inst, base)), result);
					result = call(inst, node, base, f, r, recurse,
							Recurse.Post, result);
				}

				return result;
			}

			private int getInstructionIndex(CGNode node, SSAInstruction inst) {
				if (!instructionMaps.containsKey(node)) {
					instructionMaps.put(node, new ObjectArrayMapping(node
							.getIR().getInstructions()));
				}

				return ((ObjectArrayMapping) instructionMaps.get(node))
						.getMappedIndex(inst);
			}

			public Set<InlinedInstruction> actions() {
				Set<InlinedInstruction> mostActions = instructions(
						new Function<InlinedInstructionImpl, InlinedInstruction>() {
							public InlinedInstruction apply(
									InlinedInstructionImpl x) {
								if (x.action != null) {
									return x;
								} else {
									return null;
								}
							}
						},
						new Reduce<InlinedInstruction, Set<InlinedInstruction>>() {
							public Set<InlinedInstruction> initial() {
								return new HashSet<InlinedInstruction>();
							}

							public Set<InlinedInstruction> reduce(
									InlinedInstruction x,
									Set<InlinedInstruction> y) {
								if (x != null) {
									y.add(x);
								}

								return y;
							}
						}, Recurse.Pre);
				mostActions.add(start());
				mostActions.add(end());
				return mostActions;
			}

			//
			// TODO: how to treat heap phi nodes, interprocedrual edges?
			//
			public Graph<InlinedInstruction> memoryDependences() {
				return instructions(
						new Function<InlinedInstructionImpl, InlinedInstructionImpl>() {
							public InlinedInstructionImpl apply(
									InlinedInstructionImpl x) {
								if (x.instruction() instanceof SSAFieldAccessInstruction) {
									return x;
								} else {
									return null;
								}
							}
						},
						new Reduce<InlinedInstructionImpl, Graph<InlinedInstruction>>() {
							public Graph<InlinedInstruction> initial() {
								return SlowSparseNumberedGraph.make();
							}

							public Graph<InlinedInstruction> reduce(
									InlinedInstructionImpl x,
									Graph<InlinedInstruction> y) {
								if (x != null) {
									y.addNode(x);

									WalaCGNodeInformation info = cgNodeInformation(x
											.cgNode());
									FieldSSATable ssa = info.fieldSSA();
									DefUse du = ssa.getDefUse();
									int uses[] = ssa.getUses(x.instruction());
									for (int i = 0; i < uses.length; i++) {
										SSAInstruction dep = du.getDef(uses[i]);
										if (dep != null) {
											InlinedInstruction d = x.related(
													getInstructionIndex(x
															.cgNode(), dep),
													dep);

											y.addNode(d);
											assert d != x;
											y.addEdge(d, x);
										}
									}
								}

								return y;
							}
						}, Recurse.Pre);
			}
			
			public Graph<InlinedInstruction> threadOrder() {
				final Graph<InlinedInstruction> G = instructions(
						new Function<InlinedInstructionImpl, InlinedInstructionImpl>() {
							public InlinedInstructionImpl apply(
									InlinedInstructionImpl x) {
								return x;
							}
						},
						new Reduce<InlinedInstructionImpl, Graph<InlinedInstruction>>() {
							public Graph<InlinedInstruction> initial() {
								return SlowSparseNumberedGraph.make();
							}

							private InlinedInstructionImpl prev;
							
							private Map<SSACFG.BasicBlock, InlinedInstructionImpl> last = new HashMap<SSACFG.BasicBlock, InlinedInstructionImpl>();
				
							private void frontierInternal(CGNode node, ISSABasicBlock bb, Set<InlinedInstructionImpl> stuff) {
								if (last.containsKey(bb)) {
									assert bb.getNumber() != -1;
									stuff.add(last.get(bb)); 
								} else {
									Iterator<ISSABasicBlock> pbs = node.getIR().getControlFlowGraph().getPredNodes(bb);
									while (pbs.hasNext()) {
										frontierInternal(node, pbs.next(), stuff);
									}
								}
							}
							
							public Set<InlinedInstructionImpl> frontier(CGNode node, ISSABasicBlock bb) {
								Set<InlinedInstructionImpl> stuff = HashSetFactory.make();
								frontierInternal(node, bb, stuff);
								return stuff;
							}
							
							public Graph<InlinedInstruction> reduce(
									InlinedInstructionImpl x,
									Graph<InlinedInstruction> y) {
								y.addNode(x);
								if (prev != null && !prev.bb.isExitBlock()) {
									SSACFG.BasicBlock pbb = prev.bb;
									if (pbb == x.bb) {
										y.addEdge(prev, x);
									} else if (prev.node == x.node) {
										last.put(prev.bb, prev);
										for (InlinedInstructionImpl p : frontier(x.node, x.bb)) {
											y.addEdge(p, x);
										}
									} else {
										
									}
								} 
								prev = x;
								return y;
							}
						}, Recurse.Pre);
				
				InlinedInstruction s = start();
				G.addNode(s);
				for(InlinedInstruction x : G) {
					if (x != s && G.getPredNodeCount(x) == 0) {
						G.addEdge(s, x);
					}
				}
				
				InlinedInstruction e = end();
				G.addNode(e);
				for(InlinedInstruction x : G) {
					if (x != e && G.getSuccNodeCount(x) == 0) {
						G.addEdge(x, e);
					}
				}
				
				return GraphSlicer.project(G, new Predicate<InlinedInstruction>() {
					public boolean test(InlinedInstruction o) {
						return o.action() != null;
					}
				});
			}

			private final Object fakeArrayField = "fake array field";
			
			private Object getField(SSAInstruction inst) {
				if (inst instanceof SSAFieldAccessInstruction) {
					return cha.resolveField(((SSAFieldAccessInstruction)inst).getDeclaredField());
				} else if (inst instanceof SSAArrayReferenceInstruction) {
					 return fakeArrayField;
				} else {
					assert false;
					return null;
				}		
			}
			
			public Set<InlinedInstruction> visibleWrites(
					InlinedInstruction action) {
				Set<InlinedInstruction> result = new HashSet<InlinedInstruction>();
				CGNode node = action.cgNode();
				SSAInstruction inst = action.instruction();

				result.addAll(visibleWritesSameThread(action, node));
				
				Object field = getField(inst);
							
				for (CGNode otherRoot : threadRoots) {
					if (otherRoot != threadRoot) {
						WalaConcurrentInformation info = concurrentInformation(otherRoot);
						for (Iterator x = info.actions().iterator(); x.hasNext();) {
							InlinedInstruction oinst = (InlinedInstruction) x.next();
							if (oinst.action() == Action.NORMAL_WRITE || oinst.action() == Action.VOLATILE_WRITE) {
								SSAInstruction oi = oinst.instruction();
								if (field.equals(getField(oi))) {
									result.add(oinst);
								}
							}
						}
					}
				}

				return result;
			}

			private Set<InlinedInstruction> visibleWritesSameThread(
					InlinedInstruction readAction, CGNode node) {
				FieldSSATable ssa = cgNodeInformation(node).fieldSSA();
				return visibleWritesSameThread(ssa.getUses(readAction.instruction()), readAction.callStack(), readAction.cgNode());
			}
			
			private Set<InlinedInstruction> callers(Stack<CallSite> base) {
				if (base.empty()) {
					return Collections.emptySet();
				}
				
				Set<InlinedInstruction> callers = new HashSet<InlinedInstruction>();
				
				Stack<CallSite> callerStack = new LinkedStack<CallSite>();
				for (Iterator<CallSite> refs = ReverseIterator.reverse(base.iterator()); refs.hasNext(); ) {
					callerStack.push(refs.next());
				}
				CallSite cs = callerStack.pop();
				CGNode caller = cs.snd;
				CallSiteReference site = cs.fst;
				SSAAbstractInvokeInstruction insts[] = caller.getIR().getCalls(site);
				for(int i = 0; i < insts.length; i++) {
					int idx = getInstructionIndex(caller, insts[i]);
					callers.add(new InlinedInstructionImpl(caller, idx, caller.getIR().getControlFlowGraph().getBlockForInstruction(idx), insts[i], callerStack));
				}
				return callers;
			}

			private Set<InlinedInstruction> visibleWritesSameThread(
						int vs[], Stack<CallSite> stack, CGNode node) {
				Set<InlinedInstruction> result = new HashSet<InlinedInstruction>();
				FieldSSATable ssa = cgNodeInformation(node).fieldSSA();
				if (vs != null) {					
					DefUse fdu = ssa.getDefUse();
					for(int v : vs) {
						if (fdu.getDef(v) != null) {
							PointerKey pk = ssa.getField(v);
							Stack<SSAInstruction> s = new LinkedStack<SSAInstruction>();
							s.push(fdu.getDef(v));
							while (! s.empty()) {
								SSAInstruction def = s.pop();
								if (getInstructionIndex(node, def) != -1) {
									if (def instanceof SSAAbstractInvokeInstruction) {
										CallSiteReference site = ((SSAAbstractInvokeInstruction)def).getCallSite();
										Stack<CallSite> calleeStack = makeCalleeStack(stack, site, node);
										for(CGNode callee : callGraph.getPossibleTargets(node, site)) {
											int calleeV = cgNodeInformation(callee).fieldSSA().getExitValue(pk);
											result.addAll(visibleWritesSameThread(new int[]{calleeV}, calleeStack, callee));
										}
									} else {
										result.add(new InlinedInstructionImpl(node,
											getInstructionIndex(node, def), 
											node.getIR().getControlFlowGraph().getBlockForInstruction(getInstructionIndex(node, def)),
											def, 
											stack));
									}
								} else {
									assert def instanceof SSAPhiInstruction;
									for(int i = 0; i < def.getNumberOfUses(); i++) {
										SSAInstruction x = fdu.getDef(def.getUse(i));
										if (x != null) {
											s.push(x);
										}
									}
								}
							}
						} else {
							for(InlinedInstruction callerInst : callers(stack)) {
								result.addAll(visibleWritesSameThread(callerInst, callerInst.cgNode()));
							}
						}
					}
				}
				
				return result;
			}

			public Graph<InlinedInstruction> atomicRegions() {
				return GraphSlicer.prune(threadOrder(),
						new Predicate<InlinedInstruction>() {
							public boolean test(InlinedInstruction inst) {
								return isSpecialAtomic(inst.cgNode()
										.getMethod());
							}
						});
			}

			public Collection<LocalPointerKey> locksFor(
					InlinedInstruction action) {
				return null;
			}

			public InlinedInstruction end() {
				return new InlinedInstructionImpl(threadRoot,
						Integer.MAX_VALUE, null, null, new LinkedStack<CallSite>());
			}

			public CGNode root() {
				return threadRoot;
			}

			public InlinedInstruction start() {
				return new InlinedInstructionImpl(threadRoot,
						Integer.MIN_VALUE, null, null, new LinkedStack<CallSite>());
			}

			public void computeRelevantStuffInSlice() {
				final HeapModel HM = pointerAnalysis.getHeapModel();
				instructions(
					new Function<InlinedInstructionImpl, InlinedInstructionImpl>() {
						public InlinedInstructionImpl apply(InlinedInstructionImpl object) {
							return object;
						}
					},
					new Reduce<InlinedInstructionImpl, Object>() {
						public Object initial() {
							return null;
						}
						public Object reduce(InlinedInstructionImpl ii, Object y) {
							SSAInstruction inst = ii.instruction();
							CGNode node = ii.cgNode();

							if (!(inst instanceof SSAAbstractInvokeInstruction)) {
								int[] uses = cgNodeInformation(node).fieldSSA().getUses(
										inst);
								if (uses != null) {
									for (int j = 0; j < uses.length; j++) {
										possiblyAccessedFields.add(cgNodeInformation(node)
												.fieldSSA().getField(uses[j]));
									}
								}
							}

							if (inst instanceof SSANewInstruction) {
								OrdinalSet<? extends InstanceKey> os = getPointsTo(HM
										.getPointerKeyForLocal(node, inst.getDef(0)));
								assert os.size() == 1 : "found unexpected points to set " + os;
								InstanceKey ik = os.iterator().next();

								if (!allocatedObjectCounts.containsKey(ik)) {
									allocatedObjectCounts.put(ik, new Integer(1));
								} else {
									allocatedObjectCounts.put(ik, new Integer(
											1 + ((Integer) allocatedObjectCounts.get(ik))
													.intValue()));
								}
							}

							return null;
						}
					},
					Recurse.Post);
				}
			
			public String toString() {
				final StringBuilder b = new StringBuilder();
				b.append("---------CONCURRENT INFORMATION FOR " + root().getMethod().getSignature() + "---------");
				b.append(prettyPrint(root().getIR(), 1));
				final Set<InlinedInstruction> actions = actions();
				b.append("\n INLINED INSTRUCTIONS:");
				if (actions.isEmpty())
					b.append(" { }\n");
				else {
					b.append(" {\n");
					b.append(prettyPrint(actions, 2));
					b.append(" }\n");
				}
				b.append("\n THREAD ORDER:");
				final Graph<InlinedInstruction> to = threadOrder();
				if (to.getNumberOfNodes()==0)  
					b.append(" { }\n");
				else {
					b.append(" {");
					b.append(prettyPrint(to, 2));
					b.append(" }\n");
				}
				return b.toString();
			}
		};
	}
	
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("THREADS: ");
		
		final Graph<CGNode> threads = threads();
	
		b.append(threads);
		
		b.append("\n");
		if (threads.getNumberOfNodes()>1) { 
			final Collection<CGNode> roots = InferGraphRoots.inferRoots(threads);
			assert roots.size() == 1;
			for(final Iterator<CGNode> itr = DFS.iterateDiscoverTime(threads, roots.iterator().next()); itr.hasNext();) {
				final CGNode thread = itr.next();
				final WalaConcurrentInformation tInfo = concurrentInformation(thread);
				b.append("\n\n");
				b.append(tInfo);
			}
		}
		return b.toString();
	}
}

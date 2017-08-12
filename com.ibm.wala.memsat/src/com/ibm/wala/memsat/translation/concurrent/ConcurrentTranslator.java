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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.concurrent.Justification;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.HeapExpression;
import com.ibm.wala.memsat.translation.Environment;
import com.ibm.wala.memsat.translation.Environment.Frame;
import com.ibm.wala.memsat.translation.MethodTranslation;
import com.ibm.wala.memsat.translation.TranslationWarning;
import com.ibm.wala.memsat.translation.Translator;
import com.ibm.wala.memsat.util.Nodes;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Set;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * A translator for concurrent Wala programs.
 * 
 * @author Emina Torlak
 */
public final class ConcurrentTranslator {
	private ConcurrentTranslator() {}
	
	/**
	 * Returns the translation of the method described by the 
	 * given wala information instance, with respect to the 
	 * specified translation options.
	 * @requires info.threads() > 1
	 * @return { t : Translation | t.info = info and t.options = options}
	 */
	public static ConcurrentTranslation translate(final WalaInformation info, final Options options) { 
		
	  /*
		System.out.println("RELEVANT CLASSES: " + info.relevantClasses());
		System.out.println("RELEVANT FIELDS: " + info.relevantFields());
		System.out.println("THREADS: " + info.threads());
		*/
	  
		final ConcurrentMemoryHandler handler = new ConcurrentMemoryHandler(info,options);
		final Map<CGNode, MethodTranslation> transls = translate(handler);
//		System.out.println(handler);
		final ConcurrentProgram prog = new ConcurrentProgram(handler, transls);
		final Justification just = options.memoryModel().justify(prog);
		final Set<TranslationWarning> warnings = new LinkedHashSet<TranslationWarning>();
		for(MethodTranslation transl : transls.values()) { 
			warnings.addAll(transl.warnings());
		}
		//System.out.println(Strings.prettyPrint(just.formula().and(handler.factory.invariants()), 2));
		return new ConcurrentTranslation(handler.factory.base(), 
					Nodes.simplify(just.formula().and(handler.factory.invariants()), just.bounds()), just, warnings);
	}
	
	/**
	 * Translates the threads in handler.factory.base.info.threads and returns the result.
	 * In particular, let t1, t2 and t3 be threads such that t1->t3 + t2->t3 in handler.base.info.threads.
	 * Suppose that there are two fields f1 and f2 read by t3 such that f1 is only written by t1 (and no other thread) and f2 is only
	 * written by t2 (and no other thread).  Then t3 is translated in an environment that is initialized 
	 * with the final value of f1 from the translation of t1 and the final value of f2 from the translation of t2.
	 * This method assumes that if a field is f is written by more than one thread then every access to that field
	 * will appear as an InlinedInstruction in each accessing thread's {@linkplain WalaConcurrentInformation#actions() actions} set.
	 * @requires handler.factory.base().info().threads().equals(threads)
	 * @return a map from each node in the given graph to its translation
	 */
	private static Map<CGNode, MethodTranslation> translate(ConcurrentMemoryHandler handler) { 
		final Map<CGNode, MethodTranslation> transls = new LinkedHashMap<CGNode, MethodTranslation>();
		final WalaInformation info = handler.factory.base().info();
		final Graph<CGNode> threads = info.threads();
		
		for(Iterator<CGNode> itr = DFS.iterateDiscoverTime(threads, root(threads)); itr.hasNext(); ) { 
			final CGNode node = itr.next();
			final Set<PointerKey> seqFields = sequentialFields(node, info);
			final Map<PointerKey,HeapExpression<?>> override;
			
			if (seqFields.isEmpty()) {
				override = Collections.emptyMap();
			} else {
				override = new LinkedHashMap<PointerKey, HeapExpression<?>>();
				for(Iterator<? extends CGNode> preds = threads.getPredNodes(node); preds.hasNext(); ) { 
					final CGNode pred = preds.next();
					final Frame frame = transls.get(pred).frame();
					final FieldSSATable fieldSSA = info.cgNodeInformation(pred).fieldSSA();
					for(Iterator<PointerKey> fields = fieldSSA.getFields(); fields.hasNext(); ) { 
						final PointerKey field = fields.next();
						if (seqFields.contains(field)) { 
							final HeapExpression<?> initVal = frame.heapUse(fieldSSA.getEntryValue(field));
							final HeapExpression<?> finalVal = frame.heapUse(fieldSSA.getExitValue(field));
							if (!initVal.equals(finalVal)) { 
								assert !override.containsKey(field);
								override.put(field, finalVal);
							}
						}
					}
				}
			}
			transls.put(node, Translator.translate((new Environment(handler.factory.base())).push(node, override), handler));
		}
		
		return transls;
	}
	
	/**
	 * Returns the set of field pointer keys that appear in the node's {@linkplain FieldSSATable field SSA table}
	 * and that are neither used nor defined by an InlinedInstruction
	 * in info.concurrentInformation(node).actions().
	 * @requires node in info.threads
	 * @return set of field pointer keys that appear in the node's {@linkplain FieldSSATable field SSA table}
	 * and that are neither used nor defined by an InlinedInstruction
	 */
	private final static Set<PointerKey> sequentialFields(CGNode node, WalaInformation info) { 
		
		final FieldSSATable fieldSSA = info.cgNodeInformation(node).fieldSSA();
		final Set<PointerKey> sequential = Iterator2Set.toSet(fieldSSA.getFields());
		for(InlinedInstruction inst : info.concurrentInformation(node).actions()) { 
			if (node.equals(inst.cgNode())) { 
				final SSAInstruction obj = inst.instruction();
				if (obj instanceof SSAFieldAccessInstruction) { 
					for(int use : fieldSSA.getUses(inst.instruction())) { 
						sequential.remove(fieldSSA.getField(use));
					}
					for(int def : fieldSSA.getDefs(inst.instruction())) { 
						sequential.remove(fieldSSA.getField(def));
					}
				} else if (obj instanceof SSAArrayReferenceInstruction) { 
					final int[] uses = fieldSSA.getUses(obj);
					final int[] defs = fieldSSA.getDefs(obj);
					assert uses.length == 2;
					assert defs.length <= 1;
					sequential.remove(fieldSSA.getField(uses[0]));
					if (defs.length>0)
						sequential.remove(fieldSSA.getField(defs[0]));
				}
			}
		}
		return sequential;
	}
	
}

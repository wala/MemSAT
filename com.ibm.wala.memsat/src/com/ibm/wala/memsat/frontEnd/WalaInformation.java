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
package com.ibm.wala.memsat.frontEnd;

import java.util.Set;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

/**
 * Encapsulates the result of running the {@linkplain WalaEngine} 
 * on a given {@linkplain Graph graph} of {@linkplain MethodReference methods} and their
 * corresponding {@linkplain AnalysisScope analysis scope}.
 * 
 * @specfield options: Options // miniatur options
 * @specfield callGraph: CallGraph // call graph for the entrypoints specified by the nodes in this.threads graph
 * @specfield threads: Graph<CGNode> // partial (execution) order on the entrypoints of this.callGraph 
 * @specfield initThread: CGNode // method executed by the initialization thread
 * @invariant all t: threads.nodes | t in initThread.*(threads.edges)
 * 
 * @author Emina Torlak
 */
public interface WalaInformation {
	
	/**
	 * Returns the sliced call graph for all threads. 
	 * @return sliced call graph for all threads.
	 */
	public abstract CallGraph callGraph();
	
	/**
	 * Returns the analysis options used for constructing the sliced call graph.
	 * @return analysis options used for constructing the sliced call graph.
	 */
	public abstract AnalysisOptions analysisOptions();
	
	/**
	 * Returns the graph of entrypoints for the threads in this analysis problem.
	 * @return this.threads
	 */
	public abstract Graph<CGNode> threads();
		
	/**
	 * Returns the set of instance equivalent classes that appear in the sliced call graph
	 * of some analyzed thread.  Each returned equivalence class C is allocated a distinct 
	 * set of atoms (of cardinality {@linkplain #cardinality(InstanceKey) cardinality(C)})
	 * by the Miniatur translator, but only the classes that appear in constructor or 
	 * instance of statements are explicitly represented as unary relations in the final formula.
	 * @return the set of instance equivalnce classes C such that a member of each C appears in the sliced 
	 * call graph of some analyzed thread.  
	 */
	public abstract Set<InstanceKey> relevantClasses();
	
	/**
	 * Returns the number of instances (allocated instances plus  
	 * this.options.openWorldScopeSize, if <tt>eqClass</tt> reaches into
	 * the open world) of the given equivalence class 
	 * used in all analyzed threads. 
	 * @requires eqClass in this.relevantClasses()
	 * @return the number of members of the given equivalence class
	 * in the sliced call graphs of all analyzed threads. 
	 */
	public abstract int cardinality(InstanceKey eqClass);
	
	/**
	 * Returns true if the given equivalence class of instances reaches
	 * into the open world.
	 * @return true if the given equivalence class of instances reaches
	 * into the open world.
	 */
	public abstract boolean openWorldType(InstanceKey eqClass);
	
	/**
	 * Returns the set of pointer equivalence classes for fields (members of 
	 * {@linkplain #relevantClasses() this.relevantClasses()}) 
	 * used in the (sliced) call graph of some analyzed thread.  The returned pointers
	 * are explicitly modeled as relations by the Miniatur translator.  
	 * @return the set of pointer equivalence classes F such that a member of each F 
	 * corresponds to a field used in the sliced call graph of some analyzed thread. 
	 */
	public abstract Set<PointerKey> relevantFields();
	
	/**
	 * Returns the set of instance equivalence classes (subset of this.relevantClasses())
	 * to which the given pointer key points.
	 * @requires ptrKey in this.relevantFields() or 
	 *  some t: [0..this.threads()), n: this.sequentialInformation(t).cgNodes, v: int | 
	 *   ptrKey = this.sequentialInformation(t).cgNodeInformation(n).pointerKeyFor(v)
	 * @return the set of instance equivalence classes (subset of this.relevantClasses())
	 * to which the given pointer points.
	 */
	public abstract Set<InstanceKey> pointsTo(PointerKey ptrKey);
	
	/**
	 * Returns the WalaCGNodeInformation for the given node.
	 * @requires node in this.callGraph().nodes
	 * @return the WalaCGNodeInformation for the given node.
	 * @see WalaCGNodeInformation
	 */
	public abstract WalaCGNodeInformation cgNodeInformation(CGNode node);
	
	/**
	 * Returns a {@linkplain WalaConcurrentInformation} for the thread with the given 
	 * id, which defines specified thread's concurrent behavior with respect to other
	 * analyzed threads.  This method throws an IllegalStateException if this analysis
	 * problem involves only one thread.  If the given cgNode represents the initialization
	 * thread (this.initThread), the set of actions() of the returned WalaConcurrentInformation
	 * will consists only of start, end and write actions. 
	 * @requires #this.threads.nodes > 1
	 * @requires cgNode in this.threads.nodes
	 * @return WalaConcurrentInformation for the thread with the given entrypoint
	 * @throws IllegalStateException - !this.concurrent()
	 * @see {@linkplain WalaConcurrentInformation}
	 */
	public abstract WalaConcurrentInformation concurrentInformation(CGNode entry);
}

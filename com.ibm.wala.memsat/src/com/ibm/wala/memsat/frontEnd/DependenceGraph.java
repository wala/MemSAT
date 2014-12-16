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

import com.ibm.wala.util.graph.NumberedGraph;


/**
 * Provides a data/control/etc. dependence view of an underlying Wala graph
 * (e.g. a call graph, control flow graph, etc.).
 * 
 * @specfield underlyingGraph: NumberedGraph<N> // underlying Wala graph
 * @specfield nodes: set N // nodes in this dependence graph, derived from the nodes in the underlying Wala graph
 * @specfield edges: nodes -> nodes // dependendence relation between nodes
 * @specfield labels: edges -> N // edge labels (optional)
 * @author Emina Torlak
 */
public interface DependenceGraph<N> extends NumberedGraph<N> {
	
	/**
	 * Returns the set of edge labels, if any, for the edges in the underlying Wala graph
	 * that cause the edge given by <tt>source</tt> and <tt>sink</tt> in this dependence graph. 
	 * If this dependence graph contains no labels for the given edge, returns the empty set.  
	 * @requires source -> sink in this.edges
	 * @return this.labels[source, sink]
	 */
	public abstract Set<N> edgeLabels(N source, N sink);
	
}

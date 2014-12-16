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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import kodkod.util.collections.Containers;

import com.ibm.wala.util.graph.Graph;

/**
 * A simple graph implementation based on a linked hash map.
 * Best used for representing graphs with a small number of nodes.
 * 
 * @author etorlak
 */
public final class LinkedHashGraph<T> implements Graph<T> {
	private final Map<T, Set<T>> edges; 

	/**
	 * Constructs an empty hash graph.
	 */
	public LinkedHashGraph() {
		edges = new LinkedHashMap<T, Set<T>>();
	}
	
	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
	 */
	public void removeNodeAndEdges(T N) throws UnsupportedOperationException {
		if (containsNode(N)) {
			removeIncomingEdges(N);
			edges.remove(N);
		}
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
	 */
	public void addNode(T N) {
		if (!containsNode(N)) { 
			edges.put(N, new LinkedHashSet<T>(3));
		}
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
	 */
	public boolean containsNode(T N) {
		return edges.containsKey(N);
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
	 */
	public int getNumberOfNodes() {
		return edges.size();
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.NodeManager#iterator()
	 */
	public Iterator<T> iterator() {
		return Collections.unmodifiableSet(edges.keySet()).iterator();
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
	 */
	public void removeNode(T n) {
		removeNodeAndEdges(n);
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
	 */
	public void addEdge(T src, T dst) {
		addNode(src);
		addNode(dst);
		edges.get(src).add(dst);
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
	 */
	public int getPredNodeCount(T N) {
		if (containsNode(N)) { 
			int p = 0;
			for(Set<T> succs : edges.values()) { 
				if (succs.contains(N))
					p++;
			}
			return p;
		}
		return 0;
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
	 */
	public Iterator<T> getPredNodes(final T N) {
		if (containsNode(N)) { 
			return new Iterator<T>() {
				final Iterator<T> itr = edges.keySet().iterator();
				T next, last;
				
				public boolean hasNext() {
					while(next==null && itr.hasNext()) { 
						final T node = itr.next();
						if (edges.get(node).contains(N)) { 
							next = node;
						}
					}
					return next != null;
				}

				public T next() {
					if (!hasNext()) throw new NoSuchElementException();
					last = next;
					next = null;
					return last;
				}

				public void remove() {
					if (last==null) throw new IllegalStateException();
					removeAllIncidentEdges(last);
					itr.remove();
					last = null;
				}
				
			};
		}
		return Containers.emptyIterator();
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
	 */
	public int getSuccNodeCount(T N) {
		return containsNode(N) ? edges.get(N).size() : 0;
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public Iterator<T> getSuccNodes(T N) {
		return containsNode(N) ? edges.get(N).iterator() : Collections.EMPTY_SET.iterator();
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#hasEdge(java.lang.Object, java.lang.Object)
	 */
	public boolean hasEdge(T src, T dst) {
		return containsNode(src) && edges.get(src).contains(dst);
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
	 */
	public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
		removeIncomingEdges(node);
		removeOutgoingEdges(node);
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#removeEdge(java.lang.Object, java.lang.Object)
	 */
	public void removeEdge(T src, T dst) throws UnsupportedOperationException {
		if (containsNode(src)) { 
			edges.get(src).remove(dst);
		}
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#removeIncomingEdges(java.lang.Object)
	 */
	public void removeIncomingEdges(T node) throws UnsupportedOperationException {
		if (containsNode(node)) { 
			for(Set<T> succs : edges.values()) { 
				succs.remove(node);
			}
		}
	}

	/** {@inheritDoc}
	 * @see com.ibm.wala.util.graph.EdgeManager#removeOutgoingEdges(java.lang.Object)
	 */
	public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
		if (containsNode(node)) { 
			edges.get(node).clear();
		}
	}

	public String toString() { 
		return Strings.prettyPrint(this);
	}
}

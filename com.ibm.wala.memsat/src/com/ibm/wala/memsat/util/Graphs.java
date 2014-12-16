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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;

/**
 * Provides a set of utility methods for working with {@linkplain Graph graphs}.
 * 
 * @author etorlak
 */
public final class Graphs {
	private Graphs() {}
	
	/**
	 * Returns the roots of the given graph.
	 * @return roots of the given graph
	 */
	public static final <T> Set<T> roots(Graph<T> graph) { 
		final Set<T> ret = new LinkedHashSet<T>();
		for(T n : graph) { 
			ret.add(n);
		}
		for(T n : graph) { 
			for(Iterator<? extends T> itr = graph.getSuccNodes(n); itr.hasNext(); ) { 
				ret.remove(itr.next());
			}
		}
		return ret;
	}
	
	/**
	 * Returns a root of the given graph. 
	 * @requires some n: graph.nodes | no (graph.edges).n
	 * @return a root of the given graph
	 */
	public static final <T> T root(Graph<T> graph) { 
		final Set<T> roots = roots(graph);
		assert roots.size()>=1;
		return roots.iterator().next();
	}
	
	/**
	 * Returns a new Graph with the given nodes and no edges.
	 * @return a new Graph with the given nodes and no edges.
	 */
	public static final <T> Graph<T> graph(T...nodes) { 
		final Graph<T> ret = new LinkedHashGraph<T>();
		for(T node : nodes)
			ret.addNode(node);
		return ret;
	}
	
	/**
	 * Returns a new Graph with the given nodes and no edges.
	 * @return a new Graph with the given nodes and no edges.
	 */
	public static final <T> Graph<T> graph(Collection<? extends T> nodes) { 
		final Graph<T> ret = new LinkedHashGraph<T>();
		for(T node : nodes)
			ret.addNode(node);
		return ret;
	}
	
	/**
	 * Returns a set view of the nodes in the given graph.
	 * @return a set view of the nodes in the given graph.
	 */
	public static final <T> Set<T> nodes(final Graph<T> g) { 
		return new AbstractSet<T>() {
			@Override
			public Iterator<T> iterator() { return g.iterator(); }
			@Override
			public int size() { return g.getNumberOfNodes(); }
			@SuppressWarnings("unchecked")
			@Override
			public boolean contains(Object o) { 
				return g.containsNode((T) o);
			}
		};
	}
	
	
	/**
	 * Returns a collection view of the successors of the node in the graph g.
	 * @return a collection view of the successors of the node in the graph g.
	 */
	public static final <T> Collection<T> successors(final T n, final Graph<T> g) { 
		return new AbstractCollection<T>() {
			@Override
			public Iterator<T> iterator() {
				return (Iterator<T>)g.getSuccNodes(n);
			}
			@Override
			public int size() {	return g.getSuccNodeCount(n); }
		};
	}
	
	/**
	 * Returns a collection view of the predecessors of the node in the graph g.
	 * @return a collection view of the predecessors of the node in the graph g.
	 */
	public static final <T> Collection<T> predecessors(final T n, final Graph<T> g) { 
		return new AbstractCollection<T>() {
			@Override
			public Iterator<T> iterator() {
				return (Iterator<T>)g.getPredNodes(n);
			}
			@Override
			public int size() {	return g.getPredNodeCount(n); }
		};
	}
	
	/**
	 * Returns a graph that is the transitive closure of g.
	 * @return a graph that is the transitive closure of g.
	 */
	public static final <T> Graph<T> transitiveClosure(Graph<T> g) { 
		final Graph<T> ret = new LinkedHashGraph<T>();
		for(T n : g) { 
			ret.addNode(n); 
			for(Iterator<? extends T> succs = g.getSuccNodes(n); succs.hasNext(); ) { 
				ret.addEdge(n, succs.next());
			}
		}
		for(T nk : g) { 
			for(T ni : g) { 
				for(T nj : g) { 
					if (ret.hasEdge(ni, nk) && ret.hasEdge(nk, nj))
						ret.addEdge(ni, nj);
				}
			}
		}
		return ret;
	}

	 
	/**
	 * Returns a graph that is the reflexive transitive closure of g.
	 * @return a graph that is the reflexive transitive closure of g.
	 */
	public static final <T> Graph<T> reflexiveTransitiveClosure(Graph<T> g) { 
		final Graph<T> ret = transitiveClosure(g);
		for(T n : g) { 
			ret.addEdge(n, n);
		}
		return ret;
	}
	
	/**
	 * Returns a restriction of the given graph onto the nodes accepted by the given filter.
	 * @return a restriction of the given graph onto the nodes accepted by the given filter.
	 */
	public static final <T> Graph<T> restrict(Graph<T> g, Predicate<T> filter) { 
		final Graph<T> ret = new LinkedHashGraph<T>();
		for(T n : g) { 
			if (filter.test(n)) { 
				ret.addNode(n);
				for(Iterator<? extends T> succs = g.getSuccNodes(n); succs.hasNext(); ) { 
					final T succ = succs.next();
					if (filter.test(succ)) { 
						ret.addEdge(n, succ);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns a new graph whose edge set consists of the edges in g, plus self-edges on each node in g.
	 * @return a new graph whose edge set consists of the edges in g, plus self-edges on each node in g.
	 */
	public static final <T> Graph<T> reflexive(final Graph<T> g) {
		final Graph<T> ret = union(Collections.singleton(g));
		for(T n : g) { 
			ret.addEdge(n, n);
		}
		return ret;
	}
	
	/**
	 * Returns a new graph whose edge set consists of the edges in g, minus any self-edges.
	 * @return a new graph whose edge set consists of the edges in g, minus any self-edges.
	 */
	public static final <T> Graph<T> irreflexive(final Graph<T> g) { 
		final Graph<T> ret = union(Collections.singleton(g));
		for(T n : g) { 
			ret.removeEdge(n, n);
		}
		return ret;
	}
	
	/**
	 * Returns a new graph that is the union of the nodes and edges in the given 
	 * collection of graphs.
	 * @return a new graph that is the union of the nodes and edges in the given 
	 * collection of graphs.
	 */
	public static final <T> Graph<T> union(Collection<Graph<T>> graphs) { 
		final Graph<T> ret = new LinkedHashGraph<T>();
		for(Graph<T> g : graphs) { 
			for(T n : g) { 
				ret.addNode(n);
				for(Iterator<? extends T> succs = g.getSuccNodes(n); succs.hasNext(); ) { 
					ret.addEdge(n, succs.next());
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns true if the given graphs are logically equal.
	 * @return true if the given graphs are logically equal
	 */
	public static <T> boolean  equal(Graph<T> g1, Graph<T> g2) { 
		if (g1==g2) return true;
		else if (g1==null)
			return g2==null;
		else if (nodes(g1).equals(nodes(g2))) { 
			for(T n : g1) { 
				if (!(new HashSet<T>(successors(n, g1))).equals(new HashSet<T>(successors(n, g2))))
					return false;
			}
			return true;
		} else 
			return false;
	}
	
 }

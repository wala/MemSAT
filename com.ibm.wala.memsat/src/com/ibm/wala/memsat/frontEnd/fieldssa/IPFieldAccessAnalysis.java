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
package com.ibm.wala.memsat.frontEnd.fieldssa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.ComposedIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.IteratorPlusOne;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;

public class IPFieldAccessAnalysis implements IKilldallFramework {

  private final CallGraph CG;
  private final Collection interestingNodes;
  private final FieldAccessesFactory localAccesses;

  private class FieldAccessesVariable implements IVariable {
    private int nodeId = -1;
    private int orderNumber;
    private Set readReferences;
    private Set writeReferences;

    public int getGraphNodeId() { return nodeId; }
     
    public void setGraphNodeId(int i) { nodeId = i; }

    public int getOrderNumber() { return orderNumber; }
      
    public void setOrderNumber(int i) { orderNumber = i; }

    public void copyState(IVariable v) {
      FieldAccessesVariable other = (FieldAccessesVariable)v;
      this.orderNumber = other.orderNumber;

      this.readReferences = new HashSet();
      this.readReferences.addAll( other.readReferences );

      this.writeReferences = new HashSet();
      this.writeReferences.addAll( other.writeReferences );
    }

    private boolean addReads(Set newReferences) {
      if (newReferences == null) return false;
      if (readReferences == null) {
	if (! newReferences.isEmpty()) {
	  readReferences = new HashSet();
	  readReferences.addAll( newReferences );
	  return true;
	} else {
	  return false;
	}
      } else {
	if (readReferences.containsAll( newReferences )) {
	  return false;
	} else {
	  readReferences.addAll( newReferences );
	  return true;
	}
      }
    }

    private boolean addWrites(Set newReferences) {
      if (newReferences == null) return false;
      if (writeReferences == null) {
	if (! newReferences.isEmpty()) {
	  writeReferences = new HashSet();
	  writeReferences.addAll( newReferences );
	  return true;
	} else {
	  return false;
	}
      } else {
	if (writeReferences.containsAll( newReferences )) {
	  return false;
	} else {
	  writeReferences.addAll( newReferences );
	  return true;
	}
      }
    }
  }

  private class NodeTransferFunction extends UnaryOperator {
    private final CGNode node;
    
    private NodeTransferFunction(CGNode node) {
      this.node = node;
    }

    public String toString() {
      return "FieldAccessAnalysis transfer for " + node;
    }

    public int hashCode() {
      return node.hashCode();
    }

    public boolean equals(Object o) {
      return (o instanceof NodeTransferFunction)
	                   &&
	     ((NodeTransferFunction)o).node.equals(node);
    }

    public byte evaluate(IVariable lhs, IVariable rhs) {
      FieldAccessesVariable lv = (FieldAccessesVariable)lhs;
      FieldAccessesVariable rv = (FieldAccessesVariable)rhs;

      IR ir = node.getIR();
      Set localReads = new HashSet();
      Set localWrites = new HashSet();
      if (ir != null) {
    	FieldAccesses accesses = localAccesses.get(node);
	for(Iterator insts = getInstructions(node); insts.hasNext();) {
	  SSAInstruction inst = (SSAInstruction) insts.next();

	  PointerKey[] defs = accesses.getDefs(inst);
	  for(int f = 0; f < defs.length; f++) {
	    localWrites.add( defs[f] );
	  }

	  PointerKey[] uses = accesses.getUses(inst);
	  for(int f = 0; f < uses.length; f++) {
	    localReads.add( uses[f] );
	  }
	}
      }

      boolean changed = false;
      changed |= lv.addReads( rv.readReferences );
      changed |= lv.addReads( localReads );
      changed |= lv.addWrites( rv.writeReferences );
      changed |= lv.addWrites( localWrites );
      if (changed)
	return CHANGED;
      else
	return NOT_CHANGED;      
    }
  }

  private UnaryOperator identityTransferFunction = new UnaryOperator() {

    public boolean isIdentity() {
      return true;
    }
       
    public String toString() {
      return "FieldAccessAnalysis identity transfer function";
    }

    public int hashCode() {
      return 86587567;
    }

    public boolean equals(Object o) {
      return o == this;
    }
  
    public byte evaluate(IVariable lhs, IVariable rhs) {
      FieldAccessesVariable lv = (FieldAccessesVariable)lhs;
      FieldAccessesVariable rv = (FieldAccessesVariable)rhs;
      boolean newReads = lv.addReads(rv.readReferences);
      boolean newWrites = lv.addWrites(rv.writeReferences);
      if (newReads || newWrites)
	return CHANGED;
      else
	return NOT_CHANGED;
    }

  };

  private AbstractMeetOperator unionMeetOperator = new AbstractMeetOperator() {
    public byte evaluate(IVariable lhs, IVariable[] rhs) {
      FieldAccessesVariable lv = (FieldAccessesVariable)lhs;
      boolean newReads = false;
      boolean newWrites = false;

      for(int i = 0; i < rhs.length; i++) {
	FieldAccessesVariable rv = (FieldAccessesVariable)rhs[i];
	newReads |= lv.addReads(rv.readReferences);
	newWrites |= lv.addWrites(rv.writeReferences);
      }

      if (newReads || newWrites)
	return CHANGED;
      else
	return NOT_CHANGED;
    }

    public int hashCode() {
      return 143464675;
    }

    public boolean equals(Object o) {
      return o == this;
    }
  
    public String toString() {
      return "FieldAccessesAnalysis meet operator";
    }  
  };

  private final ITransferFunctionProvider fieldAccessFunctions =
    new ITransferFunctionProvider() {
	
      public UnaryOperator getNodeTransferFunction(Object node) {
	if (node instanceof Pair) {
	  return identityTransferFunction;
	} else {
	  return new NodeTransferFunction((CGNode)node);
	}
      }

      public boolean hasNodeTransferFunctions() {
	return true;
      }

      public UnaryOperator getEdgeTransferFunction(Object src, Object dst) {
	Assertions.UNREACHABLE();
	return null;
      }

      public boolean hasEdgeTransferFunctions() {
	return false;
      }

      public AbstractMeetOperator getMeetOperator() {
	return unionMeetOperator;
      }
  };

  private class IPFieldAccessSolver extends DataflowSolver {

    protected IVariable makeNodeVariable(Object n, boolean IN) {
      return new FieldAccessesVariable();
    }

    protected IVariable makeEdgeVariable(Object src, Object dst) {
      Assertions.UNREACHABLE();
      return null;
    }

    private IPFieldAccessSolver(IKilldallFramework problem) {
      super(problem);
    }

	@Override
	protected IVariable[] makeStmtRHS(int size) {
		return new IVariable[size];
	}
  }

  protected Iterator getInstructions(CGNode node) {
      return node.getIR().iterateAllInstructions();
  }
      
  private Iterator iterateNodes() {
    if (interestingNodes != null)
      return interestingNodes.iterator();
    else
      return CG.iterator();
  }

  private boolean containsNode(CGNode node) {
    if (interestingNodes != null)
      return interestingNodes.contains(node);
    else
      return CG.containsNode(node);
  }

  public ITransferFunctionProvider getTransferFunctionProvider() {
    return fieldAccessFunctions;
  }

  public Graph getFlowGraph() {
    return new AbstractGraph() {
      private final NodeManager nodeManager = new NodeManager() {

        public Stream stream() {
          Iterable iterable = new Iterable() {
             public Iterator iterator() {
              return nodeManager.iterator();
            }
          };
          return StreamSupport.stream(iterable.spliterator(), false);
        }

        public Iterator iterator() {
	  return new ComposedIterator(IPFieldAccessAnalysis.this.iterateNodes()) {
	    public Iterator makeInner(Object outer) {
	      final CGNode node = (CGNode)outer;
	      return 
	        IteratorPlusOne.make(
		  new MapIterator(
		    node.iterateCallSites(),
		    new Function() {
		      public Object apply(Object object) {
			return Pair.make(node, object);
		      }
		    }),
		  node);
	    }
	  };	      
	}

	public int getNumberOfNodes() {
	  int count = 0;
	  for(Iterator nodes = iterateNodes(); nodes.hasNext(); ) {
	    CGNode node = (CGNode) nodes.next();
	    count++;
	    for(Iterator sites = node.iterateCallSites(); sites.hasNext(); ) {
	      CallSiteReference site = (CallSiteReference)sites.next();
	      count++;
	    }
	  }

	  return count;
	}

	public void addNode(Object n) {
	  Assertions.UNREACHABLE();
	}

        public void removeNode(Object n) {
	  Assertions.UNREACHABLE();
	}

	public boolean containsNode(Object N) {
	  if (N instanceof CGNode){
	    return containsNode((CGNode)N);
	  } else if (N instanceof Pair) {
	    Pair p = (Pair)N;
	    if (containsNode((CGNode)p.fst)) {
	      return IteratorUtil.contains(((CGNode)p.fst).iterateCallSites(), (CallSiteReference)p.snd);
	    } else {
	      return false;
	    }
	  } else {
	    return false;
	  }
	}
      };

      private final EdgeManager edgeManager = new EdgeManager() {
        public Iterator getSuccNodes(Object N) {
	  if (N instanceof Pair) {
	    return new NonNullSingletonIterator(((Pair)N).fst);
	  } else {
	    assert N instanceof CGNode;
	    final CGNode node = (CGNode)N;
	    return new ComposedIterator(iterateNodes()) {
	      public Iterator makeInner(Object outer) {
		final CGNode caller = (CGNode)outer;
		return 
		  new MapIterator(
		    new FilterIterator(
		      caller.iterateCallSites(),
		      new Predicate() {
			public boolean test(Object site) {
			  return CG
			    .getPossibleTargets(caller, (CallSiteReference)site)
			    .contains(node);
			}
		      }),
		    new Function() {
		      public Object apply(Object site) {
			return Pair.make(caller, site);
		      }
		    });
	      }
	    };
	  } 
	}

	public int getSuccNodeCount(Object N) {
	  return IteratorUtil.count( getSuccNodes(N) );
	}

	public Iterator getPredNodes(final Object N) {
	  if (N instanceof CGNode) {
	    return new MapIterator(
	      ((CGNode)N).iterateCallSites(),
	      new Function() {
		public Object apply(Object site) {
		  return Pair.make(N, site);
		}
	      });
	  } else {
	    assert N instanceof Pair;
	    Pair p = (Pair)N;
	    return 
	      CG.getPossibleTargets((CGNode)p.fst, (CallSiteReference)p.snd)
		.iterator();
	  }
	}

	public int getPredNodeCount(Object N) {
	  return IteratorUtil.count( getPredNodes(N) );
	}

	public boolean hasEdge(Object N, Object dst) {
	  if (N instanceof CGNode) {
	    return (dst instanceof Pair) &&
	      ((Pair)dst).fst.equals(N) &&
	      IteratorUtil.contains(
	        ((CGNode)N).iterateCallSites(), 
		(CallSiteReference)((Pair)dst).snd);
	  } else {
	    assert N instanceof Pair;
	    Pair p = (Pair)N;
	    return 
		CG.getPossibleTargets((CGNode)p.fst, (CallSiteReference)p.snd)
		  .contains(dst);
	  }
	}

        public void addEdge(Object src, Object dst)  {
	  Assertions.UNREACHABLE();
	}

	public void removeEdge(Object src, Object dst) {
	  Assertions.UNREACHABLE();
	}

        public void removeAllIncidentEdges(Object node) {
	  Assertions.UNREACHABLE();
	}

	public void removeIncomingEdges(Object node) {
	  Assertions.UNREACHABLE();
	}

        public void removeOutgoingEdges(Object node) {
	  Assertions.UNREACHABLE();
	}
      };
	   
      protected NodeManager getNodeManager() {
	return nodeManager;
      }

      protected EdgeManager getEdgeManager() {
	return edgeManager;
      }
    };  
  }

  public IPFieldAccessAnalysis(CallGraph CG, 
			       FieldAccessesFactory localAccesses)
  {
    this(CG, null, localAccesses);
  }

  public IPFieldAccessAnalysis(CallGraph CG, 
			       Collection interestingNodes,
			       FieldAccessesFactory localAccesses)
  {
    this.CG = CG;
    this.interestingNodes = interestingNodes;
    this.localAccesses = localAccesses;
  }

  public IPFieldAccessesResult solve() throws CancelException {
    final IPFieldAccessSolver solver = new IPFieldAccessSolver(this);
    solver.solve(null);
    return new IPFieldAccessesResult() {
      public FieldAccesses getFieldAccesses(final CGNode node) {
	return new FieldAccesses() {
	  public PointerKey[] getUses(SSAInstruction i) {
	    SSAAbstractInvokeInstruction inst=(SSAAbstractInvokeInstruction)i;
	    FieldAccessesVariable var = (FieldAccessesVariable)
	      solver.getIn(Pair.make(node, inst.getCallSite()));
	    if (var.readReferences == null)
	      return new PointerKey[0];
	    else
	      return (PointerKey[])
	        var.readReferences
		  .toArray(new PointerKey[ var.readReferences.size() ]);
	  }

	  public PointerKey[] getDefs(SSAInstruction i) {
	    SSAAbstractInvokeInstruction inst=(SSAAbstractInvokeInstruction)i;
	    FieldAccessesVariable var = (FieldAccessesVariable)
	      solver.getIn(Pair.make(node, inst.getCallSite()));
	    if (var.writeReferences == null)
	      return new PointerKey[0];
	    else
	      return (PointerKey[])
	        var.writeReferences
		  .toArray(new PointerKey[ var.writeReferences.size() ]);
	  }
	};
      }
    };
  }
}

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
package com.ibm.wala.memsat.frontEnd.slicer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.util.ints.IndexedEntry;

import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.ipa.slicer.AstJavaSlicer;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.fieldssa.FieldNameSSAConversion;
import com.ibm.wala.memsat.frontEnd.fieldssa.IPFieldAccessesResult;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.ComposedIterator;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.Graph;

public class PartialSlice extends AstJavaSlicer {

  public static Collection<Statement> 
    compute(CallGraph CG, 
	    PointerAnalysis<InstanceKey> pa, 
	    Set<CGNode> partialRoots, 
	    Collection<Statement> ss) 
      throws com.ibm.wala.util.CancelException
  {
    CallGraph pcg = PartialCallGraph.make(CG, partialRoots);
    SDG sdg = new SDG(pcg, pa, new AstJavaModRef(), DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    return computeBackwardSlice(sdg, ss);
  }

  public static FieldSSATable 
    sliceFieldSSA(final CGNode node, 
		  PointerAnalysis<InstanceKey> pa,
		  IClassHierarchy cha,
		  IPFieldAccessesResult ipaFieldAccesses,
		  final SDG sdg, 
		  final Collection<Statement> slice)
  {
    IR ir = node.getIR();

    FieldNameSSAConversion c = 
      new FieldNameSSAConversion(
        ir, 
	new FieldNameSSAConversion.IPFieldAccesses(
	  true,
	  node,
	  pa,
	  cha,
	  ipaFieldAccesses.getFieldAccesses(node)))
    {
      protected SSAInstruction[] getInstructions(IR ir) {
	SSAInstruction[] allInstrs = ir.getInstructions();

	Map<SSAInstruction,Integer> indicesMap = PDG.computeInstructionIndices(ir);
	SSAInstruction[] x = new SSAInstruction[ allInstrs.length ];
	for(int i = 0; i < allInstrs.length; i++) {
	  if (allInstrs[i] == null) {
	    continue;
	  }

	  if (slice.contains(
	        PDG.ssaInstruction2Statement(node, allInstrs[i], indicesMap, ir))) 
	  {
	    x[i] = allInstrs[i];
	  }
	}
    
	return x;
      }
    };
    
    return c.convert();
  }

  private static <T> void order(Graph<T> G, T node, List<T> order) {
	  if (! order.contains(node)) {
		  for(Iterator<T> ps = G.getPredNodes(node); ps.hasNext(); ) {
			  T p = ps.next();
			  order(G, p, order);
		  }
		  order.add(node);
	  }
  }
  
  public static class InstructionEntry implements IndexedEntry<SSAInstruction> {
      private final SSAInstruction inst;
      private final int index;
      private final SSACFG.BasicBlock bb;
      
      private InstructionEntry(SSACFG.BasicBlock bb, int index, SSAInstruction inst) {
    	  this.inst = inst;
    	  this.bb = bb;
    	  this.index = index;
      }

      public SSAInstruction value() {
    	  return inst;
      }

      public int index() {
    	  return index;
      }

      public SSACFG.BasicBlock bb() {
    	  return bb;
      }
      
      public int hashCode() {
    	  return index() * value().hashCode();
      }

      public boolean equals(Object o) {
	return (o instanceof InstructionEntry) &&
	  ((InstructionEntry)o).index() == index() &&
	  ((InstructionEntry)o).value().equals(value());
      }
    }

  public static Iterator<InstructionEntry>
    relevantInstructions(final CGNode node, 
			 final FieldSSATable fieldSSA,
			 SDG sdg, 
			 final Collection<Statement> slice)
  {
    final IR ir = node.getIR();
     final Map<SSAInstruction, Integer> indicesMap = PDG.computeInstructionIndices(ir);
    final ObjectArrayMapping<SSAInstruction> instructionIndices = 
      new ObjectArrayMapping<SSAInstruction>(ir.getInstructions());
       
    SSACFG cfg = ir.getControlFlowGraph();
	List<ISSABasicBlock> order = new ArrayList<ISSABasicBlock>(cfg.getNumberOfNodes());
    assert Acyclic.isAcyclic(cfg, cfg.entry());
    order(cfg, ir.getExitBlock(), order);
    
    return 
      new ComposedIterator(order.iterator()) {
    	
        public Iterator makeInner(Object outer) {
          final SSACFG.BasicBlock block = (SSACFG.BasicBlock)outer;
          return 
          	new CompoundIterator(
          		new MapIterator(
          			fieldSSA.getPhiNodes(block),
		new Function<SSAInstruction,InstructionEntry>() {
		  public InstructionEntry apply(SSAInstruction x) {
		    return new InstructionEntry(block, -1, x);
		  }
		}
	      ),
	      new MapIterator(
                new FilterIterator(
	          block.iterator(),
	          new Predicate() {
	            private boolean sliceContainsInstruction(SSAInstruction s) {
		      return 
			slice.contains(
			  PDG.ssaInstruction2Statement(node, s, indicesMap, ir));
		    }
		    public boolean test(Object o) {
	              return sliceContainsInstruction((SSAInstruction)o);
		    }
		  }
		),
		new Function<SSAInstruction,InstructionEntry>() {
		  public InstructionEntry apply(SSAInstruction x) {
			  int index = -1;
			  if (instructionIndices.hasMappedIndex(x)) {
				  index = instructionIndices.getMappedIndex(x);
			  }
			  return new InstructionEntry(block, index, (SSAInstruction)x);
		  }
		}
	      )		
	    );
	}
      };
  }
}

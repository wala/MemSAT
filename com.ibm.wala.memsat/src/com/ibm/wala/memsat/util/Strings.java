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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Node;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaSSAPropagationCallGraphBuilder.EnclosingObjectReferenceKey;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;

/**
 * A set of utility functions for string manipulation and pretty printing of Kodkod nodes,
 * IRs, etc.
 * 
 * @author Emina Torlak
 */
public final class Strings {

	/**
	 * Returns a pretty-printed string representation of the 
	 * given nodes, with each line offset with at least the given
	 * number of whitespaces.  The display parameter determines how 
	 * the mapped nodes are displayed; that is, a descendant d of the
	 * given node is displayed as display.get(d.toString()) if 
	 * display.containsKey(d.toString()) is true.
	 * @requires 0 <= offset < line
	 * @return a pretty-printed string representation of the 
	 * given nodes
	 */
	public static String prettyPrint(Collection<Node> nodes, int offset, int line, Map<String,String> display) { 
		return PrettyPrinter.print(nodes, offset, 80, display);
	}
	/**
	 * Returns a pretty-printed string representation of the 
	 * given node, with each line offset with at least the given
	 * number of whitespaces.  The display parameter determines how 
	 * the mapped nodes are displayed; that is, a descendant d of the
	 * given node is displayed as display.get(d.toString()) if 
	 * display.containsKey(d.toString()) is true.
	 * @requires 0 <= offset < line
	 * @return a pretty-printed string representation of the 
	 * given node
	 */
	public static String prettyPrint(Node node, int offset, Map<String, String> display) { 
		return prettyPrint(node, offset, 80, display);
	}
	
	/**
	 * Returns a pretty-printed string representation of the 
	 * given node, with each line offset with at least the given
	 * number of whitespaces.  The line parameter determines the
	 * length of each pretty-printed line, including the offset.
	 * The display parameter determines how 
	 * the mapped nodes are displayed; that is, a descendant d of the
	 * given node is displayed as display.get(d.toString()) if 
	 * display.containsKey(d.toString()) is true.
	 * @requires 0 <= offset < line
	 * @return a pretty-printed string representation of the 
	 * given node
	 */
	public static String prettyPrint(Node node, int offset, int line, final Map<String, String> display) { 
		return prettyPrint(Collections.singleton(node), offset, line, display);
	}
	
	/**
	 * Returns a pretty-printed string representation of the 
	 * given node, with each line offset with at least the given
	 * number of whitespaces.  The line parameter determines the
	 * length of each pretty-printed line, including the offset.
	 * @requires 0 <= offset < line
	 * @return a pretty-printed string representation of the 
	 * given node
	 */
	@SuppressWarnings("unchecked")
	public static String prettyPrint(Node node, int offset, int line) { 
		assert offset >= 0 && offset < line && line > 0;
		return prettyPrint(node, offset, line, Collections.EMPTY_MAP);
	}
	
	/**
	 * Returns a pretty-printed string representation of the 
	 * given node, with each line offset with at least the given
	 * number of whitespaces.  
	 * @requires 0 <= offset < 80
	 * @return a pretty-printed string representation of the 
	 * given node
	 */
	public static String prettyPrint(Node node, int offset) { 
		return prettyPrint(node,offset,80);
	}
	
	/**
	 * Returns a string that consists of the given number of repetitions
	 * of the specified string.
	 * @return str^reps
	 */
	public static String repeat(String str, int reps) {
		final StringBuffer result = new StringBuffer();
		for(int i = 0; i < reps; i++) { 
			result.append(str);
		}
		return result.toString();
	}

	/**
	 * Returns the given string, with all new lines replaced with new lines indented 
	 * by the given number of spaces.
	 * @return given string, with all new lines replaced with new lines indented 
	 * by the given number of spaces.
	 */
	public static String indent(String str, int offset) { 
		assert offset >= 0;
		final String indent = repeat(" ", offset);
		return indent + str.replaceAll("\\n", "\n"+indent);
	}
	
	/**
	 * Returns a pretty-print String representation
	 * of the given graph.
	 * @return pretty-print String representation
	 * of the given graph
	 */
	public static String prettyPrint(Graph<?> graph) { 
		return prettyPrint(graph,0);
	}
	
	/**
	 * Returns a pretty-print String representation
	 * of the given graph, with each new line starting
	 * indented at least the given number of spaces.
	 * @return pretty-print String representation
	 * of the given graph
	 */
	public static <T> String prettyPrint(Graph<T> graph, int offset) { 
		assert offset>=0;
		final StringBuffer result = new StringBuffer();
		final String indent = repeat(" ", offset);
		for(T o : graph) {
			result.append("\n");
			result.append(indent);
			result.append(o);
			for(Iterator<?> itr = graph.getSuccNodes(o); itr.hasNext(); ) { 
				result.append("\n" + indent + " --> " + itr.next());
			}
			result.append(",\n");
		}
		result.delete(result.length()-2, result.length());
		return result.toString();
	}

	/**
	 * Returns a pretty-print String representation
	 * of the given IR, with each new line starting
	 * indented at least the given number of spaces.
	 * @return pretty-print String representation
	 * of the given IR
	 */
	public static String prettyPrint(IR ir, int offset) { 
		return indent(ir.toString(), offset);
		/*
	    final StringBuffer result = new StringBuffer();
	    result.append("\n"+indent+"CFG:\n");
	    final SSACFG cfg = ir.getControlFlowGraph();
	    for (int i = 0; i <= cfg.getNumber(cfg.exit()); i++) {
	        BasicBlock bb = cfg.getNode(i);
	        result.append(indent+"BB").append(i).append("[").append(bb.getFirstInstructionIndex()).append("..").append(bb.getLastInstructionIndex())
	            .append("]\n");

	        Iterator<ISSABasicBlock> succNodes = cfg.getSuccNodes(bb);
	        while (succNodes.hasNext()) {
	          result.append(indent+"    -> BB").append(((BasicBlock) succNodes.next()).getNumber()).append("\n");
	        }
	      }
	    result.append(indent+"Instructions:\n");
	    for (int i = 0; i <= cfg.getMaxNumber(); i++) {
	      BasicBlock bb = cfg.getNode(i);
	      int start = bb.getFirstInstructionIndex();
	      int end = bb.getLastInstructionIndex();
	      result.append(indent+"BB").append(bb.getNumber());
	      if (bb instanceof ExceptionHandlerBasicBlock) {
	        result.append(indent+"<Handler>");
	      }
	      result.append("\n");
	      final SymbolTable symbolTable = ir.getSymbolTable();
	      for (Iterator<SSAPhiInstruction> it = bb.iteratePhis(); it.hasNext();) {
	        SSAPhiInstruction phi = it.next();
	        if (phi != null) {
	          result.append(indent+"           " + phi.toString(symbolTable)).append("\n");
	        }
	      }
	      if (bb instanceof ExceptionHandlerBasicBlock) {
	        ExceptionHandlerBasicBlock ebb = (ExceptionHandlerBasicBlock) bb;
	        SSAGetCaughtExceptionInstruction s = ebb.getCatchInstruction();
	        if (s != null) {
	          result.append(indent+"           " + s.toString(symbolTable)).append("\n");
	        } else {
	          result.append(indent+"           " + " No catch instruction. Unreachable?\n");
	        }
	      }
	      final SSAInstruction[] instructions = ir.getInstructions();
	      for (int j = start; j <= end; j++) {
	        if (instructions[j] != null) {
	          StringBuffer x = new StringBuffer(indent+j + "   " + instructions[j].toString(symbolTable));
	          StringStuff.padWithSpaces(x, 45);
	          result.append(indent+x);
	          result.append("\n");
	        }
	      }
	      for (Iterator<SSAPiInstruction> it = bb.iteratePis(); it.hasNext();) {
	        SSAPiInstruction pi = it.next();
	        if (pi != null) {
	          result.append(indent+"           " + pi.toString(symbolTable)).append("\n");
	        }
	      }
	    }
	    return result.toString();
	    */
	}
	
	/**
	 * Returns a pretty-print String representation
	 * of the given IR.
	 * @return pretty-print String representation
	 * of the given IR
	 */
	public static String prettyPrint(IR ir) { return prettyPrint(ir,0); }
	
	/**
	 * Returns a pretty-print String representation
	 * of the given collection.
	 * @return pretty-print String representation
	 * of the given collection
	 */
	public static String prettyPrint(Collection<?> c) { 
		return prettyPrint(c,0);
	}
	
	/**
	 * Returns a pretty-print String representation
	 * of the given collection, with each new line starting
	 * indented at least the given number of spaces.
	 * @return pretty-print String representation
	 * of the given collection
	 */
	public static String prettyPrint(Collection<?> c, int offset) { 
		assert offset>=0;
		final StringBuffer result = new StringBuffer();
		final String indent = repeat(" ", offset);
		for(Object o : c) { 
			result.append(indent);
			result.append(o);
			result.append("\n");
		}
		return result.toString();
	}

	/**
	 * Returns a String representation of the position in the source of the given method corresponding
	 * to the instruction at the specified index, or the empty string if the line is unknown.
	 * @return a String representation of the position in the source of the given method corresponding
	 * to the instruction at the specified index, or the empty string if the line is unknown.
	 */
	public static final String line(IMethod method, int instructionIndex) { 
		if (instructionIndex>=0) {
			if (method instanceof ShrikeBTMethod) { 
				try {
					return String.valueOf(method.getLineNumber(((ShrikeBTMethod)method).getBytecodeIndex(instructionIndex)));
				} catch (InvalidClassFileException e) {  } // ignore
			} else if (method instanceof AstMethod) { 
				final Position pos = ((AstMethod)method).getSourcePosition(instructionIndex);
				if (pos!=null)
					return String.valueOf(pos.getFirstLine());
			}
		}
		return "";
	}
	
	/**
	 * Returns a map from each InlinedInstruction in the given set to a unique name.
	 * The names are constructed from the names of the concrete instructions wrapped
	 * in each inlined instruction. Short type names are used whenever possible.
	 * @return a map from each InlinedInstruction in the given set to a unique name.
	 */
	public static Map<InlinedInstruction, String> instructionNames(Set<InlinedInstruction> insts) { 
		final Map<CGNode,String> methodNames = nodeNames(Programs.relevantMethods(insts));
		final Map<String, List<InlinedInstruction>> name2Inst = new LinkedHashMap<String, List<InlinedInstruction>>();
		final Map<InlinedInstruction, String> inst2Name = new LinkedHashMap<InlinedInstruction, String>();
		
		for(InlinedInstruction inst : insts) { 
			final String m = methodNames.get(inst.cgNode());
			final String infix;
			final int idx = inst.instructionIndex();
			if (idx==Integer.MIN_VALUE) { 
				infix = "start";
			} else if (idx==Integer.MAX_VALUE) { 
				infix = "end";
			} else { 
				final String cname = "";//inst.instruction().getClass().getSimpleName().replaceAll("SSA", "").replaceAll("Instruction", "");
				infix = cname+idx;
			} 
			final String name = m + "[" + infix + "]"; // m+"_"+infix;
			List<InlinedInstruction> named = name2Inst.get(name);
			if (named==null) { 
				named = new ArrayList<InlinedInstruction>(3);
				name2Inst.put(name, named);
			}
			named.add(inst);
		}
		
		for(Map.Entry<String, List<InlinedInstruction>> entry : name2Inst.entrySet()) { 
			final List<InlinedInstruction> named = entry.getValue();
			if (named.size()==1) { 
				inst2Name.put(named.get(0), entry.getKey());
			} else {
				for(InlinedInstruction inst : named) { 
					final StringBuilder b = new StringBuilder();
					assert !inst.callStack().empty();
					final Iterator<CallSite> itr = inst.callStack().iterator();
					b.append(methodNames.get(itr.next().getNode()));
					while(itr.hasNext()) { 
						b.append("_" + methodNames.get(itr.next().getNode()));
					}
					b.append("_" + entry.getKey());
					inst2Name.put(inst, b.toString());
				}
			}
		}
		
		return inst2Name;
	}
	
	/**
	 * Returns a map from each CGNode in the given set to a unique name derived
	 * from the signature of that node. Short type names are used whenever possible.
	 * @return a map from each CGNode in the given set to a unique name derived
	 * from the signature of that node. 
	 */
	public static Map<CGNode, String> nodeNames(Set<CGNode> nodes) { 
		final Map<String, List<CGNode>> name2Node = new LinkedHashMap<String, List<CGNode>>();
		final Map<CGNode,String> node2Name = new LinkedHashMap<CGNode, String>();
		
		for(CGNode ref : nodes) { 
			final String name = ref.getMethod().getName().toString();
			List<CGNode> named = name2Node.get(name);
			if (named==null) { 
				named = new ArrayList<CGNode>(3);
				name2Node.put(name, named);
			}
			named.add(ref);
		}
		for(Map.Entry<String,List<CGNode>> entry: name2Node.entrySet()) { 
			final List<CGNode> named = entry.getValue();
			if (named.size()==1) { 
				node2Name.put(named.get(0), entry.getKey());
			} else {
				for(CGNode ref : named) { 
					node2Name.put(ref, ref.getMethod().getSignature());
				}
			}
		}
		return node2Name;
	}
	
	/**
	 * Returns a map from each InstanceKey in the given set to a unique name.
	 * The names are constructed from the names of the concrete types represented
	 * by each instance key.  If there is more than one instance key for the same 
	 * type, unique suffixes are appended to make the names unique.  Short type names
	 * are used whenever possible.
	 * @return a map from each InstanceKey in the given set to a unique name.
	 */
	public static Map<InstanceKey, String> instanceNames(Set<InstanceKey> instances) { 
		final Map<TypeName, List<InstanceKey>> nameToKey = new LinkedHashMap<TypeName,List<InstanceKey>>();
		final Map<String, Boolean> uniqueShort = new HashMap<String,Boolean>();
		final Map<InstanceKey, String> keyToName = new LinkedHashMap<InstanceKey,String>();
		
		for(InstanceKey key : instances) {
			final TypeName fullName = key.getConcreteType().getName();
			List<InstanceKey> named = nameToKey.get(fullName);
			if (named==null) {
				named = new ArrayList<InstanceKey>(3);
				nameToKey.put(fullName, named);
			}
			named.add(key);
		}
		
		for(TypeName fullName : nameToKey.keySet()) { 
			final String shortName = fullName.getClassName().toString();
			final Boolean unique = uniqueShort.get(shortName);
			if (unique==null)	{ uniqueShort.put(shortName, Boolean.TRUE); }
			else 				{ uniqueShort.put(shortName, Boolean.FALSE); }
		}
		
		for(Map.Entry<TypeName, List<InstanceKey>> entry : nameToKey.entrySet()) {
			final TypeName fullName = entry.getKey();
			final List<InstanceKey> named = entry.getValue();
			final String shortName = fullName.getClassName().toString();
			final String name = uniqueShort.get(shortName) ? shortName : fullName.toString();
			final int size = named.size();
			if (size==1) { 
				keyToName.put(named.get(0), name);
			} else {
				for(int i = 0; i < size; i++) {
					keyToName.put(named.get(i), name + i);
				}
			}
		}
		assert keyToName.size() == instances.size();
		return keyToName;
	}
	
	/**
	 * Returns a map from each IField in the given set to a unique name.
	 * The names are constructed from the names of the  fields represented
	 * by IField.  Short field names are used whenever possible.
	 * @return a map from each IField in the given set to a unique name.
	 */
	public static Map<IField, String> fieldNames(Set<IField> fields) { 
		final Map<String, List<IField>> name2Field = new LinkedHashMap<String, List<IField>>();
		final Map<IField,String> field2Name = new LinkedHashMap<IField, String>();
		
		for(IField field : fields) { 
			final String name = field.getName().toString();
			List<IField> named = name2Field.get(name);
			if (named==null) { 
				named = new ArrayList<IField>(3);
				name2Field.put(name, named);
			}
			named.add(field);
		}
		for(Map.Entry<String,List<IField>> entry: name2Field.entrySet()) { 
			final List<IField> named = entry.getValue();
			if (named.size()==1) { 
				field2Name.put(named.get(0), entry.getKey());
			} else {
				for(IField field : named) { 
					field2Name.put(field, field.getReference().getSignature());
				}
			}
		}
		return field2Name;
	}

	/**
	 * Returns a map from each PointerKey in the given set to a unique name.
	 * The names are constructed from the names of the concrete fields represented
	 * by each pointer key.  If there is more than one pointer key for the same 
	 * field, unique suffixes are appended to make the names unique.  Short field names
	 * are used whenever possible.
	 * @requires fields in InstanceFieldKey + StaticFieldKey + ArrayContentsKey + ArrayLengthKey + EnclosingObjectReferenceKey
	 * @return a map from each PointerKey in the given set to a unique name.
	 */
	public static Map<PointerKey, String> pointerNames(Set<? extends PointerKey> fields) { 
		final Map<Pair<TypeName,String>, List<PointerKey>> nameToKey = new LinkedHashMap<Pair<TypeName, String>,List<PointerKey>>();
		final Map<String, Boolean> uniqueShort = new HashMap<String,Boolean>();
		final Map<PointerKey, String> keyToName = new LinkedHashMap<PointerKey,String>();
		
		for(PointerKey key : fields) {
			final TypeName typeName;
			final String fieldName;
			
			if (key instanceof InstanceFieldKey) {
				final IField field = ((InstanceFieldKey)key).getField();
				typeName = field.getDeclaringClass().getName();
				fieldName = field.getName().toString();
			} else if (key instanceof StaticFieldKey) {
				final IField field = ((StaticFieldKey)key).getField();
				typeName = field.getDeclaringClass().getName();
				fieldName = field.getName().toString();
			} else if (key instanceof ArrayContentsKey) { 
				typeName = ((ArrayContentsKey)key).getInstanceKey().getConcreteType().getName();
				fieldName = "";
			} else if (key instanceof ArrayLengthKey) { 
				typeName = ((ArrayLengthKey)key).getInstanceKey().getConcreteType().getName();
				fieldName = "length";
			} else {
				typeName = ((EnclosingObjectReferenceKey)key).getInstanceKey().getConcreteType().getName();
				fieldName = "@enc";
			}
			
			final Pair<TypeName,String> fullName = Pair.make(typeName, fieldName);
			List<PointerKey> named = nameToKey.get(fullName);
			if (named==null) {
				named = new ArrayList<PointerKey>(3);
				nameToKey.put(fullName, named);
			}
			named.add(key);
		}
		
		for(Pair<TypeName,String> fullName : nameToKey.keySet()) { 
			final String shortName = fullName.fst.getClassName().toString() + "$" + fullName.snd;
			final Boolean unique = uniqueShort.get(shortName);
			if (unique==null)	{ uniqueShort.put(shortName, Boolean.TRUE); }
			else 				{ uniqueShort.put(shortName, Boolean.FALSE); }
		}
		
		for(Map.Entry<Pair<TypeName,String>, List<PointerKey>> entry : nameToKey.entrySet()) {
			final Pair<TypeName,String> fullName = entry.getKey();
			final List<PointerKey> named = entry.getValue();
			final String shortName = fullName.fst.getClassName().toString() + "$" + fullName.snd;
			final String name = uniqueShort.get(shortName) ? shortName : fullName.fst + "$" + fullName.snd;
			final int size = named.size();
			if (size==1) { 
				keyToName.put(named.get(0), name);
			} else {
				for(int i = 0; i < size; i++) {
					keyToName.put(named.get(i), name + i);
				}
			}
		}
		
		assert keyToName.size() == fields.size();
		return keyToName;
	}
}

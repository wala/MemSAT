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
package com.ibm.wala.memsat.viz;

import static com.ibm.wala.memsat.util.Graphs.root;
import static com.ibm.wala.memsat.util.Programs.instructions;
import static com.ibm.wala.memsat.util.Programs.relevantMethods;
import static com.ibm.wala.memsat.util.Strings.nodeNames;
import static com.ibm.wala.util.graph.traverse.DFS.iterateDiscoverTime;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Justification;
import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction;
import com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action;
import com.ibm.wala.memsat.frontEnd.WalaConcurrentInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ConstantFactory;
import com.ibm.wala.memsat.representation.Interpreter;
import com.ibm.wala.memsat.translation.concurrent.ConcurrentTranslation;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.types.TypeReference;

import kodkod.ast.Expression;
import kodkod.engine.Solution;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;

/**
 * Visualizes the results of a concurrent translation.
 * 
 * @author etorlak
 */
final class ConcurrentStringVisualizer extends StringVisualizer<ConcurrentTranslation> {
	private final Justification just;
	private final Program prog;
	private final WalaInformation info;
	/**
	 * Constructs a new string visualizer from the given translation and solution.
	 * @requires solution.instance!=null
	 * @requires solution.formula = translation.formula && solution.bounds = translation.bounds
	 */
	ConcurrentStringVisualizer(ConcurrentTranslation translation, Solution solution) {
		super(translation, solution);
		this.just = translation.context();
		this.prog = just.program();
		this.info = prog.info();
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.viz.StringVisualizer#visualize()
	 */
	@Override
	public String visualize() {
		final StringBuilder s = new StringBuilder();
		final Map<CGNode,String> methodNames = nodeNames(relevantMethods(instructions(info)));
		
		warnings(s, translation.warnings());
		
		s.append("solution interpretation:\n");
		s.append(" main execution");
		execution(s, just.execution(), methodNames);
		
		if (!just.speculations().isEmpty()) {		
			final List<? extends Execution> speculations = just.speculations();
			final List<? extends Expression> commits = just.commits();
			for(int i = 0, max = speculations.size(); i < max; i++) { 
				s.append("\n\n  speculative execution " + i + ":");
				execution(s, speculations.get(i), methodNames);
				s.append("\n  committed " + i + ": ");
				commit(s, commits.get(i));
			}
		}
		
		s.append("\n");
		return s.toString();
	}
	
	/**
	 * Appends to the specified buffer a string representation of the given commit set.
	 */
	private void commit(StringBuilder s, Expression commit) { 
		s.append("{");
		final TupleSet commitValue = eval.evaluate(commit);
		assert commitValue.arity()==1;
		if (!commitValue.isEmpty()) { 
			final Iterator<Tuple> itr = commitValue.iterator();
			s.append(itr.next().atom(0));
			while(itr.hasNext()) { 
				s.append(", " + itr.next().atom(0));
			}
		}
		s.append("}");
	}
	
	/**
	 * Appends to the specified buffer a string representation of the given execution.
	 */
	private void execution(StringBuilder s, Execution exec, Map<CGNode,String> methodNames) { 
		final Map<InlinedInstruction,String> execed = executedInstructions(exec, methodNames);
		for(Iterator<CGNode> threadItr = iterateDiscoverTime(info.threads(), root(info.threads())); threadItr.hasNext(); ) {
			final CGNode n = threadItr.next();
			final WalaConcurrentInformation cinfo = info.concurrentInformation(n);
			s.append("\n   actions of thread " + methodNames.get(n) + ":");
			for(Iterator<InlinedInstruction> instItr = iterateDiscoverTime(cinfo.threadOrder(), cinfo.start()); instItr.hasNext(); ) { 
				final InlinedInstruction inst = instItr.next();
				if (execed.containsKey(inst)) { 
					s.append("\n    " + atom(eval.evaluate(exec.action(inst))) + " = " + execed.get(inst));
				}
			}
		}
		writeSeen(s, exec);
		orderings(s, exec);
	}
	
	/**
	 * Appends a string representation of the write-seen relation for the given execution to the given string builder.
	 */
	private void writeSeen(StringBuilder s, Execution exec) { 
		s.append("\n   write seen: {");
		final TupleSet w = eval.evaluate(exec.w());
		assert w.arity()==2;
		if (!w.isEmpty()) { 
			final Iterator<Tuple> itr = w.iterator();
			s.append(pair(itr.next()));
			while(itr.hasNext()) { 
				s.append(", " + pair(itr.next()));
			}
		}
		s.append("}");
	}
	
	/**
	 * Appends a string representation of the orderings returned by exec.viz() to the given string builder.
	 */
	private void orderings(StringBuilder s, Execution exec) { 
		for(Map.Entry<Expression, String> entry : exec.viz().entrySet()) { 
			final TupleSet vizOrd = eval.evaluate(entry.getKey());
			assert vizOrd.arity() == 2;
			s.append("\n   " + entry.getValue() + ": {");
			if (vizOrd.isEmpty()) {
				 s.append(" }");
			} else {
				final Iterator<Tuple> itr = vizOrd.iterator();
				s.append("\n    " + pair(itr.next()));
				for(int i = 1; itr.hasNext(); i=(i+1)%5) { 
					if (i==0)	{ s.append(",\n    "); }
					else 		{ s.append(", "); }	
					s.append(pair(itr.next()));
				}
				s.append("\n   }");
			}
		}
	}
	
	/**
	 * Returns a map from each instruction in this.just.program.info that is executed by
	 * the given Execution to a readable string.
	 * @return a map from each instruction in this.just.program.info that is executed by
	 * the given Execution to a readable string.
	 */
	private final Map<InlinedInstruction, String> executedInstructions(Execution exec, Map<CGNode,String> methodNames) { 
		final Map<InlinedInstruction, String> ret = new LinkedHashMap<InlinedInstruction, String>();
		for(CGNode n : info.threads()) { 
			final WalaConcurrentInformation cinfo = info.concurrentInformation(n);
			for(InlinedInstruction inst : cinfo.actions()) { 
				if (eval.evaluate(exec.action(inst).some())) { 
					final String methodString = methodNames.get(inst.cgNode());
					final String instString = instruction(exec, inst);
					final String lineString = line(inst.cgNode().getMethod(), inst.instructionIndex());
					ret.put(inst, methodString + "[" + lineString + "]::" + instString);
				}
			}
		}
		return ret;
	}

	/**
	 * Returns a String representation of the given executed instruction.
	 * @requires this.eval.evaluate(this.prog.executes(exec,inst))
	 * @return a String representation of the given executed instruction.
	 */
	private final String instruction(Execution exec, InlinedInstruction inst) {
		final Action act = inst.action();
		switch(act) { 
		case START 	: case END : 
			return act.name().toLowerCase();
		case SPECIAL : 
			return ((SSAAbstractInvokeInstruction)inst.instruction()).getDeclaredTarget().getName().toString();
		case LOCK : case UNLOCK :
			return act.name().toLowerCase() + "(" + eval.evaluate(exec.action(inst).join(exec.monitor())) + ")";
		case NORMAL_READ : case VOLATILE_READ :
			final String readLoc = locationOf(exec, inst);
			final Interpreter<Object> reader = valueInterpreter(inst);
			final Object readVal = reader.evaluate(reader.fromObj(exec.action(inst).join(exec.w()).join(exec.v())), eval);
			return "read(" + readLoc + ", " + readVal + ")";
		case NORMAL_WRITE : case VOLATILE_WRITE :
			final String writeLoc = locationOf(exec, inst);
			final Interpreter<Object> writer = valueInterpreter(inst);
			final Object writeVal = writer.evaluate(writer.fromObj(exec.action(inst).join(exec.v())), eval);
			return "write(" + writeLoc + ", " + writeVal + ")";
		default : 
			throw new AssertionError("unreachable");
		}
	}

	/**
	 * Returns an interpreter for the value read or written by the given inlined instruction.
	 * @requires inst.action in NORMAL_READ + VOLATILE_READ + NORMAL_WRITE + VOLATILE_WRITE
	 * @return an interpreter for the value read or written by the given inlined instruction.
	 */
	private final Interpreter<Object> valueInterpreter(InlinedInstruction inst) { 
		final ConstantFactory consts = translation.factory().constants();
		final TypeReference type;
		if (inst.instruction() instanceof SSAFieldAccessInstruction) { 
			type = ((SSAFieldAccessInstruction)inst.instruction()).getDeclaredFieldType();
		} else {
			type = ((SSAArrayReferenceInstruction)inst.instruction()).getElementType();
		}
		return consts.interpreter(IRType.convert(type));
	}
	
	/**
	 * Returns a String representation of the location read or written by the given inlined instruction.
	 * @requires inst.action in NORMAL_READ + VOLATILE_READ + NORMAL_WRITE + VOLATILE_WRITE
	 * @requires this.eval.evaluate(this.prog.executes(exec,inst))
	 * @return a String representation of the location read or written by the given inlined instruction.
	 */
	private final String locationOf(Execution exec, InlinedInstruction inst) { 
		if (inst.instruction() instanceof SSAFieldAccessInstruction) { 
			final SSAFieldAccessInstruction obj = (SSAFieldAccessInstruction)inst.instruction();
			final IField field = info.callGraph().getClassHierarchy().resolveField(obj.getDeclaredField());
			final Expression locExpr = exec.action(inst).join(exec.location());
			if (field.isStatic()) {
				return atom(eval.evaluate(locExpr)).toString();
			} else {
				final Expression type = translation.factory().constants().valueOf(field.getDeclaringClass().getReference());
				final TupleSet fieldAtom = eval.evaluate(locExpr.difference(type));
				final TupleSet objectAtom = eval.evaluate(locExpr.intersection(type));
				return atom(objectAtom) + "." + atom(fieldAtom);
			}
		} else {
			assert inst.instruction() instanceof SSAArrayReferenceInstruction;
			final Expression location = exec.action(inst).join(exec.location());
			final int idx = eval.evaluate(location.sum());
			final TupleSet instance = eval.evaluate(location.difference(Expression.INTS));
			return atom(instance) + "[" + idx + "]";
		}
	}

	
	/**
	 * Given a singleton unary relation, returns the underlying atom.
	 * @requires s.size()==1 && s.arity()==1;
	 * @return s.iterator().next().atom(0)
	 */
	private static final Object atom(TupleSet s) { 
		assert s.arity()==1 : "Expected set of arity 1 but got " + s;
		assert s.size()==1 : "Expected set of size 1 but got " + s;
		return s.iterator().next().atom(0);
	}
	
	/** 
	 * Returns a String representation of the first two atoms of the given tuple, separated by ->.
	 * @requires t.arity >= 2 
	 * @return t.atom(0) + "->" + t.atom(1)
	 **/
	private static String pair(Tuple t) { return t.atom(0) + "->" + t.atom(1); }
	
	/**
	 * Returns a String representation of the position in the source of the given method corresponding
	 * to the instruction at the specified index, or the empty string if the line is unknown.
	 * @return a String representation of the position in the source of the given method corresponding
	 * to the instruction at the specified index, or the empty string if the line is unknown.
	 */
	private static final String line(IMethod method, int instructionIndex) { 
		if (instructionIndex!=Integer.MAX_VALUE && instructionIndex!=Integer.MIN_VALUE) {
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
	
}

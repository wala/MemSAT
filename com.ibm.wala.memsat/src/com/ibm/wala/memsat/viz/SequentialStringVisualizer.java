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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.HeapExpression;
import com.ibm.wala.memsat.representation.Interpreter;
import com.ibm.wala.memsat.translation.Environment.Frame;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslation;

import kodkod.ast.Expression;
import kodkod.engine.Evaluator;
import kodkod.engine.Solution;
import kodkod.util.ints.SparseSequence;

/**
 * Visualizes the results of a sequential translation as a string.
 * 
 * @author etorlak
 */
final class SequentialStringVisualizer extends StringVisualizer<SequentialTranslation> {
	private final WalaInformation info;
	private final ExpressionFactory factory;
	private final CGNode node;
	
	
	/**
	 * Constructs a new string visualizer from the given translation and solution.
	 * @requires solution.instance!=null
	 * @requires solution.formula = translation.formula && solution.bounds = translation.bounds
	 */
	SequentialStringVisualizer(SequentialTranslation translation, Solution solution) {
		super(translation, solution);
		this.factory = translation.factory();
		this.info = factory.info();
		this.node = info.threads().iterator().next();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.viz.StringVisualizer#visualize()
	 */
	public String visualize() {
		final StringBuilder str = new StringBuilder();
		
		warnings(str, translation.context().warnings());
		
		str.append("solution interpretation:\n");
		method(str);
		arguments(str);
		returnValue(str);
		exceptionValue(str);
		
		if (info.relevantFields().isEmpty()) { 
			str.append(" initial heap: (empty)\n");
			str.append(" final heap: (empty)\n");		
		} else {
			final Map<InstanceKey, Set<PointerKey>> heapShape = collate();
			initialHeap(str, heapShape);
			finalHeap(str, heapShape);
		}
		
		return str.toString();
	}
	
	/** Appends the name of this.node to the given builder, followed by a newline. */
	private final void method(StringBuilder str) { 
		str.append(" method: " + node.getMethod() + "\n");
	}
	
	/** Appends the arguments of this.node to the given builder, followed by a newline. */
	private final void arguments(StringBuilder str) { 
		str.append(" arguments: (");
		int params = node.getMethod().getNumberOfParameters();
		final WalaCGNodeInformation nodeInfo = info.cgNodeInformation(node);
		final Object[] paramTransls = translation.factory().arguments(node);
		for(int param = 1; param <= params; param++) { 
			final IRType paramType = nodeInfo.typeOf(param);
			final Object paramTransl = paramTransls[param-1];
			str.append(factory.constants().interpreter(paramType).evaluate(paramTransl, eval));
			if (param < params) str.append(", ");
		}
		str.append(")\n");
	}
	
	/** Appends the return value of this.node to the given builder, followed by a newline. */
	private final void returnValue(StringBuilder str) { 
		str.append(" return value: ");
		final Object retTransl = translation.context().returnValue();
		if (retTransl==null)
			str.append("(void)");
		else {
			final IRType retType = IRType.convert(node.getMethod().getReturnType());
			str.append(factory.constants().interpreter(retType).evaluate(retTransl,eval));
		}
		str.append("\n");
	}
	
	/** Appends the exception value of this.node to the given builder, followed by a newline. */
	private final void exceptionValue(StringBuilder str) { 
		str.append(" exception value: ");
		final Object exceptionTransl = translation.context().exceptionValue();
		if (exceptionTransl==null)
			str.append("(none)");
		else {
			str.append(factory.constants().interpreter(IRType.OBJECT).evaluate(exceptionTransl,eval));
		}
		str.append("\n");
	}
	
	/** 
	 * Appends the initial heap values of this.node to the given builder, followed by a newline. 
	 * @requires heapShape.equals(this.collate())
	 **/
	private final void initialHeap(StringBuilder str, Map<InstanceKey,Set<PointerKey>> heapShape) { 
		final Frame frame = translation.context().frame();
		final FieldSSATable ssa = info.cgNodeInformation(node).fieldSSA();
		
		str.append(" initial heap:");

		for(Map.Entry<InstanceKey,Set<PointerKey>> e: heapShape.entrySet())  {
			final Set<PointerKey> fields = e.getValue();
			for(Iterator<Expression> instances = e.getKey()!=null?factory.constants().instances(e.getKey()): 
				Collections.singleton((Expression)null).iterator(); instances.hasNext(); ) { 
				final Expression instance = instances.next();
				for(PointerKey field : fields) { 
					str.append("\n  " + fieldName(instance,field) + "=");
					HeapExpression<?> fieldTransl = frame.heapUse(ssa.getEntryValue(field));
					if (fieldTransl.isArray()) { 
						final int length  = ((FieldExpression<?>) frame.heapUse(ssa.getEntryValue(lengthKey(fields)))).evaluate(instance, eval);
						final Object defaultValue = defaultValue(fieldTransl.valueInterpreter(),eval);
						str.append(toArray(((ArrayExpression<?>)fieldTransl).evaluate(instance, eval), length, defaultValue));
					} else {
						str.append(""+((FieldExpression<?>)fieldTransl).evaluate(instance, eval));
					}
				}
			}
		}
		str.append("\n");
	}
	
	/** 
	 * Appends the final heap values of this.node to the given builder, followed by a newline. 
	 * @requires heapShape.equals(this.collate())
	 **/
	private final void finalHeap(StringBuilder str, Map<InstanceKey,Set<PointerKey>> heapShape) { 
		final Frame frame = translation.context().frame();
		final FieldSSATable ssa = info.cgNodeInformation(node).fieldSSA();
		
		str.append(" final heap:");
		for(Map.Entry<InstanceKey,Set<PointerKey>> e: heapShape.entrySet())  {
			Set<PointerKey> fields = e.getValue();
			for(Iterator<Expression> instances = e.getKey()!=null?factory.constants().instances(e.getKey()): 
				Collections.singleton((Expression)null).iterator(); instances.hasNext(); ) { 
				final Expression instance = instances.next();
				for(PointerKey field : fields) { 
					str.append("\n  " + fieldName(instance,field) + "=");
					final HeapExpression<?> fieldTransl = frame.heapUse(ssa.getExitValue(field));
					if (fieldTransl.isArray()) { 
						final int length  = ((FieldExpression<?>) frame.heapUse(ssa.getExitValue(lengthKey(fields)))).evaluate(instance, eval);
						final Object defaultValue = defaultValue(fieldTransl.valueInterpreter(),eval);
						str.append(toArray(((ArrayExpression<?>)fieldTransl).evaluate(instance, eval), length, defaultValue));
					} else {
						str.append(""+((FieldExpression<?>)fieldTransl).evaluate(instance, eval));
					}
				}
			}
		}
		str.append("\n");
	}
	
	private static String fieldName(Expression instance, PointerKey field) { 
		return (instance==null?"":(instance + ".")) + field;
	}

	/**
	 * Returns a dense array representation from the given sparse representation, where
	 * the length of the returned array and the default value are as specified.
	 * @return a dense array representation from the given sparse representation, where
	 * the length of the returned array and the default value are as specified.
	 */
	private static <V> List<V> toArray(SparseSequence<V> s, int length, V defaultValue) { 
		final List<V> list = new ArrayList<V>(length);
		for(int i = 0; i < length; i++) { 
			list.add( s.containsIndex(i) ? s.get(i) : defaultValue );
		}
		return list;
	}
	
	/**
	 * Returns the value of the interpreter.defaultValue(), as given by the specified evaluator.
	 * @return the value of the interpreter.defaultValue(), as given by the specified evaluator.
	 */
	private static <T> Object defaultValue(Interpreter<T> interpreter, Evaluator eval) {
		return interpreter.evaluate(interpreter.defaultValue(), eval);
	}
	
	/**
	 * Returns the first ArrayLengthKey found in the given set or null if none is found.
	 * @return the first ArrayLengthKey found in the given set or null if none is found.
	 */
	private static ArrayLengthKey lengthKey(Set<PointerKey> keys) { 
		for(PointerKey key : keys) { 
			if (key instanceof ArrayLengthKey) { 
				return (ArrayLengthKey)key;
			}
		}
		return null;
	}
	
	/**
	 * Returns a map from each instance key in info.relevantClasses to the
	 * fields in relevant fields for which they are the source.
	 * @return a map, as described above.
	 */
	private Map<InstanceKey, Set<PointerKey>> collate() { 
		final Map<InstanceKey, Set<PointerKey>> ret = 
			new LinkedHashMap<InstanceKey, Set<PointerKey>>();
		for(InstanceKey key: info.relevantClasses()) { 
			ret.put(key, new LinkedHashSet<PointerKey>());
		}
		ret.put(null, new LinkedHashSet<PointerKey>());
		
		for(PointerKey field : info.relevantFields()) { 
			if (field instanceof AbstractFieldPointerKey){
				AbstractFieldPointerKey afield = (AbstractFieldPointerKey) field;
				ret.get(afield.getInstanceKey()).add(afield);
			} else {
				ret.get(null).add(field);
			}
		}
		return ret;
	}
}

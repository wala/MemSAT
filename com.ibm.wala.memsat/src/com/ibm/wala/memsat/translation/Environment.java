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
package com.ibm.wala.memsat.translation;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.util.collections.ArrayStack;
import kodkod.util.collections.Stack;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.ConstantFactory;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.HeapExpression;
import com.ibm.wala.memsat.representation.RealExpression;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.Iterator2Collection;
/**
 * Stores and manages the state of a symbolic execution 
 * performed by an instance of the {@linkplain Translator Miniatur translator}.
 * An instance of this class is, conceptually, a stack of {@linkplain Frame context frames}, 
 * one for each method that has been symbolically called so far (analogous to a 
 * stack frame created for a method call in a real execution environment).  
 * 
 * <p>An environment manages both the local (stack) and global (heap and lexical) variables.
 * Each method has its own view of the heap and the lexical scope, as given by its context frame.
 * The handling of globally visible writes (i.e. to heap locations) is left unspecified.  An
 * environment only guarantees that after its top frame has been popped off, the exit writes to 
 * the heap and lexical variables will have correctly propagated to the preceding frames.  Note that
 * only the top frame's view of the heap / lexical / local variables is visible at any given time. 
 * </p>
 * 
 * @specfield frames: Stack<Frame> 
 * @specfield top: lone frames.elems[0]
 * @specfield factory: ExpressionFactory // factory for creating Kodkod representations of constants, initial heap values, etc.
 * @specfield instantiated: factory.info.relevantClasses ->one int 
 * 
 * @invariant no top.call // the entry point method doesn't have a call instruction associated with it.
 * @invariant all c: factory.info.relevantClasses | 0 <= instantiated[c] < factory.info.cardinality(c)
 * @author Emina Torlak
 */
public final class Environment {
	private final Stack<Frame> frames;
	private final ExpressionFactory factory;
	private Frame top;
	private Stack<CallSite> callStack;
	private final Map<InstanceKey, Iterator<Expression>> nextObj;
	/**
	 * Creates a new empty environment that will generate initial values using the given expression factory.
	 * @effects no this.frames' and this.factory' = factory and
	 * this.instantiated' = factory.info.relevantClasses -> 0
	 */
	public Environment(ExpressionFactory factory) {
		assert factory != null;
		
		this.factory = factory;
		this.top = null;
		this.frames = new ArrayStack<Frame>();
		this.callStack = null;
		this.nextObj = new LinkedHashMap<InstanceKey, Iterator<Expression>>();
		for(InstanceKey key : factory.info().relevantClasses()) { 
			nextObj.put(key, factory.constants().instances(key));
		}
	}
	
	/**
	 * Returns this.factory.
	 * @return this.factory
	 */
	public ExpressionFactory factory() { return factory; }
	
	/**
	 * Returns the top frame, if any. Otherwise returns null.
	 * @return this.top
	 */
	public Frame top() { return top; }
	
	/**
	 * Returns the number of recursive occurrences of the given target in
	 * this environment.
	 * @return # { f: this.frames.elems[int] | f.callInfo.cgNode.equals(target) }
	 */
	public int recursionCount(CGNode target) {
		int count = 0;
		for(Frame frame : frames) {
			if (frame.callInfo.cgNode().equals(target)) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Returns an unmodifiable view of this environment's call stack, given
	 * as a stack of call site references.  The view
	 * does not include the top-level call (for which there is no 
	 * call site reference).  The view's hashcode is the sum of the contained
	 * references' hashcodes.
	 * @return an unmodifiable view of this environment's call stack, given as a 
	 * stack of call site references.
	 */
	public Stack<CallSite> callStack() { 
		if (callStack==null) {
			callStack = new Stack<CallSite>() {
				public boolean empty() { return size() > 0; }
				
				public int size() { return frames.size() - 1; }	
				
				public CallSite peek() {
					if (empty()) throw new EmptyStackException();
					return new CallSite(top.call.getCallSite(), top.callInfo.cgNode());
				}
				
				public int search(Object arg0) {
					int i = 1;
					for(CallSite site : this) {
						if (site.equals(arg0)) return i;
						i++;
					}
					return -1;
				}
				
				public Iterator<CallSite> iterator() {
					return new Iterator<CallSite>() {
						final Iterator<Frame> itr = frames.iterator();
						Frame caller = null, callee = null;
						
						public boolean hasNext() { 
							if (callee==null) { 
								if (itr.hasNext()) {
									callee = itr.next();
								} else {
									return false;
								}	
							}
							if (caller==null) { 
								if (itr.hasNext()) { 
									caller = itr.next();
								} else {
									return false;
								}
							}
							return true;
						}

						public CallSite next() {
							if (!hasNext()) throw new NoSuchElementException();
							final CallSite next = new CallSite(callee.call.getCallSite(), caller.callInfo.cgNode());
							callee = caller;
							caller = null;
							return next;
						}

						public void remove() { throw new UnsupportedOperationException(); }
					};
				}
				
				public CallSite pop() { throw new UnsupportedOperationException(); }
				public CallSite push(CallSite arg0) { throw new UnsupportedOperationException(); }
			};
		}
		return callStack;
	}
	
	/**
	 * @effects puts definitions for the given args and all constant
	 * values in the top frame.
	 */
	private void defConstants(Object[] args) {
		for(int i = 0; i < args.length; i++) { 
			top.localDef(i+1, args[i]);
		}
		final SymbolTable table = top.callInfo.cgNode().getIR().getSymbolTable();
		final ConstantFactory constants = factory.constants();
		for(int v = args.length+1, max = table.getMaxValueNumber(); v <= max; v++) {
			if (table.isConstant(v)) {
				if (table.isBooleanConstant(v)) {
					top.localDef(v, constants.valueOf(table.isTrue(v)));
				} else if (table.isIntegerConstant(v)) {
					top.localDef(v, constants.valueOf(table.getIntValue(v)));
				} else if (table.isNullConstant(v)) {
					top.localDef(v, constants.nil());
				} else if (table.isStringConstant(v)) {
					top.localDef(v, constants.valueOf(table.getStringValue(v)));
				} else if (table.isLongConstant(v)) {
					top.localDef(v, constants.valueOf(table.getLongValue(v)));
				} else if (table.isFloatConstant(v)) {
					top.localDef(v, constants.valueOf(table.getFloatValue(v)));
				} else if (table.isDoubleConstant(v)) {
					top.localDef(v, constants.valueOf((float)table.getDoubleValue(v)));
				} else {
					throw new AssertionError("Unknown constant type for value number " + v);
				}
			}
		}
	}
	
	/**
	 * Returns an expression that represents the next 
	 * instance in the set of instances defined by the 
	 * given key.
	 * @requires key in this.factory.info.relevantClasses()
	 * @requires this.instantiated[key] < this.factory.info.cardinality(key)
	 * @effects this.instantiated' = this.instantiated ++ key -> (this.instantiated[key]+1)
	 * @return this.factory.instance(key, this.instantiated[key])
	 */
	public Expression instantiate(InstanceKey key) { 
		return nextObj.get(key).next();
	}
	
	/**
	 * Pushes a new frame for the given entry-point call onto  
	 * the empty environment stack, and returns this environment.  
	 * The local and heap environment are populated using this.factory.
	 * @requires no this.frames.elems
	 * @requires some i: [0..this.factory.info.threads()) | this.factory.info.entry(i) = node
	 * @effects 
	 * this.frames.push(this.top') and 
	 * no this.top'.call and this.top'.callInfo = this.factory.info.cgNodeInformation(node) and 
	 * (all i: [1..args.length] | this.top'.localEnv[i] = this.factory.arguments(node)[i-1]) and 
	 * (all i: [1..this.top'.callInfo.fieldSSATable().getMaxInitialHeapNumber()] | 
	 *   this.top'.heapEnv[i] = this.factory.initValueOf(this.top'.callInfo.fieldSSATable().getField(i)))
	 * @return this
	 */
	public Environment push(CGNode node) {
		assert frames.empty();
		
		top = frames.push(new Frame(null, factory.info().cgNodeInformation(node)));
		
		// define args and constants
		defConstants(factory.arguments(node));
		
		// initialize the heap
		final FieldSSATable fieldSSA = top.callInfo.fieldSSA();
		for(int i = 1, maxInit = fieldSSA.getMaxInitialHeapNumber(); i <= maxInit; i++) { 
			top.heapDef(i, factory.initValueOf(fieldSSA.getField(i)));
		}
			
		return this;
	}
	
	/**
	 * Pushes a new frame for the given entry-point call onto  
	 * the empty environment stack, and returns this environment.  
	 * The local environment is populated using this.factory.  The
	 * initial values for fields are populated using the given map
	 * and this.factory.  In particular, if the map has a value for
	 * a given (pointer key to a) field, that value is used as the 
	 * initial value.  Otherwise, the {@linkplain ExpressionFactory#initValueOf(PointerKey) initial value}
	 * given by this.factory is used.
	 * @requires no this.frames.elems
	 * @requires some i: [0..this.factory.info.threads()) | this.factory.info.entry(i) = node
	 * @effects 
	 * this.frames.push(this.top') and 
	 * no this.top'.call and this.top'.callInfo = this.factory.info.cgNodeInformation(node) and 
	 * (all i: [1..args.length] | this.top'.localEnv[i] = this.factory.arguments(node)[i-1]) and 
	 * (all i: [1..this.top'.callInfo.fieldSSATable().getMaxInitialHeapNumber()] | 
	 *   let field = this.top'.callInfo.fieldSSATable().getField(i) | 
	 *    this.top'.heapEnv[i] = override.containsKey(field) => 
	 *      override.get(field) else this.factory.initValueOf(field)) )
	 * @return this
	 */
	public Environment push(CGNode node, Map<PointerKey, HeapExpression<?>> override) { 
		assert frames.empty();
		
		top = frames.push(new Frame(null, factory.info().cgNodeInformation(node)));
		
		// define args and constants
		defConstants(factory.arguments(node));
		
		// initialize the heap
		final FieldSSATable fieldSSA = top.callInfo.fieldSSA();
		for(int i = 1, maxInit = fieldSSA.getMaxInitialHeapNumber(); i <= maxInit; i++) { 
			final PointerKey key = fieldSSA.getField(i);
			top.heapDef(i, override.containsKey(key) ? override.get(key) : factory.initValueOf(key));
		}
			
		return this;
	}
	
	/**
	 * Pushes a new frame for the given entry-point call onto  
	 * the existing environment stack and returns this environment.  The local and heap environment 
	 * are populated using the caller's local and heap environment (i.e.
	 * this.top.localEnv and this.top.heapEnv).
	 * @requires some this.frames.elems
	 * @requires call in this.top.callInfo.cgNode.getIR().getInstructions
	 * @requires callInfo in this.top.callInfo.cgNode.getPossibleTargets(call.getCallSite())
	 * @effects this.frames.push(this.top') and
	 * this.top'.call = call and this.top'.callInfo = callInfo and 
	 * (all i: [1..args.length] | this.top'.localEnv[i] = args[i-1]) and 
	 * (all i: this.top.callInfo.getUses(call) | 
	 *  this.top'.heapEnv[callInfo.fieldSSA.getEntryValue(this.top.callInfo.getField(i))] =  
	 *   this.top.heapEnv[i])) and
	 * @return this
	 */
	public Environment push(SSAAbstractInvokeInstruction call, WalaCGNodeInformation callInfo) {
		assert !frames.empty();
		assert call != null;
		
		final Frame caller = top;
		top = frames.push(new Frame(call, callInfo));
		
		// get args 
		final Object[] args = new Object[call.getNumberOfParameters()];
		for(int i = 0; i < args.length; i++) { 
			args[i] = caller.localUse(call.getUse(i));
		}
		
		// define the args and constants
		defConstants(args);
		
		// initialize heap
		final FieldSSATable calleeFSSA = callInfo.fieldSSA();
		final FieldSSATable callerFSSA = caller.callInfo.fieldSSA();
		
		final Set<PointerKey> used = Iterator2Collection.toSet(calleeFSSA.getFields());
		for(int use : callerFSSA.getUses(call)) {
//			System.out.println("used field: " + callerFSSA.getField(use));
//			System.out.println("used field value: " + caller.heapUse(use));
//			if (caller.heapUse(use)==null) { 
//				System.out.println("relevant field? " + factory.info().relevantFields().contains(callerFSSA.getField(use)));
//				System.out.println("factory field value: " + factory.valueOf(callerFSSA.getField(use)));
//			}
//			System.out.println("HERE::"+callerFSSA.getField(use)+"::"+calleeFSSA.getEntryValue(callerFSSA.getField(use)));
		    PointerKey field = callerFSSA.getField(use);
		    if (used.contains(field)) {
		    	top.heapDef(calleeFSSA.getEntryValue(field), caller.heapUse(use));
		    }
		}
		
		return this;
	}

	/**
	 * Removes the top frame from the stack and returns it.
	 * @requires some this.frames.elems
	 * @effects this.frames.pop()
	 * @effects this.top' = this.frames.elems'[0]
	 * @return this.top
	 */
	public Frame pop() {
		final Frame callee = frames.pop();
		top = frames.empty() ? null : frames.peek();
		return callee;
	}

	/**
	 * Returns the reference value stored at the given local address in the top frame.
	 * @requires use in this.top.localVars
	 * @requires this.top.callInfo.typeOf(use) = IRType.OBJECT
	 * @return this.top.localEnv[use]
	 */
	public Expression refUse(int use) { return top.localUse(use); }
	
	/**
	 * Returns the integer value stored at the given local address in the top frame.
	 * @requires use in this.top.localVars
	 * @requires this.top.callInfo.typeOf(use) = IRType.INTEGER
	 * @return this.top.localEnv[use]
	 */
	public IntExpression intUse(int use) { return top.localUse(use); } 
	
	/**
	 * Returns the real value stored at the given local address in the top frame.
	 * @requires use in this.top.localVars
	 * @requires this.top.callInfo.typeOf(use) = IRType.REAL
	 * @return this.top.localEnv[use]
	 */
	public RealExpression realUse(int use) { return top.localUse(use); }
	
	/**
	 * Returns the boolean value stored at the given local address in the top frame.
	 * @requires use in this.top.localVars
	 * @requires this.top.callInfo.typeOf(use) = IRType.BOOLEAN
	 * @return this.top.localEnv[use]
	 */
	public Formula boolUse(int use) { return top.localUse(use); }
	
	/**
	 * Returns the value of the given local variable in the top context frame, 
	 * or null if it hasn't been defined.
	 * @requires some this.top 
	 * @requires use in this.top.localVars
	 * @return this.top.localEnv[use]
	 */
	public Object localUse(int use) { return top.localUse(use); }

	/**
	 * Assigns the given value to the specified local variable in the top frame.
	 * @requires value in Formula iff this.top.callInfo.typeOf(use) in IRType.BOOLEAN
	 * @requires value in IntExpression iff this.top.callInfo.typeOf(use) in IRType.INTEGER
	 * @requires value in Expression iff this.top.callInfo.typeOf(use) in IRType.OBJECT
	 * @requires value in RealExpression iff this.top.callInfo.typeOf(use) in IRType.REAL
	 * @requires def in this.top.localVars
	 * @requires no this.top.localEnv[def]
	 * @effects this.top.localEnv' = this.top.localEnv + def -> value
	 */
	public <T> void localDef(int def, T value) { top.localDef(def, value); }

	/**
	 * Returns the top frame's view of the value of the given heap variable, 
	 * or null if it hasn't been defined.  
	 * @requires !this.top.callInfo.fieldSSA.isArrayNumber(use)
	 * @requires some this.top
	 * @requires use in this.top.heapVars
	 * @return this.top.heapEnv[use] 
	 */
	@SuppressWarnings("unchecked")
	public <T> FieldExpression<T> fieldUse(int use) { return (FieldExpression<T>)top.heapUse(use); }
	
	/**
	 * Returns the top frame's view of the value of the given heap variable, 
	 * or null if it hasn't been defined.  
	 * @requires this.top.callInfo.fieldSSA.isArrayNumber(use)
	 * @requires some this.top
	 * @requires use in this.top.heapVars
	 * @return this.top.heapEnv[use] 
	 */
	@SuppressWarnings("unchecked")
	public <T> ArrayExpression<T> arrayUse(int use) { return (ArrayExpression<T>)top.heapUse(use); }
	
	/**
	 * Returns the top frame's view of the value of the given heap variable, 
	 * or null if it hasn't been defined.  If the given heap variable is an array, then the 
	 * returned value is an ArrayExpression; otherwise, it is an Expression.
	 * @requires some this.top
	 * @requires use in this.top.heapVars
	 * @return this.top.heapEnv[use] 
	 */
	public <T> HeapExpression<T> heapUse(int use) { return top.heapUse(use); }
	
	/**
	 * Assigns the given value to the top frame's view of the specified heap variable.
	 * This method assumes that the given heap variable has not already been defined.
	 * @requires some this.top
	 * @requires this.top.callInfo.fieldSSA.isArrayNumber(use) => value in ArrayExpression
	 * else value in FieldExpression
	 * @requires def in this.top.heapVars
	 * @requires no this.top.heapEnv[def]
	 * @effects this.top.heapEnv' = this.top.heapEnv + def -> value
	 */
	public <T> void heapDef(int def, HeapExpression<T> value) { 
		top.heapDef(def, value);
	}
	
	/**
	 * Returns a string representation of this environment.
	 * @return a string representation of this environment
	 */
	public String toString() { 
		if (frames.empty()) return "EMPTY ENVIRONMENT\n";
		final List<String> fstrings = new ArrayList<String>(frames.size());
		for(Frame frame : frames) {
			fstrings.add(frame.toString());
		}
		final StringBuilder s = new StringBuilder();
		s.append("--------------\n");
		for(int i = fstrings.size()-1; i >= 0; i--) {
			s.append(fstrings.get(i));
			s.append("--------------\n");
		}
		return s.toString();
	}
	
	/**
	 * A symbolic execution frame for a method that has been translated
	 * by the {@linkplain Translator Miniatur translator} with respect
	 * to a given {@linkplain Environment environment} and {@linkplain ExpressionFactory initial heap}.   
	 * 
	 * @specfield call: lone SSAAbstractInvokeInstruction // invoke instruction, if any, that caused this frame to be dropped
	 * @specfield callInfo: one WalaCGNodeInformation // cg node info, if any, for the method being executed
	 * @specfield localVars: callInfo.cgNode.getIR().getSymbolTable().getMaxValueNumber() // SSA value numbers for local variables
	 * @specfield heapVars: callInfo.fieldSSA.getMaxHeapNumber() // SSA value numbers for heap variables
	 * @specfield localEnv: {i: int | 1 <= i <= localVars} ->lone (Node + RealExpression)
	 * @specfield heapEnv: {i: int | 1 <= i <= heapVars} ->lone HeapExpression<?>
	 * 
	 * @author Emina Torlak
	 */
	public static final class Frame {
		private final SSAAbstractInvokeInstruction call;
		private final WalaCGNodeInformation callInfo;
		private final Object[] localEnv;
		private final Object[] heapEnv;

		/**
		 * Constructs a context frame for the given call, info, and entry guard.
		 * @effects this.call' = call and this.callInfo' = callInfo and 
		 * no this.localEnv' and no this.heapEnv' and no this.guardedBlocks' and
		 * no this.returnVal' and no this.exceptionVal'
		 */
		Frame(SSAAbstractInvokeInstruction call, WalaCGNodeInformation callInfo) {
			this.call = call;
			this.callInfo = callInfo;
			final CGNode node = callInfo.cgNode();
			this.heapEnv = new Object[callInfo.fieldSSA().getMaxHeapNumber()];
			this.localEnv = new Object[node.getIR().getSymbolTable().getMaxValueNumber()];
		}
		
		/**
		 * Returns this.callInfo, if any.  Otherwise returns null.
		 * @return this.callInfo
		 */
		public final WalaCGNodeInformation callInfo() { return callInfo; }
		
		/**
		 * Returns this.call, if any.  Otherwise returns null.
		 * @return this.call
		 */
		public final SSAAbstractInvokeInstruction call() { return call; }
		
		/**
		 * Returns true if i is in [1..max]
		 * @return i >= 1 && i <= max
		 */
		private static final boolean inSSARange(int i, int max) {
			return i >= 1 && i <= max;
		}
		
		/**
		 * Returns the definition of the given local variable, or 
		 * null if it hasn't been defined.  
		 * @requires use in this.localVars
		 * @return this.localEnv[use]
		 */
		@SuppressWarnings("unchecked")
		public <T> T localUse(int use) { 
			assert inSSARange(use, localEnv.length);
			return (T)localEnv[use-1];
		}
		
		/**
		 * Assigns the given value to the specified variable.
		 * @requires def in this.stackVars
		 * @requires no this.localEnv[def]
		 * @effects this.localEnv' = this.localEnv + def -> value
		 */
		<T> void localDef(int def, T value) {
			assert inSSARange(def, localEnv.length);
			assert value != null;
			assert localEnv[def-1] == null;
			localEnv[def-1] = value;
		}

		/**
		 * Returns the definition of the given heap variable, or null
		 * if it hasn't been defined. If the given heap variable is an array, then the 
		 * returned value is an ArrayExpression; otherwise, it is an Expression.
		 * @requires use in this.heapVars
		 * @return this.heapEnv[use] 
		 */
		@SuppressWarnings("unchecked")
		public <T> HeapExpression<T> heapUse(int use) {
			assert inSSARange(use, heapEnv.length);
			return (HeapExpression<T>)heapEnv[use-1];
		}
		
		/**
		 * Assigns the given value to the specified variable.
		 * @requires def in this.heapVars
		 * @requires no this.heapEnv[def]
		 * @requires 
		 *  this.callInfo.fieldSSA.isArrayNumber(def) => 
		 *   value in ArrayExpression 
		 *  else 
		 *   value in Expression
		 * @effects this.heapEnv' = this.heapEnv + def -> value
		 */
		void heapDef(int def, HeapExpression<?> value) {
		  assert inSSARange(def,  heapEnv.length) : "bad def " + def + " for " + value;
		  assert value != null : "no value for " + callInfo.fieldSSA().getField(def);
		  assert heapEnv[def-1] == null;
		  heapEnv[def-1] = value;
		}

		/**
		 * Returns true if the given access is an access to a lexically scoped
		 * variable identified by the specified name and definer.
		 * @return name.equals(access.variableName) && definer.equals(access.variableDefiner);
		 */
		static boolean accessToLexVar(Access access, String name, String definer) {
			return name.equals(access.variableName) && definer.equals(access.variableDefiner);
		}
						
		/**
		 * Returns a string representation of this frame's contents.
		 * @return a string representation of this frame's contents.
		 */
		public String toString() { 
			final StringBuilder s = new StringBuilder();
			if (call()==null) {
				s.append("Method: " + callInfo().cgNode().getMethod() + "\n");
			} else {
				s.append("Call: " + call() + "\n");
			}
			s.append(" Local environment:\n");
			for(int i = 1, max = callInfo().cgNode().getIR().getSymbolTable().getMaxValueNumber(); i < max; i++) {
				Object val = localUse(i);
				if (val!=null)
					s.append("  " + (i+1) + ": " + val + "\n");
			}
			s.append(" Heap:\n");
			final FieldSSATable fieldSSA = callInfo().fieldSSA();
			for(int i = 1, max = fieldSSA.getMaxHeapNumber(); i <= max; i++) {
				Object val = heapUse(i);
				if (val!=null)
					s.append("  " + fieldSSA.getField(i) + "_" + (i) + ": " + val + "\n");
			}
			return s.toString();
		}
	}
}

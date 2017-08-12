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

import static com.ibm.wala.memsat.util.Strings.repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.java.ipa.callgraph.AstJavaSSAPropagationCallGraphBuilder.EnclosingObjectReferenceKey;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.HeapExpression;
import com.ibm.wala.memsat.representation.PhiExpression;
import com.ibm.wala.memsat.representation.RealExpression;
import com.ibm.wala.memsat.translation.Environment.Frame;
import com.ibm.wala.memsat.util.Nodes;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.util.ints.IndexedEntry;

/**
 * Provides access to a general translator that can be applied to any method.
 * In the absence of a specialized translator for a given method, an instance
 * of this class computes the method's translation by 
 * composing the translations of the individual instructions that comprise
 * the method's loop-unrolled body. If a specialized translator is available,
 * however, it will be used instead.
 * 
 * @author Emina Torlak
 */
@SuppressWarnings("unused")
public final class Translator implements MethodTranslator {

	private final SpecialTranslatorFactory specialTransls;
	
	/**
	 * Constructs a new generic method translator.
	 */
	Translator(SpecialTranslatorFactory specialTranslFactory) {
		this.specialTransls =specialTranslFactory;
	}

	/**
	 * Returns the translation of the top level method given by env.top.callInfo().
	 * @requires no env.top.call
	 * @return translation of the top level method given by env.top.callInfo().
	 */
	public static MethodTranslation translate(Environment env, MemoryInstructionHandler memoryHandler) { 
		assert env.top().call()==null;
		final Translator transl = new Translator(SpecialTranslatorFactory.factory(env.factory().options(), env.factory().info()));
		return transl.translate(Formula.TRUE, env, memoryHandler);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.translation.MethodTranslator#translate(kodkod.ast.Formula, com.ibm.wala.memsat.translation.Environment, com.ibm.wala.memsat.translation.MemoryInstructionHandler)
	 */
	public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
		final MethodReference method = env.top().callInfo().cgNode().getMethod().getReference();
		final MethodTranslator special = specialTransls.translatorFor(method);
		
//			System.err.println("method: " + method);
//			System.err.println("special: " + special);
		
		if (special==null) {
			return new TranslationVisitor(entryGuard,env,memoryHandler).execute();
		} else 
			return special.translate(entryGuard, env, memoryHandler);
	}
	
	/*------------------ GENERIC (INSTRUCTION-BY-INSTRUCTION) TRANSLATION VISITOR ------------------ */
	
	/** Instruction-by-instruction translation visitor. */
	private class TranslationVisitor extends AbstractInstructionVisitor<MethodTranslation> {
		private int instIdx = -1;
		
		private final Set<Formula> assertions, assumes;
		private final Set<TranslationWarning> warnings;
		private final Set<MethodReference> memoryInstructions;
		private final Environment env;
		
		private final MemoryInstructionHandler memoryHandler;
		private final GuardHandler guardHandler;
		
		private final PhiExpression<Expression> exceptionPhi;
		private final PhiExpression<Object> returnPhi;
		private final Map<SSAGetCaughtExceptionInstruction, PhiExpression<Expression>> catcherPhis;
		
		private final ExpressionFactory factory;
		private final WalaCGNodeInformation callInfo;
		private final CGNode node;
		private final FieldSSATable fieldSSA;
		
		
		/** Constructs a fresh translation visitor for the top environment frame,
		 * with respect to the given entry guard and memory handler. */
		TranslationVisitor(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) { 
			
			this.env = env;
			this.assertions = new LinkedHashSet<Formula>();
			this.assumes = new LinkedHashSet<Formula>();
			this.warnings = new LinkedHashSet<TranslationWarning>();
			
			this.memoryHandler = memoryHandler;
			this.guardHandler = new GuardHandler(entryGuard, env, warnings);
			
			this.factory = env.factory();
			this.callInfo = env.top().callInfo();
			this.node = callInfo.cgNode();
			this.fieldSSA = callInfo.fieldSSA();
			
			if (factory.options().memoryModel()!=null) { 
				this.memoryInstructions = factory.options().memoryModel().memoryInstructions();
			} else {
				this.memoryInstructions = Collections.emptySet();
			}
			
			this.exceptionPhi = factory.valuePhi(IRType.OBJECT);
			final IRType returnType = IRType.convert(node.getMethod().getReturnType());
			this.returnPhi = (returnType==null) ? null : factory.valuePhi(returnType);
			this.catcherPhis = new LinkedHashMap<SSAGetCaughtExceptionInstruction, PhiExpression<Expression>>();
		}
		
		/** 
		 * Translates the method in the topmost frame of this.env, pops the frame
		 * off and returns it, along with return, exception, etc., values wrapped
		 * in a Result object.
		 * @effects adds translations of callInfo.allInstructions() to this.env.top, and pops it off
		 * @return Result object storing the translation of the method in the topmost
		 * frame of this.env, as computed by this translation visitor
		 **/
		@SuppressWarnings("unchecked")
		protected final MethodTranslation execute() { 
			
			
			final int spaces = env.callStack().size();
			final String indent = repeat("  ", spaces);
			
			/*
			System.out.println();
			System.out.println(indent+"----------all instructions for "+callInfo.cgNode()+"------------");
			System.out.println(prettyPrint(env.top().callInfo().cgNode().getIR(), spaces*2));
			System.out.println(indent+"----------end all instructions for "+callInfo.cgNode()+"------------");
			System.out.println(indent+"----------relevant instructions for "+callInfo.cgNode()+"------------");
					
			for(Iterator<? extends IndexedEntry<SSAInstruction>> itr = callInfo.relevantInstructions(); itr.hasNext(); ) { 
				IndexedEntry<SSAInstruction> inst = itr.next();
				System.out.println(indent+inst.index() + ":: "+inst.value());
			}
			
			System.out.println(indent+"----------end relevant instructions for "+callInfo.cgNode()+"------------");
			*/
			
			for(Iterator<? extends IndexedEntry<SSAInstruction>> itr = callInfo.relevantInstructions(); itr.hasNext(); ) { 
				IndexedEntry<SSAInstruction> inst = itr.next();
//				System.out.println(indent+"TRANSLATING "+inst.index() + ":: "+inst.value());
				this.instIdx = inst.index();
				inst.value().visit(this);
			}
			
//			System.out.println(indent+"------------------------------------\n");
			
//			System.out.println("----------def use------------");
//			DefUse du = env.top().callInfo().cgNode().getDU();
//			for(int i = 1; i <= env.top().callInfo().cgNode().getIR().getSymbolTable().getMaxValueNumber(); i++) {
//				System.out.println(i + " = " + env.localUse(i) + "; definer: " + du.getDef(i));
//			}
//			System.out.println("-----------------------------");
//			System.out.println();
			
			return new MethodTranslation() {
				
				final Formula exitGuard = guardHandler.normalExitGuard();
				// if returnValue phi is empty, then the return instruction(s) have been sliced out
				// and therefore irrelevant, so we set the return value to null
				final Object returnValue = returnPhi==null || returnPhi.size()==0 ? null : returnPhi.value(); 
				final Expression exceptionValue = exceptionPhi.size()==0 ? null : exceptionPhi.value(); 
				final Frame frame = env.pop();

				public Set<Formula> assertions() { return assertions; }
				public Set<Formula> assumptions() { return assumes; }
				public Expression exceptionValue() { return exceptionValue; }
				public Frame frame() { return frame; }
				public Formula normalExitGuard() { return exitGuard; }
				public <T> T returnValue() { return (T) returnValue; }
				public Set<TranslationWarning> warnings() { return warnings; } 
			};
		}

		
		/*------------------ ASSERTION INSTRUCTION ------------------ */
		
		/** @effects this.assertions'.add( this.entryGuard(blockFor(inst)) and ![[inst.getUse(0)]] ) */
		public void visitAssert(AstAssertInstruction inst) {
			final Formula assertion = env.boolUse(inst.getUse(0));
			if (inst.isFromSpecification()) {
				if (env.factory().options().assertsAreAssumptions())
					assertions.add(Nodes.simplify(guardHandler.absoluteEntryGuard(inst).implies(assertion)));
				else 
					assertions.add(Nodes.simplify(guardHandler.absoluteEntryGuard(inst).and(assertion.not())));			
			} else {
				assumes.add(Nodes.simplify(guardHandler.absoluteEntryGuard(inst).implies(assertion)));
			}
		}
	
		/*------------------ RETURN / THROW / CATCH INSTRUCTIONS ------------------ */
		
		/** @effects updates this.top.returnVal with [[inst]] */
		public final void visitReturn(SSAReturnInstruction inst) {
			if (returnPhi != null) { 
				final Formula guard = guardHandler.relativeEntryGuard(inst);
				returnPhi.add(guard, env.localUse(inst.getUse(0)));
			}
		}
	
		/** @effects updates this.top.exceptionVal with [[inst]] */
		public final void visitThrow(SSAThrowInstruction inst) { 
			visitThrower(inst, env.refUse(inst.getUse(0))); }
			
		/** @effects updates exceptionPhi with [[exception]] if the block
		 * holding the given instruction is an exit block; otherwise, updates the
		 * defs of relevant SSAGetCaughtExceptionInstruction(s) */
		private final void visitThrower(SSAInstruction inst, Expression exception) { 
			final SSACFG.BasicBlock block = guardHandler.blockFor(inst);
			final Formula guard = guardHandler.relativeEntryGuard(block);
			for(IBasicBlock<?> SB : node.getIR().getControlFlowGraph().getExceptionalSuccessors(block)) {
				if (SB.isExitBlock()) { 
					exceptionPhi.add(guard, exception);
				} else {
					SSAGetCaughtExceptionInstruction catcher = (SSAGetCaughtExceptionInstruction)((SSACFG.ExceptionHandlerBasicBlock)SB).getCatchInstruction();
					PhiExpression<Expression> catcherPhi = catcherPhis.get(catcher);
					if (catcherPhi==null) { 
						catcherPhi = factory.valuePhi(IRType.OBJECT);
						catcherPhis.put(catcher, catcherPhi);
					}
					catcherPhi.add(guard, exception);
				}
			}
		}
			
		/** @effects this.env.localDef(inst.getDef(), catcherPhis.get(inst).value())  */
		public final void visitGetCaughtException(SSAGetCaughtExceptionInstruction inst) {
			// we can remove the phi for the given catcher; it should no longer be needed
			final PhiExpression<Expression> catcherPhi = catcherPhis.remove(inst);
			assert catcherPhi != null;
			env.localDef(inst.getDef(), catcherPhi.value());
		}
	
		/*------------------ INVOKE INSTRUCTIONS ------------------ */
		
		/** @effects sets the heap values defined by the given instruction to 
		 * their corresponding uses; in other, the instruction is not symbolically executed */
		private final void notCalled(SSAAbstractInvokeInstruction inst) { 
			if (inst.hasDef()) { 
				int def = inst.getDef();
				env.localDef(def, factory.constants().defaultValue(env.top().callInfo().typeOf(def)));
			}
			final int[] hUses = fieldSSA.getUses(inst);
			for(int hDef : fieldSSA.getDefs(inst)) {
				PointerKey field = fieldSSA.getField(hDef);
				for(int hUse : hUses) {
					if (field.equals(fieldSSA.getField(hUse))) { 
						env.heapDef(hDef, env.heapUse(hUse));
						break;
					}
				}
				assert env.heapUse(hDef)!=null;
			}
			// TODO:  should the guard here be true or false?
			guardHandler.addCallExitGuard(inst, Formula.TRUE);
		}
		
		/**
		 * Updates the given return, exception and heap phis
		 * with their type-guarded translations, if the given invocation can be handled.  
		 * Otherwise does nothing.
		 * @effects updates this.assertions, this.env, and this.warnings, if needed
		 * @effects updates the given return, exception and heap phis
		 * with their type-guarded translations, if the given invocation can be handled. 
		 * @return true if the invocation was handled, otherwise false. 
		 **/
		private boolean visitInvocation(SSAAbstractInvokeInstruction call, CGNode target, 
			PhiExpression<Object> callReturn, PhiExpression<Expression> callException,
			PhiExpression<Formula> guardPhi, Map<PointerKey, PhiExpression<HeapExpression<Object>>> callHeap) {
			
			final IMethod method = target.getMethod();
			final WalaInformation info = factory.info();
			
			if (!specialTransls.hasTranslatorFor(method.getReference()) &&
				!(method instanceof AstMethod) &&
				!(method instanceof JavaScriptSummarizedFunction)) { 
				warn(call, "cannot call unexpected method " + target);
				return false;
			}
				
			final Formula typeGuard;
			
			if (call.isDispatch()) { 
				final List<Formula> typeGuards = new ArrayList<Formula>();
				for(InstanceKey key : info.pointsTo(callInfo.pointerKeyFor(call.getUse(0)))) {
					if (info.analysisOptions().getMethodTargetSelector().
						getCalleeTarget(target, call.getCallSite(), key.getConcreteType())
						== target.getMethod()) {
						Expression type = factory.constants().valueOf(key);
						if (type != null) {
							typeGuards.add(env.refUse(call.getUse(0)).in(type));
						}
					}
				}
				
				if (typeGuards.isEmpty()) { 
					warn(call, "cannot handle call to method " + target);
					return false;
				}
				
				typeGuard = Formula.or(typeGuards);
			} else {
				typeGuard = Formula.TRUE;
			}
			
			final MethodTranslation result = translate(guardHandler.absoluteEntryGuard(call).and(typeGuard),
					env.push(call, info.cgNodeInformation(target)), memoryHandler);
			
			// get all assertions and warnings
			assertions.addAll(result.assertions());
			warnings.addAll(result.warnings());
			
			// update normal exit guard for the inst
			guardPhi.add(typeGuard, result.normalExitGuard());
			
			// handle return value, if any
			if (result.returnValue()!=null) { 
				callReturn.add(typeGuard, result.returnValue());
			}
			
			// handle exception value, if any
			if (result.exceptionValue()!=null) { 
				callException.add(typeGuard, result.exceptionValue());
			}
			
			// handle heap updates, if any
			final Frame frame = result.frame();	
			for(int hDef : fieldSSA.getDefs(call)) { 
				PointerKey field = fieldSSA.getField(hDef);
				int v = frame.callInfo().fieldSSA().getExitValue(field);
				HeapExpression<Object> fieldValue = null;
				if (v != -1)
					fieldValue = frame.heapUse(v);
				else
					fieldValue = env.heapUse(fieldSSA.getUse(call, fieldSSA.getUse(call, field))) ;
					//HeapExpression<Object> fieldValue = frame.heapUse(frame.callInfo().fieldSSA().getExitValue(field));
				
				
				PhiExpression<HeapExpression<Object>> fieldPhi = callHeap.get(field);
				if (fieldPhi==null) { 
					fieldPhi = factory.heapPhi(typeGuard, fieldValue);
					callHeap.put(field, fieldPhi);
				} else {
					fieldPhi.add(typeGuard, fieldValue);
				}
				
			}
						
			return true;
		}
	
		/** @effects translates the code for the given call instruction and sets inst.getDef(),
		 * if any, to the returned value */
		public final void visitAbstractInvoke(SSAAbstractInvokeInstruction inst) { 
			final WalaInformation info = factory.info();
			final Set<CGNode> targets = info.callGraph().getPossibleTargets(env.top().callInfo().cgNode(), inst.getCallSite());
		
			// a memory instruction (no-op)
			if (memoryInstructions.contains(inst.getDeclaredTarget()) || 
				(targets.size()==1 && memoryInstructions.contains(targets.iterator().next().getMethod().getReference()))) { 
				memoryHandler.handleSpecialInvoke(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
				return;
			}
			
			// unreached call site
			if (targets.isEmpty()) { 
				warn(inst, "no targets");
				notCalled(inst); 
				return;
			} 
			
			// non-static call to unknown type
			if (!inst.getCallSite().isStatic() && 
				info.pointsTo(callInfo.pointerKeyFor(inst.getUse(0))).isEmpty()) { 
				warn(inst, "non-static call to unknown type");
				notCalled(inst);
				return;
			} 
				
			assert inst.isDispatch() || targets.size()==1;	
			final int limit = factory.options().recursionLimit();					
			
			boolean called = false;
			
			final PhiExpression<Expression> callException = factory.valuePhi(IRType.OBJECT);
			
			final IRType retType = IRType.convert(inst.getDeclaredTarget().getReturnType());
			final PhiExpression<Object> callReturn = retType==null ? null : factory.valuePhi(retType);
			final PhiExpression<Formula> callGuard = factory.valuePhi(IRType.BOOLEAN);
			
			final Map<PointerKey,PhiExpression<HeapExpression<Object>>> callHeap = 
				new LinkedHashMap<PointerKey, PhiExpression<HeapExpression<Object>>>();
			
			for(CGNode target : targets) { 
				if (env.recursionCount(target) < limit) { 
					called |= visitInvocation(inst, target, callReturn, callException, callGuard, callHeap);
				} 
			}
			
			if (!called) { 
				notCalled(inst);
//				System.out.println("NOT CALLED " + inst + " FROM " + callInfo.cgNode());
			} else { 
				if (callGuard.size()>0) { 
					guardHandler.addCallExitGuard(inst, callGuard.value());
				}
				if (inst.hasDef() && callReturn.size()>0) {
					// if callReturn is empty, then all of the return instructions for each
					// of the targets have been sliced out, and therefore irrelevant.
					env.localDef(inst.getDef(), callReturn.value());
				}
				if (callException.size() > 0) {
					env.localDef(inst.getException(), callException.value());
					visitThrower(inst, callException.value());
				}
				for(int hDef : fieldSSA.getDefs(inst)) { 
					env.heapDef(hDef, callHeap.get(fieldSSA.getField(hDef)).value());
				}
			}
		}
		
		/*------------------ JOIN INSTRUCTIONS ------------------ */
		
		/** @effects sets the heap or local def for the given phi instruction in this.env */
		public final void visitPhi(SSAPhiInstruction inst) {
			final Formula[] useGuards = guardHandler.phiUseGuards(inst); 
			if (fieldSSA.isHeapPhi(inst)) {
				final PhiExpression<HeapExpression<Object>> phi = 
					factory.heapPhi(useGuards[0], env.heapUse(inst.getUse(0)));
				for(int i = 1; i < useGuards.length; i++) { 
					phi.add(useGuards[i], env.heapUse(inst.getUse(i)));
				}
				assert phi.size() > 0;
				env.heapDef(inst.getDef(), phi.value());
			} else {
				final PhiExpression<Object> phi = factory.valuePhi(callInfo.typeOf(inst.getDef()));
				for(int i = 0; i < useGuards.length; i++) { 
					phi.add(useGuards[i], env.localUse(inst.getUse(i)));
				}
				assert phi.size() > 0;
				env.localDef(inst.getDef(), phi.value());	
			}
		}
	
		/** @effects env.localDef(inst.getDef(), [[ inst.getUse(0) ]] ) */
		public final void visitPi(SSAPiInstruction inst) {
			env.localDef(inst.getDef(), env.localUse(inst.getUse(0)));
		}
	
		/*------------------ MONITOR INSTRUCTION ------------------ */
		
		/** @effects calls memoryHandler.handleMonitor(inst, env) */
		public final void visitMonitor(SSAMonitorInstruction inst) {
			memoryHandler.handleMonitor(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
		}
	
		/*------------------ HEAP & LEXICAL ACCESS INSTRUCTIONS ------------------ */
		
		/** @effects calls this.memoryHandler.handleGet(inst, env) */
		public final void visitGet(SSAGetInstruction inst) {
			memoryHandler.handleGet(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
		}
		
		/** @effects calls this.memoryHandler.handlePut(inst, env) */
		public final void visitPut(SSAPutInstruction inst) {
			memoryHandler.handlePut(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
		}
		
		/** @effects calls this.memoryHandler.handleArrayLoad(inst, env) */
		public final void visitArrayLoad(SSAArrayLoadInstruction inst) {
			memoryHandler.handleArrayLoad(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
		}
	
		/** @effects calls this.memoryHandler.handleArrayLoad(inst, env) */
		public final void visitArrayStore(SSAArrayStoreInstruction inst) {
			memoryHandler.handleArrayStore(instIdx, inst, guardHandler.absoluteEntryGuard(inst), env);
		}
		
		/** @effects this.env.localDef(inst.getDef(), [[ inst ]] )*/
		public final void visitArrayLength(SSAArrayLengthInstruction inst) {
			//System.err.println(this.node.getIR().toString());
			final PhiExpression<IntExpression> phi = factory.valuePhi(IRType.INTEGER);
			final Expression arrayObj = env.refUse(inst.getUse(0)); 
			for(int use : fieldSSA.getUses(inst)) {
				FieldExpression<IntExpression> fieldPiece = env.fieldUse(use); 
				phi.add(arrayObj.in(fieldPiece.instances()), fieldPiece.read(arrayObj));
			}
			assert phi.size() > 0;
			env.localDef(inst.getDef(), phi.value());
		}
		
		/** @effects visitGet(inst) */
		public final void visitAstGlobalRead(AstGlobalRead inst) { visitGet(inst); }
	
		/** @effects visitPut(inst) */
		public final void visitAstGlobalWrite(AstGlobalWrite inst) { visitPut(inst); }
		
		/** @effects all d: inst.getAccesses | this.env.localDef(d.valueNumber, this.env.lexUse(d) )*/
		public final void visitAstLexicalRead(AstLexicalRead inst) {
			int i = 0;
			int[] uses = env.top().callInfo().fieldSSA().getUses(inst);
			for(Access access : inst.getAccesses()) {
				env.localDef(access.valueNumber, env.fieldUse(uses[i++]).read(null));
			}
		}
		
		/*------------------ OBJECT / CLASS / TYPE INSTRUCTIONS ------------------ */
		/** @effects updates local environment, and heap environment if needed */
		public final void visitNew(SSANewInstruction inst) {
			final Set<InstanceKey> defKeys = factory.info().pointsTo(callInfo.pointerKeyFor(inst.getDef()));
			assert defKeys.size()==1;
			final InstanceKey defKey = defKeys.iterator().next();
			final Expression obj = env.instantiate(defKey);
			env.localDef(inst.getDef(), obj);
			
			final TypeReference type = inst.getConcreteType();
			
			if (type.isArrayType()) { // update length field
				final FieldExpression<IntExpression> arrLen = env.fieldUse(fieldSSA.getUse(inst, 0));
				final IntExpression len = env.intUse(inst.getUse(0));
				env.heapDef(fieldSSA.getDef(inst, 0), arrLen.write(obj, len));
			} else if (!node.getMethod().isStatic()) { // update any enclosing reference fields
				final IClass declaringClass = node.getMethod().getDeclaringClass();
				final IClassHierarchy cha = factory.info().callGraph().getClassHierarchy();
				
				for(int hUse : fieldSSA.getUses(inst)) { 
					FieldExpression<Expression> fieldValue = env.fieldUse(hUse);
					EnclosingObjectReferenceKey field = (EnclosingObjectReferenceKey)fieldSSA.getField(hUse);
					int hDef = fieldSSA.getDef(inst, fieldSSA.getDef(inst, field));
					IClass enclosingClass = factory.info().pointsTo(field).iterator().next().getConcreteType();
					if (cha.isSubclassOf(declaringClass, enclosingClass)) { 
						env.heapDef(hDef, fieldValue.write(obj, env.refUse(1)));
					} else {
						env.heapDef(hDef, fieldValue.write(obj, fieldValue.read(env.refUse(1))));
					}
				}
			}
		}
		
		/**
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] in [[inst.getCheckedType()]])
		 */
		public final void visitInstanceof(SSAInstanceofInstruction inst) {
			env.localDef(inst.getDef(), env.refUse(inst.getUse(0)).in(factory.constants().valueOf(inst.getCheckedType())));
		}
	
		/** @effects this.env.localDef(inst.getDef(), [[ inst.getUse(0) ]] )*/
		public final void visitCheckCast(SSACheckCastInstruction inst) {
			env.localDef(inst.getDef(), env.refUse(inst.getUse(0)));		
		}
		
		/** 
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] . [[inst.getEnclosingType()]] ) 
		 **/
		public final void visitEnclosingObjectReference(EnclosingObjectReference inst) {
			final PhiExpression<Expression> phi = factory.valuePhi(IRType.OBJECT);
			final Expression obj = env.refUse(inst.getUse(0));
			for(int use : fieldSSA.getUses(inst)) {
				FieldExpression<Expression> fieldPiece = env.fieldUse(use);
				phi.add(obj.in(fieldPiece.instances()), fieldPiece.read(obj));
			}
			assert phi.size() > 0;
			env.localDef(inst.getDef(), phi.value());
		}
			
		/*------------------ COMPARISON / ARITHMETIC / CONVERSION INSTRUCTIONS ------------------ */
		
		/** 
		 * @effects this.env.localDef(inst.getDef(), [[inst.op]] [[inst.getUse(0)]] ) 
		 **/
		public final void visitUnaryOp(SSAUnaryOpInstruction inst) {
			if (inst.getOpcode() == UnaryOpInstruction.Operator.NEG) {
				
				final int use = inst.getUse(0);						
				switch(env.top().callInfo().typeOf(use)) {	
				case BOOLEAN	: env.localDef(inst.getDef(), env.boolUse(use).not()); break;
				case INTEGER	: env.localDef(inst.getDef(), env.intUse(use).negate()); break;
				case REAL 		: env.localDef(inst.getDef(), env.realUse(use).negate()); break;
				default 		: throw new AssertionError("unreachable");
				}
				
			} else {
				warnOp(inst);
			}
		}
		
		/** 
		 * @requires inst.getOperator() instanceof ShiftInstruction.Operator
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] [[inst.op]] [[inst.getUse(1)]] ) 
		 **/
		private void visitBinaryShiftOp(SSABinaryOpInstruction inst) { 
			final int def = inst.getDef(), use0 = inst.getUse(0), use1 = inst.getUse(1);
			final IRType type = callInfo.typeOf(def);
			final ShiftInstruction.Operator op = (ShiftInstruction.Operator)inst.getOperator();
			
			switch(type) { 			
			case INTEGER : 
				final IntExpression i0 = env.intUse(use0), i1 = env.intUse(use1);
				switch(op) { 
				case SHL 	: env.localDef(def, i0.shl(i1)); break;
				case SHR 	: env.localDef(def, i0.sha(i1)); break;
				case USHR 	: env.localDef(def, i0.shr(i1)); break;
				default 	: throw new AssertionError("unreachable"); }
				break;				
			default : throw new AssertionError("unreachable");
			}
		}
		
		/** 
		 * @requires inst.getOperator() instanceof BinaryOpInstruction.Operator
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] [[inst.op]] [[inst.getUse(1)]] ) 
		 **/
		private void visitBinaryArithmeticOp(SSABinaryOpInstruction inst) { 
			final int def = inst.getDef(), use0 = inst.getUse(0), use1 = inst.getUse(1);
			final IRType type = callInfo.typeOf(def);
			final BinaryOpInstruction.Operator op = (BinaryOpInstruction.Operator)inst.getOperator();
			
//			System.out.println(inst + ", " + use0 + "=" + env.localUse(use0) + ", " + use1 + "=" + env.localUse(use1));
//			System.out.println("type of "+ use0 + "="+callInfo.typeOf(use0) + ", type of " + use1 +"="+ callInfo.typeOf(use1) + ", type of " +def+"=" + callInfo.typeOf(def));
			
			switch(type) { 			
			case INTEGER : 
				final IntExpression i0 = env.intUse(use0), i1 = env.intUse(use1); 
				switch(op) { 
				case ADD	: env.localDef(def, i0.plus(i1)); break;
				case DIV	: env.localDef(def, i0.divide(i1)); break;
				case MUL	: env.localDef(def, i0.multiply(i1)); break;
				case AND	: env.localDef(def, i0.and(i1)); break;
				case OR		: env.localDef(def, i0.or(i1)); break;
				case REM	: env.localDef(def, i0.modulo(i1)); break; 
				case SUB	: env.localDef(def, i0.minus(i1)); break;
				case XOR	: env.localDef(def, i0.xor(i1)); break;
				default		: throw new AssertionError("unreachable"); }
				break;				
			case REAL : 
				final RealExpression r0 = env.realUse(use0), r1 = env.realUse(use1);
				switch(op) { 
				case ADD	: env.localDef(def, r0.plus(r1)); break;
				case DIV	: env.localDef(def, r0.divide(r1)); break;
				case MUL	: env.localDef(def, r0.multiply(r1)); break;
				case REM	: env.localDef(def, r0.modulo(r1)); break; 
				case SUB	: env.localDef(def, r0.minus(r1)); break;
				default		: throw new AssertionError("unreachable"); }
				break;				
			default : throw new AssertionError("unreachable");
			}
		}
		
		/** 
		 * @requires inst.getOperator() instanceof AstConstants.BinaryOp
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] [[inst.op]] [[inst.getUse(1)]] ) 
		 **/
		private void visitBinaryComparisonOp(SSABinaryOpInstruction inst) {
			final int def = inst.getDef(), use0 = inst.getUse(0), use1 = inst.getUse(1);
			final IRType type = callInfo.typeOf(use0);
			final CAstBinaryOp op = (CAstBinaryOp)inst.getOperator();
			
			switch(type) { 	
			case OBJECT : 
				final Expression obj0 = env.refUse(use0), obj1 = env.refUse(use1);
				switch(op) { 
				case EQ 	: env.localDef(def, obj0.eq(obj1)); break;
				case NE		: env.localDef(def, obj0.eq(obj1).not()); break;
				case CONCAT : warnOp(inst); env.localDef(def, factory.constants().nil()); break;
				default		: throw new AssertionError("unreachable"); }
				break;				
			case BOOLEAN : 
				final Formula b0 = env.boolUse(use0), b1 = env.boolUse(use1);
				switch(op) {
				case EQ		: env.localDef(def, b0.iff(b1)); break;
				case NE		: env.localDef(def, b0.iff(b1).not()); break;
				default		: throw new AssertionError("unreachable"); }
				break;				
			case INTEGER : 
				final IntExpression i0 = env.intUse(use0), i1 = env.intUse(use1);
				switch(op) {
				case EQ		: env.localDef(def, i0.eq(i1)); break;
				case NE		: env.localDef(def, i0.eq(i1).not()); break;
				case LT		: env.localDef(def, i0.lt(i1)); break;
				case LE		: env.localDef(def, i0.lte(i1)); break;
				case GT		: env.localDef(def, i0.gt(i1)); break;
				case GE		: env.localDef(def, i0.gte(i1)); break;
				default		: throw new AssertionError("unreachable"); }
				break;			
			case REAL : 
				final RealExpression r0 = env.realUse(use0), r1 = env.realUse(use1);
				switch(op) {
				case EQ		: env.localDef(def, r0.eq(r1)); break;
				case NE		: env.localDef(def, r0.eq(r1).not()); break;
				case LT		: env.localDef(def, r0.lt(r1)); break;
				case LE		: env.localDef(def, r0.lte(r1)); break;
				case GT		: env.localDef(def, r0.gt(r1)); break;
				case GE		: env.localDef(def, r0.gte(r1)); break;
				default		: throw new AssertionError("unreachable"); }
				break;				
			default : throw new AssertionError("unreachable");
			}
		}
		
		/** 
		 * @effects this.env.localDef(inst.getDef(), [[inst.getUse(0)]] [[inst.op]] [[inst.getUse(1)]] ) 
		 **/
		public final void visitBinaryOp(SSABinaryOpInstruction inst) {
			final BinaryOpInstruction.IOperator op = inst.getOperator();
			if (op instanceof CAstBinaryOp) 
				visitBinaryComparisonOp(inst);
			else if (op instanceof BinaryOpInstruction.Operator)
				visitBinaryArithmeticOp(inst);
			else if (op instanceof ShiftInstruction.Operator) 
				visitBinaryShiftOp(inst);
			else 
				throw new AssertionError("unreachable");
		}
		
		/** @effects this.env.localDef(inst.getDef(), ([[inst.getToType()]]) [[inst.getUse(0)]]) */
		public void visitConversion(SSAConversionInstruction inst) {
			final IRType from = IRType.convert(inst.getFromType());
			final IRType to = IRType.convert(inst.getToType());
			final int def = inst.getDef(), use = inst.getUse(0);
			
			switch(from) {	
			case INTEGER : 
				final IntExpression i = env.intUse(use);
				switch(to) {
				case INTEGER 	: env.localDef(def, i); break;
				case REAL 		: env.localDef(def, RealExpression.fromIntExpr(i)); break;
				default			: throw new AssertionError("unreachable"); }
				break;			
			case REAL : 
				final RealExpression r = env.realUse(use);
				switch(to) {
				case INTEGER 	: env.localDef(def, r.toIntExpr()); break;
				case REAL 		: env.localDef(def, r); break;
				default			: throw new AssertionError("unreachable"); }
				break;			
			default : throw new AssertionError("unreachable");
			}
			
		}

		/*------------------ EMPTY METHODS ------------------ */
		/** Does nothing; echo only generates output */
		public final void visitEcho(AstEchoInstruction instruction) {}
		/** Does nothing; control dependence handled by guard computation. */
		public final void visitGoto(SSAGotoInstruction instruction) {}
		
		/** Does nothing; control dependence handled by guard computation. */
		public final void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {}
	
		/** Does nothing; control dependence handled by guard computation. */
		public final void visitSwitch(SSASwitchInstruction instruction) {}
	
		/**	Does nothing, lexical writes handled by call/return mechanism. */
		public final void visitAstLexicalWrite(AstLexicalWrite instruction) {}
			
		/*------------------ HELPER METHODS ------------------ */
	
		/** @effects adds the given warning for the specified instruction to this.warnings*/
		final void warn(SSAInstruction inst, String msg) { 
			warnings.add(new TranslationWarning(node.getIR(), inst, msg));
		}
		
		/** @effects adds the warning "operator not modelled" to this.warnings. */
		final void warnOp(SSAInstruction inst) { 
			warn(inst, "operator not modelled");
		}	
	}
}

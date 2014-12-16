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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.NotFormula;

import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.Util;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.memsat.frontEnd.DependenceGraph;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;

/**
 * Handles the caching and computation of guards during
 * the translation of a given method.
 * 
 * @specfield methodEntryGuard: Formula // the guard for entering this.node
 * @specfield node: CGNode // the method being translated
 * @specfield frame: Frame // the translation frame
 * @specfield callGuards: SSAAbstractCallInstruction ->lone Formula // call guards
 * @invariant frame.callInfo.cgNode = node
 * 
 * @author etorlak
 */
final class GuardHandler {
	private static enum EdgeType {
		EXCEPTIONAL,
		NORMAL,
		NOT_APPLICABLE;
	}
	
	private final Environment env;
	private final Formula methodEntryGuard;
	private final Map<SSACFG.BasicBlock, Formula> blockEntryGuards;
	private final Map<SSAAbstractInvokeInstruction, Formula> callExitGuards;
	private final Map<SSAInstruction, SSACFG.BasicBlock> instructionToBlock;
	private final Map<Expression, Formula> nonNullGuards;
	private final CGNode node;
	private final FieldSSATable fieldSSA;
	private final Set<TranslationWarning> warnings;
	private final Expression nil;
	
	
	/**
	 * Creates a GuardHandler for the translation of env.top().callInfo().cgNode().
	 * Any warnings generated during guard computation will be stored in the provided set.
	 * @effects  this.methodEntryGuard' = entryGuard and this.frame' = env.top and 
	 * this.node = this.frame'.callInfo.cgNode
	 */
	GuardHandler(Formula entryGuard, Environment env, Set<TranslationWarning> warnings) {
		this.methodEntryGuard = entryGuard;
		this.env = env;
		this.warnings = warnings;
		this.node = env.top().callInfo().cgNode();
		this.fieldSSA = env.top().callInfo().fieldSSA();
		this.blockEntryGuards = new LinkedHashMap<SSACFG.BasicBlock, Formula>();
		this.callExitGuards = new LinkedHashMap<SSAAbstractInvokeInstruction, Formula>();
		this.instructionToBlock = new LinkedHashMap<SSAInstruction, SSACFG.BasicBlock>();
		this.nonNullGuards = new LinkedHashMap<Expression, Formula>();
		this.nil = env.factory().constants().nil();
		
		for(Iterator<ISSABasicBlock> bbs = node.getIR().getControlFlowGraph().iterator(); bbs.hasNext(); ) {
			SSACFG.BasicBlock bb = (SSACFG.BasicBlock)bbs.next();
			for(Iterator<SSAInstruction> insts = bb.iterator(); insts.hasNext(); ) {
				instructionToBlock.put(insts.next(), bb);
			}
			for(Iterator<SSAPhiInstruction> insts = fieldSSA.getPhiNodes(bb); insts.hasNext(); ) {
				instructionToBlock.put(insts.next(), bb);
			}
		}
	}
	
	/**
	 * Adds the given guard to the given call.
	 * @requires call in this.node.getIR().getInstructions()
	 * @requires no this.callGuards[call]
	 * @effects this.callGuards' = this.callGuards + call->guard
	 */
	final void addCallExitGuard(SSAAbstractInvokeInstruction call, Formula guard) { 
		assert !callExitGuards.containsKey(call);
		callExitGuards.put(call, guard);
	}
	
	/**
	 * Returns the absolute entry guard for the basic block that contains
	 * the given instruction; the absolute guard is the conjunction of the 
	 * methodEntryGuard and the relativeEntryGuard for the given instruction.
	 * @requires inst in this.node.getIR().getInstructions()
	 * @return this.methodEntryGuard && relativeEntryGuard(inst)
	 */
	final Formula absoluteEntryGuard(SSAInstruction inst) { 
		return methodEntryGuard.and(relativeEntryGuard(inst));
	}
	
	/**
	 * Returns the relative entry guard for the basic block (from this.cfg)
	 * that contains the given instruction; the returned formula doesn't 
	 * include the guard for the call to the analyzed method.
	 * @requires inst in this.node.getIR().getInstructions()
	 * @return the entry guard for the basic block that contains the 
	 * given instruction
	 */
	final Formula relativeEntryGuard(SSAInstruction inst) {
		return relativeEntryGuard(blockFor(inst));
	}
	
	/**
	 * Returns the normal exit guard for this.node.getIR().getControlFlowGraph().
	 * @return normal exit guard for this.node.getIR().getControlFlowGraph()
	 */
	final Formula normalExitGuard() { 
		final List<Formula> normalExits = new ArrayList<Formula>();
		
		final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = node.getIR().getControlFlowGraph();
		final SSACFG.BasicBlock exit = (SSACFG.BasicBlock)cfg.exit();
		
		for(ISSABasicBlock normal : cfg.getNormalPredecessors(exit)) { 
			if (cfg.getSuccNodeCount(normal) == 1) {
				normalExits.add(relativeEntryGuard((SSACFG.BasicBlock)normal));
			} else {
				normalExits.add(
						relativeEntryGuard((SSACFG.BasicBlock)normal)
						.and(edgeGuard((SSACFG.BasicBlock)normal,exit,EdgeType.NORMAL)));
			}
		}
		assert !normalExits.isEmpty();
		return Formula.or(normalExits);
	}
	
	/**
	 * Returns relative guards for each use of the given phi instruction.
	 * In particular, the ith Formula in the return array stores the 
	 * guard for inst.getUse(i).
	 * @requires inst in this.node.getIR().getInstructions()
	 * @return relative guards for each use of the given phi instruction
	 */
	final Formula[] phiUseGuards(SSAPhiInstruction inst) { 
		final Formula[] ret = new Formula[inst.getNumberOfUses()];
		final SSACFG.BasicBlock bb = blockFor(inst);
		final EdgeType type = bb.isExitBlock()? EdgeType.NORMAL: EdgeType.NOT_APPLICABLE;
		final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = node.getIR().getControlFlowGraph();
		for(Iterator<? extends IBasicBlock<SSAInstruction>> itr = cfg.getPredNodes(bb); itr.hasNext();) { 
			SSACFG.BasicBlock pb = (SSACFG.BasicBlock)itr.next();
			int which = com.ibm.wala.cast.ir.cfg.Util.whichPred(cfg, bb, pb);
			if (cfg.getSuccNodeCount(pb) > 1) { 
				ret[which] = edgeGuard(pb,bb,type).and(relativeEntryGuard(pb));
			} else {
				ret[which] = relativeEntryGuard(pb);
			}
		}
		//TODO: need both normal & exceptional edge to exit for the same block.
		return ret;
	}
	
	/**
	 * Returns the basic block that contains the given instruction. 
	 * @return the block containing the given instruction 
	 **/
	final SSACFG.BasicBlock blockFor(SSAInstruction inst) { 
		return instructionToBlock.get(inst);
	}
	
	/**
	 * Returns the relative entry guard for the given basic block, if already
	 * stored in this.blockEntryGuards.  Otherwise, computes the relative guard, stores it
	 * in this.blockEntryGuards, and returns it.  The returned formula doesn't 
	 * include the guard for the call to the analyzed method.
	 * @requires bb in this.env.top.callInfo.controlDependences
	 * @return the relative entry guard for the given basic block 
	 */
	final Formula relativeEntryGuard(SSACFG.BasicBlock bb) { 
		Formula guard = blockEntryGuards.get(bb);
		
		if (guard == null) {
			final DependenceGraph<SSACFG.BasicBlock> cdg = env.top().callInfo().controlDependences();
			
			if (!cdg.containsNode(bb) || cdg.getPredNodeCount(bb)==0) { 
				guard = Formula.TRUE;
			} else {
				final List<Formula> guards = new ArrayList<Formula>(cdg.getPredNodeCount(bb));
				for(Iterator<? extends SSACFG.BasicBlock> pbs = cdg.getPredNodes(bb); pbs.hasNext(); ) {
					SSACFG.BasicBlock pb = pbs.next();
					List<Formula> labelGuards = new ArrayList<Formula>();
					for(SSACFG.BasicBlock label : cdg.edgeLabels(pb, bb)) {
						labelGuards.add(edgeGuard(pb, label, EdgeType.NOT_APPLICABLE));
					}
					assert !labelGuards.isEmpty();
					guards.add(relativeEntryGuard(pb).and(Formula.or(labelGuards)));                                        
				}
				guard = Formula.or(guards);
			}

			blockEntryGuards.put(bb, guard);
		}
		
		return guard;
	}
	
	/**
	 * Returns a formula that evaluates to true if the given 
	 * reference expression is not null.
	 * @return a formula that evaluates to true if the given 
	 * reference expression is not null.
	 */
	private final Formula nonNullGuard(Expression ref) { 
		Formula guard = nonNullGuards.get(ref);
		if (guard==null) { 
			guard = ref.eq(nil).not();
			nonNullGuards.put(ref, guard);
		} 
		return guard;
	}
	
	/**
	 * Returns a formula that evaluates to true if the given 
	 * reference expression is  null.
	 * @return a formula that evaluates to true if the given 
	 * reference expression is  null.
	 */
	private final Formula nullGuard(Expression ref) { 
		final Formula nonNullGuard = nonNullGuard(ref);
		// nonNullGuard must be either a NotFormula or a ConstantFormula
		if (nonNullGuard instanceof NotFormula) { 
			return ((NotFormula)nonNullGuard).formula();
		} else if (nonNullGuard == Formula.TRUE) { 
			return Formula.FALSE;
		} else if (nonNullGuard == Formula.FALSE) { 
			return Formula.TRUE;
		} else {
			throw new AssertionError("unreachable");
		}
	}
	/**
	 * Returns the guard for the CFG edge defined by the given blocks.
	 * @requires bb in this.env.top.cfg.nodes
	 * @requires bb in pb.getNormalSuccessors() + pb.getExceptionalSuccessors()
	 * @return the edge guard for the given CFG edge
	 */
	private final Formula edgeGuard(final SSACFG.BasicBlock pb, final SSACFG.BasicBlock bb, final EdgeType type) {
		
		assert bb.isExitBlock()? 
				!type.equals(EdgeType.NOT_APPLICABLE): 
				type.equals(EdgeType.NOT_APPLICABLE);
			
		final AbstractInstructionVisitor<Formula> guardVisitor = new AbstractInstructionVisitor<Formula>() {
			Formula guard = null;
			final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = node.getIR().getControlFlowGraph();
			
			/** @return result of applying this visitor to pb.getLastInstruction*/
			protected final Formula execute() { 
				pb.getLastInstruction().visit(this);
				assert guard != null : "Null guard for " + pb.getLastInstruction();
				return guard;
			}
			
			public final void visitEcho(AstEchoInstruction instruction) {
			  Assertions.UNREACHABLE();
			}
			
			/**
			 * @effects sets this.guard to TRUE if bb is a normal successor of pb;
			 * otherwise sets the guard to FALSE.
			 */
			private final void takeNormalSuccessor(SSAInstruction inst) { 
				assert cfg.getNormalSuccessors(pb).size() >= 1;
				
				final boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				final boolean normal = cfg.getNormalSuccessors(pb).contains(bb);

				assert normal || exceptional;
				
				if (normal && !type.equals(EdgeType.EXCEPTIONAL)) {
					guard = Formula.TRUE;
				} else {
					guard = Formula.FALSE;
				}
			}
			
			/** @effects sets this.guard to TRUE and adds a warning to this.warnings */
			public final void visitInstruction(SSAInstruction inst) { 
				final String msg = "control dependent and no model of guard";
				warnings.add(new TranslationWarning(node.getIR(), inst, msg));
				takeNormalSuccessor(inst);	
			}
			
			public final void visitMonitor(SSAMonitorInstruction inst) { 
				takeNormalSuccessor(inst);
			}
			
			/** @effects sets this.guard to the guard induced by the given switch instruction */
			public final void visitSwitch(SSASwitchInstruction inst) {
				assert type.equals(EdgeType.NOT_APPLICABLE);
				final IntExpression v = env.intUse(inst.getUse(0));
				
				// if v is null, then this switch instruction has been sliced out.
				// in that case, it means that inst is irrelevant to the assertion we are checking.
				// so, we set the guard to TRUE
				if (v==null) { 
					guard = Formula.TRUE; 
				} else {
					final List<Formula> cases = new ArrayList<Formula>();
					if (Util.isSwitchDefault(cfg, pb, bb)) {
						List<Formula> negated = new ArrayList<Formula>();
						for(IntIterator itr = inst.iterateLabels(); itr.hasNext(); ) {
							negated.add(v.eq(IntConstant.constant(itr.next())).not());
						}
						cases.add(Formula.and(negated));
					} else {
						cases.add(v.eq(IntConstant.constant(Util.getSwitchLabel(cfg, pb, bb))));
					}
					
					assert !cases.isEmpty(); // must be true if bb is a successor of pb
					guard = Formula.or(cases);
				}
			}
			
			/** @effects sets this.guard to the guard induced by the given branch instruction*/
			public final void visitConditionalBranch(SSAConditionalBranchInstruction inst) {
				assert ! type.equals(EdgeType.EXCEPTIONAL);
						
				final int left = inst.getUse(0), right = inst.getUse(1);
				
				assert node.getIR().getSymbolTable().isZero(right);
				assert env.top().callInfo().typeOf(left)==IRType.BOOLEAN;
				
				final boolean trueSucc = (bb==Util.getTakenSuccessor(cfg, pb));
				final boolean eqTest = (inst.getOperator()==ConditionalBranchInstruction.Operator.EQ);
				
				assert trueSucc || (bb==Util.getNotTakenSuccessor(cfg, pb));
				assert eqTest || (inst.getOperator()==ConditionalBranchInstruction.Operator.NE);
				
				final Formula formula = env.boolUse(left);
				// if formula is null, then this conditional branch instruction has been sliced out.
				// in that case, it means that this conditional is irrelevant to the assertion we are checking.
				// so, we arbitrarily pick the true branch
				if (formula==null) { 
					guard = trueSucc ? Formula.TRUE : Formula.FALSE;
				} else {
					guard = (trueSucc ^ eqTest) ? env.boolUse(left) : env.boolUse(left).not();
				}
			}
					
			/** @effects sets this.guard to the guard induced by the given field access instruction */
			public final void visitFieldAccess(SSAFieldAccessInstruction inst) { 
				boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				boolean normal = cfg.getNormalSuccessors(pb).contains(bb);

				assert normal || exceptional;
				assert cfg.getNormalSuccessors(pb).size()==1;
				
				if (normal && exceptional) { 
					assert bb.isExitBlock() && ! type.equals(EdgeType.NOT_APPLICABLE);
					if (type.equals(EdgeType.NORMAL)){
						normal = true;
						exceptional = false;
					} else {
						normal = false;
						exceptional = true;
					}
				}
				
				final Expression ref = env.refUse(inst.getRef());
				// if ref is null, the instruction that defines it has been sliced out.
				// hence, we ignore all flows, normal and exceptional, that leave the enclosing basic block.
				// if this is an access to a static field, then the guards are the same as if the instruction has
				// been sliced out
				if (inst.isStatic() || ref==null) { 
					guard = normal ? Formula.TRUE : Formula.FALSE;
				} else {
					guard = normal ? nonNullGuard(ref) : nullGuard(ref);
				}
			}
			
			/** @effects sets this.guard to the guard induced by the given array length instruction */
			public final void visitArrayLength(SSAArrayLengthInstruction inst) { 
				
				boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				boolean normal = cfg.getNormalSuccessors(pb).contains(bb);

				assert normal || exceptional;
				assert cfg.getNormalSuccessors(pb).size()==1;
				
				if (normal && exceptional) { 
					assert bb.isExitBlock() && ! type.equals(EdgeType.NOT_APPLICABLE);
					if (type.equals(EdgeType.NORMAL)){
						normal = true;
						exceptional = false;
					} else {
						normal = false;
						exceptional = true;
					}
				}
				
				// if ref is null, the instruction that defines it has been sliced out.
				// hence, we ignore all flows, normal and exceptional, that leave the enclosing basic block.
				final Expression ref = env.refUse(inst.getUse(0));
				if (ref==null) { 
					guard = normal ? Formula.TRUE : Formula.FALSE;
				} else {
					guard = normal ? nonNullGuard(ref) : nullGuard(ref);
				}
			}

			/** @effects sets this.guard to the guard induced by the given array access instruction */
			public final void visitArrayReferenceInstruction(SSAArrayReferenceInstruction inst) { 
				boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				boolean normal = cfg.getNormalSuccessors(pb).contains(bb);

				assert normal || exceptional;
				assert cfg.getNormalSuccessors(pb).size()==1;
				
				if (normal && exceptional) { 
					assert bb.isExitBlock() && ! type.equals(EdgeType.NOT_APPLICABLE);
					if (type.equals(EdgeType.NORMAL)){
						normal = true;
						exceptional = false;
					} else {
						normal = false;
						exceptional = true;
					}
				}
				
				final Expression ref = env.refUse(inst.getArrayRef());
				
				// if ref is null, the instruction that defines it has been sliced out.
				// hence, we ignore all flows, normal and exceptional, that leave the enclosing basic block.
				if (ref==null) {
					guard = normal ? Formula.TRUE : Formula.FALSE;
				} else {
					
					final IntExpression idx = env.intUse(inst.getIndex());
				
					final ArrayExpression<IntExpression> array = env.arrayUse(fieldSSA.getUse(inst, 0));
					final int card = array.cardinality();
					final List<Formula> idxMatches = new ArrayList<Formula>(card);
					for(int i = 0; i < card; i++) { 
						idxMatches.add( array.index(ref, i).eq(idx) );
					}
					
					final FieldExpression<IntExpression> length = env.fieldUse(fieldSSA.getUse(inst, 1));
					
					final Formula nonNullGuard = nonNullGuard(ref);
					final Formula lengthGuard = idx.lt(length.read(ref));
					final Formula accessGuard = Formula.or(idxMatches);
					final Formula normalGuard = Formula.and(nonNullGuard, lengthGuard, accessGuard);	
					
					guard = normal ? normalGuard : normalGuard.not();
				}
				
			}
		
			/** @effects sets this.guard to the guard induced by the given cast checking instruction*/
			public final void visitCheckCast(SSACheckCastInstruction inst) {
				
				boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				boolean normal = cfg.getNormalSuccessors(pb).contains(bb);
				
				assert normal || exceptional;
				assert cfg.getNormalSuccessors(pb).size()==1;
				
				if (normal && exceptional) { 
					assert bb.isExitBlock() && ! type.equals(EdgeType.NOT_APPLICABLE);
					if (type.equals(EdgeType.NORMAL)){
						normal = true;
						exceptional = false;
					} else {
						normal = false;
						exceptional = true;
					}
				}
				
				// if ref is null, the instruction that defines it has been sliced out.
				// hence, we ignore all flows, normal and exceptional, that leave the enclosing basic block.
				final Expression ref = env.refUse(inst.getUse(0));
				if (ref==null) { 
					guard = normal ? Formula.TRUE : Formula.FALSE;
				} else {
					Formula typeGuard = nullGuard(ref);
					for(TypeReference t : inst.getDeclaredResultTypes()) {
						final Expression type = env.factory().constants().valueOf(t);
						typeGuard = typeGuard.or(ref.in(type));
					}
					guard = normal ? typeGuard : typeGuard.not();
				}
			}
						
			/** @effects sets this.guard to the guard induced by the given invoke instruction*/
			public final void visitAbstractInvoke(SSAAbstractInvokeInstruction inst) { 
				boolean exceptional = cfg.getExceptionalSuccessors(pb).contains(bb);
				boolean normal = cfg.getNormalSuccessors(pb).contains(bb);
				
				assert normal || exceptional;
				
				if (normal && exceptional) { 
					assert bb.isExitBlock() && ! type.equals(EdgeType.NOT_APPLICABLE);
					if (type.equals(EdgeType.NORMAL)){
						normal = true;
						exceptional = false;
					} else {
						normal = false;
						exceptional = true;
					}
				}
				
				
				if (normal){
					guard = callExitGuards.get(inst);
					if (guard == null){ // this call must have been sliced out
						guard = Formula.TRUE;
					}
				} else if (exceptional){
					if (callExitGuards.containsKey(inst)){
						guard = callExitGuards.get(inst).not();
					} else {
						guard = Formula.FALSE;
					}
				} else {
					Assertions.UNREACHABLE();
				}

			}
		};
		
		return guardVisitor.execute();
	}
}


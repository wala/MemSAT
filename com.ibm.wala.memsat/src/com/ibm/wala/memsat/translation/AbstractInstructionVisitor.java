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

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.AstPropertyRead;
import com.ibm.wala.cast.ir.ssa.AstPropertyWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.cast.js.ssa.JSInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;

/**
 * A default implementation of the InstructionVisitor interface.  
 *  
 * @author Emina Torlak
 */
abstract class AbstractInstructionVisitor<T> implements JSInstructionVisitor, AstJavaInstructionVisitor {
	
	/**
	 * Returns the result of executing this visitor.
	 * @return result of executing this visitor.
	 */
	protected abstract T execute();
	
	/** 
	 * Default behavior of visitInstruction is to throw a 
	 * an assertion error, indicating that non-overriden methods are not reachable. */
	protected void visitInstruction(SSAInstruction instruction) {
		throw new AssertionError("unreachable: " + instruction);
	}
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	protected void visitAbstractInvoke(SSAAbstractInvokeInstruction instruction) { 
		visitInstruction(instruction); }
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	protected void visitFieldAccess(SSAFieldAccessInstruction instruction) { 
		visitInstruction(instruction); }
	
	/** Calls {@linkplain #visitAbstractInvoke(SSAAbstractInvokeInstruction)}. */
	public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
		visitAbstractInvoke(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitPropertyRead(AstPropertyRead instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitPropertyWrite(AstPropertyWrite instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
		visitInstruction(instruction); }	
	   
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitAssert(AstAssertInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitAstGlobalRead(AstGlobalRead instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitAstGlobalWrite(AstGlobalWrite instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitAstLexicalRead(AstLexicalRead instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitAstLexicalWrite(AstLexicalWrite instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitEachElementGet(EachElementGetInstruction inst) {
		visitInstruction(inst); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
		visitInstruction(inst); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitIsDefined(AstIsDefinedInstruction inst) {
		visitInstruction(inst); }	
		
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitArrayReferenceInstruction(SSAArrayReferenceInstruction instruction) { 
		visitInstruction(instruction); }
	/** Calls {@linkplain #visitArrayReferenceInstruction(SSAArrayReferenceInstruction)}. */
	public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		visitArrayReferenceInstruction(instruction); }	
	
	/** Calls {@linkplain #visitArrayReferenceInstruction(SSAArrayReferenceInstruction)}. */
	public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		visitArrayReferenceInstruction(instruction);  }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitCheckCast(SSACheckCastInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitComparison(SSAComparisonInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitConversion(SSAConversionInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitFieldAccess(SSAFieldAccessInstruction)}. */
	public void visitGet(SSAGetInstruction instruction) {
		visitFieldAccess(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitGoto(SSAGotoInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitInstanceof(SSAInstanceofInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitAbstractInvoke(SSAAbstractInvokeInstruction)}. */
	public void visitInvoke(SSAInvokeInstruction instruction) {
		visitAbstractInvoke(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitMonitor(SSAMonitorInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitNew(SSANewInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitPhi(SSAPhiInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitPi(SSAPiInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitFieldAccess(SSAFieldAccessInstruction)}. */
	public void visitPut(SSAPutInstruction instruction) {
		visitFieldAccess(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitReturn(SSAReturnInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitSwitch(SSASwitchInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitThrow(SSAThrowInstruction instruction) {
		visitInstruction(instruction); }	
	
	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		visitInstruction(instruction); }

	/** Calls {@linkplain #visitInstruction(SSAInstruction)}. */
	public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
		visitInstruction(inst); }

	/** Calls {@linkplain #visitAbstractInvoke(SSAAbstractInvokeInstruction)}. */
	public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
		visitAbstractInvoke(instruction); }

	public void visitCheckRef(JavaScriptCheckReference inst) {
		visitInstruction(inst);	}

	public void visitJavaScriptInstanceOf(JavaScriptInstanceOf inst) {
		visitInstruction(inst); }

	public void visitWithRegion(JavaScriptWithRegion inst) {
		visitInstruction(inst); }

	public void visitEcho(AstEchoInstruction inst) {
		visitInstruction(inst); }

	@Override
	public void visitSetPrototype(SetPrototype instruction) {
		visitInstruction(instruction); }

	@Override
	public void visitPrototypeLookup(PrototypeLookup instruction) {
		visitInstruction(instruction); }
	
}

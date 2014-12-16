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
package com.ibm.wala.memsat.concurrent.memory.jmm;

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.SPECIAL;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_READ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Variable;

import com.ibm.wala.memsat.concurrent.Program;
import com.ibm.wala.types.MethodReference;

/**
 * Implements the alternative definition of the JMM, given by
 * Sevcik and Aspinall.
 * 
 * @see [1] D.�Aspinall and J.�Sevcik. Formalising Java�s data race free guarantee. In 
 * Theorem Proving in Higher-Order Logics (TPHOLs �07), pages 22�37, 2007.
 * @see [2] D.�Aspinall and J.�Sevc�k. Java memory model examples: good, bad and ugly. 
 * In VAMP �07, Lisbon, Portugal, September 2007.
 * @see [3] J.�Sevc�k and D.�Aspinall. On validity of program transformations in the Java memory model. 
 * In ECOOP �08, volume 5142 of LNCS, pages 27�51. Springer Berlin, 2008.
 * @author etorlak
 */
public final class JMMAlt extends JavaMemoryModel {

	/**
	 * Constructs a new instance of JMMAlt that will use the given number of speculations
	 * and that will treat calls to specified methods as external actions.
	 */
	public JMMAlt(int maxSpeculations, Set<MethodReference> memoryInstructions) {
		super(maxSpeculations, memoryInstructions);
	}

	/**
	 * Returns the JMMAlt rule for the hb relations: for all reads r in C_i,
	 * W(r) <=_{hb} r iff W(r) <=_{hb_i} r, and not r <=_{hb_i} W(r)
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule2(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule2(Program prog, JMMExecution main, List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression reads = prog.allOf(NORMAL_READ,VOLATILE_READ);
		final Expression hb = main.hb();
		for(int i = 1; i < maxSpeculations; i++) {
			final Variable r = Variable.unary("r"+i);
			final Expression w = main.w(r);
			final Expression C_i = commits.get(i);
			final Expression hb_i = speculations.get(i).hb();
			final Expression W2r = w.product(r);
			final Formula f1 = W2r.in(hb).iff(W2r.in(hb_i));
			final Formula f2 = r.product(w).in(hb_i).not();
			ret.add( f1.and(f2).forAll(r.oneOf(C_i.intersection(reads))) );
		}
		return Formula.and(ret);
	}

	/**
	 * JMMAlt omits the third rule.  This method simply returns TRUE.
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule3(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule3(Program prog, JMMExecution main, List<JMMExecution> speculations, List<? extends Expression> commits) {
		return Formula.TRUE;
	}
	
	/**
	 * Returns the JMMAlt rule for the visibility of committed writes: 
	 * for all reads r in C_i - C_{i-1}, W(r) in C_{i-1}
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule7(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule7(Program prog, JMMExecution main, List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression reads = prog.allOf(NORMAL_READ,VOLATILE_READ);
		for(int i = 1; i < maxSpeculations; i++) { 
			final Expression C_i = commits.get(i), C_j = commits.get(i-1);
			final Variable r = Variable.unary("r" + i);
			ret.add( main.w(r).in(C_j).forAll(r.oneOf(C_i.difference(C_j).intersection(reads))) );
		}
		return Formula.and(ret);
	}

	/**
	 * JMMAlt omits the eight rule [1,3].  This method simply returns TRUE.
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule8(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule8(Program prog, JMMExecution main, List<JMMExecution> speculations, List<? extends Expression> commits) {
		return Formula.TRUE;
	}

	
	/**
	 * Returns the JMMAlt rule for external actions [3]: if y in C_i is an external action
	 * and x <=_{hb} y, then x in C_i
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule9(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule9(Program prog, JMMExecution main, List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression externals = prog.allOf(SPECIAL);
		final Expression hb = main.hb();
		for(int i = 1; i < maxSpeculations; i++) { 
			final Expression C_i = commits.get(i);
			final Variable y = Variable.unary("y"+i);
			ret.add( hb.join(y).in(C_i).forAll(y.oneOf(C_i.intersection(externals))) );
		}
		return Formula.and(ret);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "JMMAlt";
	}
}

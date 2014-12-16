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

import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.END;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.LOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.NORMAL_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.SPECIAL;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.START;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.UNLOCK;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_READ;
import static com.ibm.wala.memsat.frontEnd.InlinedInstruction.Action.VOLATILE_WRITE;

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
 * Implements the original definition of the JMM, given by
 * Manson, Pugh and Adve.
 * 
 * @see [1]	J.�Manson, W.�Pugh, and S.�V. Adve. The java memory model. In POPL �05: Proc. of 
 * the 32nd ACM SIGPLAN-SIGACT symposium on Principles of programming languages, pages 378�391, New York, NY, USA, 2005. ACM.
 * @author etorlak
 */
public final class JMMOriginal extends JavaMemoryModel {

	/**
	 * @param maxSpeculations
	 * @param memoryInstructions
	 */
	public JMMOriginal(int maxSpeculations,
			Set<MethodReference> memoryInstructions) {
		super(maxSpeculations, memoryInstructions);
	}

	/**
	 * @return relation & (set -> set)
	 */
	private final Expression restrict(Expression relation, Expression set) { 
		return relation.intersection(set.product(set));
	}
	
	/**
	 * Returns the JMMOriginal rule for hb relations: hb_i |_{C_i} = hb |_{C_i}
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule2(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule2(Program prog, JMMExecution main,
			List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression hb = main.hb();
		for(int i = 1; i < maxSpeculations; i++) {
			final Expression C_i = commits.get(i);
			final Expression hb_i = speculations.get(i).hb();
			ret.add( restrict(hb_i, C_i).eq(restrict(hb, C_i)) );
		}
		return Formula.and(ret);
	}

	/**
	 * Returns the JMMOriginal rule for so relations: so_i |_{C_i} = so |_{C_i}
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule3(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule3(Program prog, JMMExecution main,
			List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression so = main.so();
		for(int i = 1; i < maxSpeculations; i++) {
			final Expression C_i = commits.get(i);
			final Expression so_i = speculations.get(i).so();
			ret.add( restrict(so_i, C_i).eq(restrict(so, C_i)) );
		}
		return Formula.and(ret);
	}

	/**
	 * Returns the JMMOriginal rule for the visibility of committed writes: 
	 * for all reads r in C_i - C_{i-1}, W_i(r) in C_{i-1} W(r) in C_{i-1}
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule7(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule7(Program prog, JMMExecution main,
			List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression reads = prog.allOf(NORMAL_READ,VOLATILE_READ);
		final Expression w = main.w();
		for(int i = 1; i < maxSpeculations; i++) { 
			final Expression w_i = speculations.get(i).w();
			final Expression C_i = commits.get(i), C_j = commits.get(i-1);
			final Variable r = Variable.unary("r" + i);
			ret.add( (r.join(w_i).in(C_j)).and(r.join(w).in(C_j)).forAll(r.oneOf(C_i.difference(C_j).intersection(reads))) );
		}
		return Formula.and(ret);
	}

	/**
	 * Returns the ssw relation for the given execution.  These are the edges that are in
	 * the transitive reduction of e.hb but not in e.po.
	 * @return  ssw relation for the given execution
	 */
	private final Expression ssw(JMMExecution e) { 
		final Variable a = Variable.unary("a"), b = Variable.unary("b");
		final Expression swDpo = e.sw().difference(e.po());
		final Expression hb = e.hb();
		final Formula sswEdge = hb.difference(a.product(b)).closure().eq(hb.closure()).not();
		return sswEdge.comprehension(a.oneOf(e.actions()).and(b.oneOf(a.join(swDpo))));
	}
	
	/**
	 * Returns the JMMOriginal rule for sufficient synchronizes-with edges.
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule8(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule8(Program prog, JMMExecution main,
			List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression syncs = prog.allOf(START,END,VOLATILE_READ,VOLATILE_WRITE,LOCK,UNLOCK);
		for(int i = 1; i < maxSpeculations; i++) { 
			final JMMExecution E_i = speculations.get(i);
			final Expression ssw_i = ssw(E_i);
			final Expression hb_i = E_i.hb();
			
			final Expression C_i = commits.get(i), C_j = commits.get(i-1);
			
			final Variable x = Variable.unary("x"), y = Variable.unary("y"), z = Variable.unary("z");
			final Expression xExpr = E_i.actions().intersection(syncs);
			final Expression yExpr = x.join(ssw_i);
			final Expression zExpr = y.join(hb_i).intersection(C_i.difference(C_j));
			
			final Collection<Formula> body = new ArrayList<Formula>(maxSpeculations-i);
			for(int j = i; j < maxSpeculations; j++) { 
				body.add(x.product(y).in(speculations.get(j).sw()));
			}

			ret.add( Formula.and(body).forAll(x.oneOf(xExpr).and(y.oneOf(yExpr)).and(z.oneOf(zExpr))) );
		}
		
		return Formula.and(ret);
	}

	/**
	 * Returns the JMMOriginal rule for external actions: if y in C_i, x is an external action
	 * and x <=_{hb_i} y, then x in C_i
	 * @see com.ibm.wala.memsat.concurrent.memory.jmm.JavaMemoryModel#rule9(com.ibm.wala.memsat.concurrent.Program, com.ibm.wala.memsat.concurrent.memory.jmm.JMMExecution, java.util.List, java.util.List)
	 */
	@Override
	protected Formula rule9(Program prog, JMMExecution main,
			List<JMMExecution> speculations, List<? extends Expression> commits) {
		final Collection<Formula> ret = new ArrayList<Formula>(maxSpeculations);
		final Expression externals = prog.allOf(SPECIAL);
		for(int i = 1; i < maxSpeculations; i++) { 
			final Expression hb_i = speculations.get(i).hb();
			final Expression C_i = commits.get(i);
			final Variable y = Variable.unary("y"+i);
			ret.add( hb_i.join(y).intersection(externals).in(C_i).forAll(y.oneOf(C_i)) );
		}
		return Formula.and(ret);
	}

}

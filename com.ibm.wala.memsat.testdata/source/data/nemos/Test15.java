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
package data.nemos;

/**
 * Litmus test case from 
 * Y.?Yang, G.?Gopalakrishnan, G.?Lindstrom, and K.?Slind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS ?04, pages 26?30, 2004.
 * 
 * <pre>
 *	p1			p2
 *	w(a)1		w(b)1
 *  w(c)1		w(c)2
 *	r(c)1		r(c)2
 *	r(b)0		r(a)0
 *
 * Decision: not SC, not Causal, is PRAM, is Coherent, not PC
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test15 {

	static int a = 0, b = 0, c = 0;
	
	public static final void p1() {
		a = 1;
		c = 1;
		final int r1 = c;
		final int r2 = b;
		
		assert r1 == 1;
		assert r2 == 0;
	}
	
	public static final void p2() {
		b = 1;
		c = 2;
		final int r3 = c;
		final int r4 = a;
		
		assert r3 == 2;
		assert r4 == 0;
	}


}

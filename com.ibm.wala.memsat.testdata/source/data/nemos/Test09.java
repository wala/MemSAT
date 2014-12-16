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
 *	p1			p2			p3			p4
 *	w(x)0		r(y)1		w(z)0		r(v)1
 *	w(x)1		r(z)0		w(z)1		r(x)0
 *	w(y)1					w(v)1
 *
 * Decision: not SC, is PRAM, is Coherence, is Causal, is PC.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test09 {
	static int x = 0, y = 0, z = 0, v = 0;
	
	public static final void p1() {
		x = 0;
		x = 1;
		y = 1;
	}
	
	public static final void p2() {
		final int r1 = y;
		final int r2 = z;
		assert r1 == 1;
		assert r2 == 0;
	}

	public static final void p3() {
		z = 0;
		z = 1;
		v = 1;
	}

	public static final void p4() {
		final int r3 = v;
		final int r4 = x;
		assert r3 == 1;
		assert r4 == 0;
	}


}

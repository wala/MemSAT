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
 *	w(x)1		r(y)1
 *	w(y)1		r(x)0
 *
 * Decision: not SC, not Causal, not PRAM, is Coherent, not PC
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test10 {
	static int x = 0, y = 0;
	
	public static final void p1() {
		x = 1;
		y = 1;
	}
	
	public static final void p2() {
		final int r1 = y;
		final int r2 = x;
		assert r1 == 1;
		assert r2 == 0;
	}

}

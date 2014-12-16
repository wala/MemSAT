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
package data.nemos;

/**
 * Litmus test case from 
 * Y.ÊYang, G.ÊGopalakrishnan, G.ÊLindstrom, and K.ÊSlind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS Õ04, pages 26Ð30, 2004.
 * 
 * <pre>
 *	p1			p2
 *	w(x)0		w(y)0
 *	w(x)1		w(y)1
 *	r(y)0		r(x)0
 *
 * Decision: not SC, is Causal, is PRAM, is Coherent, is PC
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test04 {
	static int x = 0, y = 0;
	
	public static final void p1() {
		x = 0;
		x = 1;
		final int r1 = y;
		assert r1 == 0;
	}
	
	public static final void p2() {
		y = 0;
		y = 1;
		final int r2 = x;
		assert r2 == 0;
	}

}

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
 *	r(b)1		r(a)1
 *	w(a)1		w(b)1
 *
 * Decision: not SC, not Causal, is PRAM, is Coherent, is PC
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test02 {
	static int a = 0, b = 0;
	
	public static final void p1() {
		final int r1 = b;
		a = 1;
		assert r1 == 1;
	}
	
	public static final void p2() {
		final int r2 = a;
		b = 1;
		assert r2 == 1;
	}
}

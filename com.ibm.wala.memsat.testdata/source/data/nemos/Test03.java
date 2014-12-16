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
 *     P1       P2       P3       P4       P5      P6       P7       P8
 *   W(B,0)   W(B,1)                                      W(C,1)    W(C,0)
 *                     R(B,0)   R(B,1)   R(C,1)   R(C,0)
 *                     W(A,0)   R(A,0)   R(A,1)   W(A,1)
 *                     R(C,0)   R(C,1)   R(B,1)   R(B,0)
 *                     
 * Decision: ?
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test03 {
	static int a = 0, b = 0, c = 0;
	
	public static final void p1() {
		b = 0;
	}
	
	public static final void p2() {
		b = 1;
	}
	
	public static final void p3() {
		final int r1 = b;
		a = 0;
		final int r2 = c;
		
		assert r1 == 0;
		assert r2 == 0;
		
	}
	
	public static final void p4() {
		final int r3 = b;
		final int r4 = a;
		final int r5 = c;
		
		assert r3 == 1;
		assert r4 == 0;
		assert r5 == 1;
	}

	public static final void p5() {
		final int r6 = c;
		final int r7 = a;
		final int r8 = b;
		
		assert r6 == 1;
		assert r7 == 1;
		assert r8 == 1;
	}

	public static final void p6() {
		final int r9 = c;
		a = 1;
		final int r10 = b;
		
		assert r9 == 0;
		assert r10 == 0;
	}

	public static final void p7() {
		c = 1;
	}
	
	public static final void p8() {
		c = 0;
	}
}

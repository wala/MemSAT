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
 *	w(x)0		r(y)2
 *	w(x)1		r(x)0
 *	w(y)2

 *
 * Decision: not SC, is Causal, not PRAM, is Coherent, is PC
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test05 {
	static int x = 0, y = 0;
	
	public static final void p1() {
		x = 0;
		x = 1;
		y = 2;
	}
	
	public static final void p2() {
		final int r1 = y;
		final int r2 = x;
		
		assert r1 == 2;
		assert r2 == 0;
	}


}

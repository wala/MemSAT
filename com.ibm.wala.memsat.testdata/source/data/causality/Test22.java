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
package data.causality;

/**
 * Litmus test case from 
 * Y.ÊYang, G.ÊGopalakrishnan, G.ÊLindstrom, and K.ÊSlind. 
 * Nemos: a framework for axiomatic and executable specifications of memory consistency models. 
 * In IPDPS Õ04, pages 26Ð30, 2004.
 * 
 * <pre>
 * Initially, a = b = c = 0
 * 
 * Thread 1:
 * a = 1
 * c = 0
 * r1 = b
 * 
 * Thread 2:
 * b = 1
 * c = 2
 * r2 = a
 * 
 * Behavior in question: r1 == r2 == 0
 * 
 * Decision: Allowed
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test22 {
	static int a = 0, b = 0, c = 0;
	
	public static final void thread1() {
		a = 1;
		c = 0;
		final int r1 = b;
		
		assert r1==0;
	}
	
	public static final void thread2() {
		b = 1;
		c = 2;
		final int r2 = a;
		
		assert r2==0;
	}
}

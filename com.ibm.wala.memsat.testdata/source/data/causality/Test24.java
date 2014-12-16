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
 * M. Ahamad, R. Bazzi, R. John, P. Kohli and G. Neiger.
 * The power of Processor Consistency.
 * Proceedings of the fifth annual ACM symposium on Parallel algorithms and architectures.
 * 1993.
 * 
 * 
 * <pre>
 * Initially, x = y = v = z = 0
 * 
 * Thread 1:
 * x = 0
 * x = 1
 * y = 1
 * 
 * Thread 2:
 * r1 = y
 * r2 = z
 * 
 * Thread 3:
 * z = 0
 * z = 1
 * v = 1
 * 
 * Thread 4:
 * r3 = v
 * r4 = x
 * 
 * 
 * Behavior in question: r1 == 1, r2 == 0, r3 == 1, r4 = 0
 * 
 * Decision: Allowed
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test24 {
	static int x = 0, y = 0, z = 0, v = 0;
	
	public static final void thread1() {
		x = 0;
		x = 1;
		y = 1;
	}
	
	public static final void thread2() {
		final int r1 = y;
		final int r2 = z;
		
		assert r1 == 1;
		assert r2 == 0;
	}
	
	public static final void thread3() {
		z = 0;
		z = 1;
		v = 1;
	}
	
	public static final void thread4() {
		final int r3 = v;
		final int r4 = x;
		
		assert r3 == 1;
		assert r4 == 0;
	}
	
//	static int x = 0, y = 0;
//	
//	public static final void thread1() {
//		x = 1;
//	}
//	
//	public static final void thread2() {
//		final int r1 = x;
//		final int r2 = x;
//		
//		assert r1 == 1;
//		assert r2 == 2;
//	}
//
//	public static final void thread3() {
//		final int r1 = x;
//		final int r2 = x;
//		
//		assert r1 == 2;
//		assert r2 == 1;
//	}
//
//	public static final void thread4() {
//		x = 2;
//	}
}

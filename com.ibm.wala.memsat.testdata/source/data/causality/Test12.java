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
 * Causality test case 12 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = 0; a[0] = 1, a[1] = 2
 * 
 * Thread 1
 * r1 = x
 * a[r1] = 0
 * r2 = a[0]
 * y = r2
 * 
 * Thread 2
 * r3 = y
 * x = r3
 * 
 * Behavior in question: r1 = r2 = r3 = 1
 * 
 * Decision: Disallowed. Since no other thread accesses the array a,
 *      the code for thread 1 should be equivalent to:
 * 
 * 	r1 = x
 * 	a[r1] = 0
 * 	if (r1 == 0)
 *     r2 = 0
 *  else
 *     r2 = 1
 * 	y = r2
 * 
 * 
 * With this code, it is clear that this is the same situation as
 * test 4.
 * </pre>
 * 
 * @author etorlak
 */
public final class Test12 {
	static int x = 0, y = 0;
	final static int[] a = {1, 2};
	
	public static final void thread1() {
		final int r1 = x;
		a[r1] = 0;
		final int r2 = a[0];
		y = r2;
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread2() {
		final int r3 = y;
		x = r3;
		
		assert r3==1;
	}
}

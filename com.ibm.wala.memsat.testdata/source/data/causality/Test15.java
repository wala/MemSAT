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
 * Causality test case 15 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, a = b = x = y = 0, x and y are volatile
 * 
 * Thread 1:
 * r0 = x
 * if (r0 == 1)
 *   r1 = a
 * else 
 *   r1 = 0
 * if (r1 == 0)
 *   y = 1
 * else
 *   b = 1
 * 
 * Thread 2:
 * do {
 *   r2 = y
 *   r3 = b
 *   } while (r2 + r3 == 0);
 * a = 1;
 * 
 * Thread 3:
 * x = 1
 * 
 * Behavior in question: r0 == r1 == r3 = 1; r2 == 0
 * 
 * Decision: Disallowed. In all sequentially consistent executions, r1 = 0 and
 * 	the program is correctly synchronized. Since the program is correctly
 * 	synchronized in all SC executions, no non-sc behaviors are allowed.
 * </pre>
 * 
 * 
 * @author etorlak
 * 
 */
public final class Test15 {
	static int a = 0, b = 0;
	static volatile int x = 0, y = 0;
	
	public static final void thread1() {
		final int r0 = x;
		final int r1;
		if (r0 == 1) 
			r1 = a;
		else 
			r1 = 0;
		if (r1 == 0)
			y = 1;
		else 
			b = 1;
		
		assert r0==1;
		assert r1==1;
	}
	
	public static final void thread2() {
		int r2, r3;
		
		do {
			r2 = y;
			r3 = b;
		} while (r2 + r3 == 0);
		
		a = 1;
		
		assert r2==0;
		assert r3==1;
	}
	
	public static final void thread3() {
		x = 1;
	}
}

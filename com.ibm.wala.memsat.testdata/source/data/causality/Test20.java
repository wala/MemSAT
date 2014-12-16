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
 * Causality test case 20 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially,  x = y = 0
 * 
 * Thread 1:
 * join thread 3
 * r1 = x
 * y = r1
 * 
 * Thread 2:
 * r2 = y
 * x = r2
 * 
 * Thread 3:
 * r3 = x
 * if (r3 != 42)
 *   x = 42
 * 
 * Behavior in question: r1 == r2 == r3 == 42
 * 
 * Decision: Allowed. This is the same as test case 17, except that thread 1 has been
 *   split into two threads.
 * </pre>
 * 
 * @threadOrder: thread3->thread1
 * @author etorlak
 */
public final class Test20 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r1 = x;
		y = r1;
		
		assert r1==42;
	}
	
	public static final void thread2() {
		final int r2 = y;
		x = r2;
		
		assert r2==42;
	}
	
	public static final void thread3() {
		final int r3 = x;
		if (r3 != 42)
			x = 42;
		
		assert r3==42;
	}
}

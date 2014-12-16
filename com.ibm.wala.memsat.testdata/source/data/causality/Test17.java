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
 * Causality test case 17 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially,  x = y = 0
 * 
 * Thread 1:
 * r3 = x
 * if (r3 != 42)
 *   x = 42
 * r1 = x
 * y = r1
 * 
 * Thread 2:
 * r2 = y
 * x = r2
 * 
 * Behavior in question: r1 == r2 == r3 == 42
 * 
 * Decision: Allowed. A compiler could determine that at r1 = x in thread 1,
 *   is must be legal for to read x and see the value 42. Changing r1 = x 
 *   to r1 = 42 would allow y = r1 to be transformed to y = 42 and performed
 *   earlier, resulting in the behavior in question.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test17 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r3 = x;
		if (r3 != 42)
			x = 42;
		final int r1 = x;
		y = r1;
		
		assert r1==42;
		assert r3==42;
	}
	
	public static final void thread2() {
		final int r2 = y;
		x = r2;
		
		assert r2==42;
	}
}

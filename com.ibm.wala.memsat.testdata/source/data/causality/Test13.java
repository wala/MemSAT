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
 * Causality test case 13 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * r1 = x
 * if (r1 == 1)
 *   y = 1
 * 
 * Thread 2:
 * r2 = y
 * if (r2 == 1)
 *   x = 1
 * 
 * Behavior in question: r1 == r2 == 1
 * 
 * Decision: Disallowed. In all sequentially consistent executions, no writes
 * 	to x or y occur and the program is correctly synchronized. The
 * 	only SC behavior is r1 == r2 == 0.
 * </pre>
 * 
 * 
 * @author etorlak
 */
public final class Test13 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r1 = x;
		if (r1 == 1) 
			y = 1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = y;
		if (r2 == 1)
			x = 1;
		
		assert r2==1;
	}
}

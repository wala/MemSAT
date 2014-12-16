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
 * Causality test case 3 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 *<pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * r1 = x
 * r2 = x
 * if r1 == r2
 *   y = 1
 * 
 * Thread 2:
 * r3 = y
 * x = r3
 * 
 * Thread 3:
 * x = 2
 * 
 * Behavior in question: r1 == r2 == r3 == 1
 * 
 * Decision: Allowed, since redundant read elimination could result in simplification
 * 	of r1 == r2 to true, allowing y = 1 to be moved early.
 * 
 * Notes: Same as test case 2, except there are SC executions in which r1 != r2
 * </pre>
 * 
 * @author etorlak
 */
public final class Test03 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r1 = x;
		final int r2 = x;
		if (r1==r2)
			y = 1;
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread2() {
		final int r3 = y;
		x = r3;
		
		assert r3==1;
	}
	
	public static final void thread3() { 
		x = 2;
	}
}

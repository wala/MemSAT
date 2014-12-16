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
 * Causality test case 5 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = z = 0
 * 
 * Thread 1:
 * r1 = x
 * y = r1
 * 
 * Thread 2
 * r2 = y
 * x = r2
 * 
 * Thread 3
 * z = 1
 * 
 * Thread 4
 * r3 = z
 * x = r3
 * 
 * Behavior in question: r1 == r2 == 1, r3 == 0.
 * 
 * Decision: Forbidden: values are not allowed to come out of thin air,
 * 	even if there are other executions in which the thin-air value
 * 	would have been written to that variable by some not out-of-thin
 * 	air means.
 * </pre>
 * 
 * @author etorlak 
 */
public final class Test05 {
	static int x = 0, y = 0, z = 0;
	
	public static final void thread1() {
		final int r1 = x;
		y = r1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = y;
		x = r2;
		
		assert r2==1;
	}
	
	public static final void thread3() { 
		z = 1;
	}
	
	public static final void thread4() {
		final int r3 = z;
		x = r3;
		
		assert r3==0;
	}
}

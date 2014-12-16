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
 * Causality test case 11 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = z = 0
 * 
 * Thread 1:
 * r1 = z
 * w = r1
 * r2 = x
 * y = r2
 * 
 * Thread 2:
 * r4 = w
 * r3 = y
 * z = r3
 * x = 1
 * 
 * Behavior in question: r1 = r2 = r3 = r4 = 1
 * 
 * Decision: Allowed. Reordering of independent statements can transform
 *   the code to:
 * 
 * 	Thread 1:
 * 	r2 = x
 * 	y = r2
 * 	r1 = z
 * 	w = r1
 * 
 * 	Thread 2:
 * 	x = 1
 * 	r3 = y
 * 	z = r3
 * 	r4 = w
 * 
 *   after which the behavior in question is SC.
 * 
 * Note: This is similar to test case 7, but extended with one more
 * 	rung in the ladder
 * </pre>
 * 
 * 
 * @author etorlak
 * 
 */
public class Test11 {
	static int x = 0, y = 0, z = 0, w = 0;
	
	public static final void thread1() {
		final int r1 = z;
		w = r1;
		final int r2 = x;
		y = r2;
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread2() {
		final int r4 = w;
		final int r3 = y;
		z = r3;
		x = 1;
		
		assert r3==1;
		assert r4==1;
	}
}

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
 * Causality test case 6 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * 
 * <pre>
 * Initially A = B = 0
 * 
 * Thread 1
 * r1 = A
 * if (r1 == 1)
 *    B = 1
 * 
 * Thread 2
 * r2=B
 * if (r2 == 1)
 *     A = 1
 * if (r2 == 0)
 *     A = 1
 * 
 * Behavior in question: r1 == r2 == 1 allowed?
 * 
 * Decision: Allowed. Intrathread analysis could determine that thread 2 always writes 
 * 	1 to A and hoist the write to the beginning of thread 2.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test06 {
	static int A = 0, B = 0;
	
	public static final void thread1() {
		final int r1 = A;
		if (r1==1)
			B = 1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = B;
		if (r2==1)
			A = 1;
		if (r2==0)
			A = 1;
		
		assert r2==1;
	}
}

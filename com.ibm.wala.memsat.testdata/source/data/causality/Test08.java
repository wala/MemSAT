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
 * Causality test case 8 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * r1 = x
 * r2 = 1 + r1*r1 - r1
 * y = r2
 * 
 * Thread 2:
 * r3 = y
 * x = r3
 * 
 * Behavior in question: r1 = r2 = 1
 * 
 * Decision: Allowed. Interthread analysis could determine that x and y are always
 * 	either 0 or 1, and thus determine that r2 is always 1. Once this determination
 * 	is made, the write of 1 to y could be moved early in thread 1.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test08 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r1 = x;
		final int r2 = 1 + r1*r1 - r1;
		y = r2;
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread2() {
		final int r3 = y;
		x = r3;
	}
}

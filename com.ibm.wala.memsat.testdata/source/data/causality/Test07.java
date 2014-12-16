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
 * Causality test case 7 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = z = 0
 * 
 * Thread 1:
 * r1 = z
 * r2 = x
 * y = r2
 * 
 * Thread 2:
 * r3 = y
 * z = r3
 * x = 1
 * 
 * Behavior in question: r1 = r2 = r3 = 1.
 * 
 * Decision: Allowed. Intrathread transformations could move r1 = z to 
 * 	after the last statement in thread 1, and x = 1 to before the 
 * 	first statement in thread 2.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test07 {
	static int x = 0, y = 0, z = 0;
	
	public static final void thread1() {
		final int r1 = z;
		final int r2 = x;
		y = r2;
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread2() {
		final int r3 = y;
		z = r3;
		x = 1;
		
		assert r3==1;
	}
}

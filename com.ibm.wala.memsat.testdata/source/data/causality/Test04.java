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
 * Causality test case 4 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * r1 = x
 * y = r1
 * 
 * Thread 2:
 * r2 = y
 * x = r2
 * 
 * Behavior in question: r1 == r2 == 1
 * 
 * Decision: Forbidden: values are not allowed to come out of thin air
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test04 {
	static int x = 0, y = 0;
	
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
}

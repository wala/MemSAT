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
 * Causality test case 16 from
 * http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html
 * 
 * <pre>
 * Initially, x = 0
 * 
 * Thread 1:
 * r1 = x
 * x = 1
 * 
 * Thread 2:
 * r2 = x 
 * x = 2
 * 
 * Behavior in question: r1 == 2; r2 == 1
 * 
 * Decision: Allowed.
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test16 {
	static int x = 0;
	
	public static final void thread1() {
		final int r1 = x;
		x = 1;
		
		assert r1==2;
	}
	
	public static final void thread2() {
		final int r2 = x;
		x = 2;
		
		assert r2==1;
	}
}

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
 * Litmus test case from 
 * P. Kohli, G. Neiger and M. Ahamad
 * A characterization of sclaable shared memories. 
 * GUT-CC-93/04, 1993.
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * x = 1
 * 
 * Thread 2:
 * r1 = x
 * y = 1
 * 
 * Thread 3:
 * r2 = y
 * r3 = x
 * 
 * 
 * Behavior in question: r1 == 1, r2 == 1, r3 == 0
 * 
 * Decision: Allowed
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test23 {
	static int x = 0, y = 0;
	
	public static final void thread1() {
		x = 1;
	}
	
	public static final void thread2() {
		final int r1 = x;
		y = 1;
		assert r1 == 1;
	}
	
	public static final void thread3() {
		final int r2 = y;
		final int r3 = x;
		
		assert r2 == 1;
		assert r3 == 0;
	}
}

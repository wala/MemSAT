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
 * A causality test case from a reviewer.
 * 
 * <pre>
 * Initially, x = y = z = 0
 * 
 * Thread 1
 * r1 = x
 * if (r1 == 0)
 * 	 r2 = x
 * 	 r3 = z
 * 	 y = r3
 * else 
 *   r2 = 0
 *   r3 = z
 *   y = 1
 * 
 * Thread 2
 * x = 1
 * r4 = y
 * z = r4
 * 
 * Behavior in question: r1 == 0,  r2 == r3 == r4 == 1
 * 
 * Decision: Allowed.
 * </pre>
 * 
 * @author etorlak
 */
public class Test26 {
	static int x = 0, y = 0, z = 0;
	
	public static final void thread1() {
		final int r1, r2, r3;
		
		r1 = x;
		
		if (r1==0) {
			r2 = x;
			r3 = z;
			y = r3;	
		} else { 	
			r2 = 0;
			r3 = z;
			y = 1;
		}
		
		assert r1==0 && r2==1 && r3==1;
	}
	
	public static final void thread2() {
		x = 1;
		final int r4 = y;
		z = r4;
		
		assert r4==1;
	}
	
}

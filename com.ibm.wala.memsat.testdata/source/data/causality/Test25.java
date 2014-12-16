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
 * A basic causality test case 
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1
 * r1 = x
 * if (r1 != 0)
 * 	 y = r1
 * else 
 *   y = 1
 * 
 * Thread 2
 * r2 = y
 * x = r2
 * 
 * Behavior in question: r1 == r2 == 1
 * 
 * Decision: Allowed.
 * </pre>
 * 
 * @author etorlak
 */
public class Test25 {
	static int x = 0, y = 0;
	//final static Object m1 = new Object(), m2 = new Object();
	
	public static final void thread1() {
		final int r1 = x;
		
		if (r1!=0)
			y = r1;	
		else 	
			y = 1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = y;
//		final int r3;
//		if (r2!=0)
//			r3 = r2;
//		else 
//			r3 = 1;
//		x = r3;
		x = 1;
		assert r2==1;
	}
}

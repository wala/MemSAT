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
 * "Good" test case from 
 * D.ÊAspinall and J.ÊSevc“k. Java memory model examples: good, bad and ugly. In VAMP Õ07, Lisbon, Portugal, September 2007.
 * 
 * <pre>
 * Initially, x = y = 0
 * 
 * Thread 1:
 * r1 = x
 * if (r1 != 0)
 * 	y = r1
 * else
 * 	y = 1
 * 
 * Thread 2:
 * r2 = y
 * x = r2
 * 
 * Behavior in question: r1 == r2 == 1
 * 
 * Decision: Allowed: The compiler may realise that the only values 
 * in the program are 0 and 1; that the first thread can never write 0 to y; thus it 
 * must write 1. As a result, it could replace the if statement with an assignment 
 * y:=1. Then it could reorder this with the statement r1:=x, giving the program: 
 * 
 * y := 1  |  r2 := y 
 * r1 := x |  x := r2
 * 
 * After the reordering we can get r1 = r2 = 1 from a SC execution. 
 * </pre>
 * 
 * @author etorlak
 * 
 */
public final class Test21 {
static int x = 0, y = 0;
	
	public static final void thread1() {
		final int r1 = x;
		if (r1 != 0)
			y = r1;
		else
			y = 1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = y;
		x = r2;
		
		assert r2==1;
	}
}

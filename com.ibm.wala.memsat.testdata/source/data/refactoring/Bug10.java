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
package data.refactoring;

/**
 * Demonstrates that the Concurrencer refactoring rule for
 * eliminating synchronization on fiels is buggy.
 * 
 * @see Bug10R
 * @author etorlak
 */
public final class Bug10 {
	//static int value = 0;
	static int x = 0;
	static int y = 0;
	final static Object m = new Object();
	
	public static final void thread1() {
		final int r1 = x;
		synchronized(m) {}
		y = 1;
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = y;
		synchronized(m) {
		//	final int r3 = value;
		//	value = r3 + 1;
		}
		x = 1;
		
		assert r2==1;
	}
	
}

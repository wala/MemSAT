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
 * Demonstrates that changing method signature can introduce a bug in a concurrent setting.
 * @see Bug11
 * @author etorlak
 */
public class Bug11R {
	volatile static int x = 0;
	volatile static boolean y = false;
	
	public static final void m(boolean b, int i) {} 
	
	public static final void thread1() {
		final int r1;
		m(y = true, r1 = x);
		assert r1==1;
	}
	
	public static final void thread2() {
		final boolean r2 = y;
		x = 1;
		assert r2;
	}
}

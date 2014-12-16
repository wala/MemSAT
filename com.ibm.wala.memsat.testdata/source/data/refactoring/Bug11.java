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
 * @see Bug11R
 * @author etorlak
 */
public final class Bug11 {
	volatile static int x = 0;
	volatile static boolean y = false;
	
	public static final void m(int i, boolean b) {} 
	
	public static final void thread1() {
		final int r1;
		m(r1 = x, y = true);
		assert r1==1;
	}
	
	public static final void thread2() {
		final boolean r2 = y;
		x = 1;
		assert r2;
	}
}

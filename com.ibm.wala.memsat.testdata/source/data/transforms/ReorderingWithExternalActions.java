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
package data.transforms;

/**
 * Reordering with external actions: 
 * J.ÊSevc“k and D.ÊAspinall. On validity of program transformations in 
 * the Java memory model. In ECOOP Õ08, volume 5142 of LNCS, pages 27Ð51. Springer Berlin, 2008.
 * 
 * @author etorlak
 */
public final class ReorderingWithExternalActions {
	static int x = 0, y = 0;
	
	public static void print(String s) {}
	
	public static final void thread1() {
		final int r1 = y;
		if (r1 == 1)
			x = 1;
		else {
			print("!");
			x = 1;
		}
		
		assert r1==1;
	}
	
	public static final void thread2() {
		final int r2 = x;
		y = r2;
		
		assert r2==1;
	}
	
	public static final void thread1T() {
		final int r1 = y;
		if (r1 == 1)
			x = 1;
		else {
			x = 1;
			print("!");
		}
		
		assert r1==1;
	}
	
}

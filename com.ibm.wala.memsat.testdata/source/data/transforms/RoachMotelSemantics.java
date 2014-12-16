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
 * Roach motel semantics: 
 * J.ÊSevc“k and D.ÊAspinall. On validity of program transformations in 
 * the Java memory model. In ECOOP Õ08, volume 5142 of LNCS, pages 27Ð51. Springer Berlin, 2008.
 * 
 * @author etorlak
 */
public final class RoachMotelSemantics {
	static int x = 0, y = 0, z = 0;
	final static Object m = new Object();
	
	public static final void thread1() {
		synchronized (m) {
			x = 2;
		}
	}
	
	public static final void thread2() {
		synchronized (m) {
			x = 1;
		}
	}
	
	public static final void thread3() {
		final int r1, r2;
		r1 = x;
		synchronized (m) {
			r2 = z;
			if (r1 == 2)
				y = 1;
			else 
				y = r2;
		}
		
		assert r1==1;
		assert r2==1;
	}
	
	public static final void thread4() {
		final int r3 = y;
		z = r3;
		
		assert r3==1;
	}
	
	public static final void thread3T() {
		final int r1, r2;
		synchronized (m) {
			r1 = x;
			r2 = z;
			if (r1 == 2)
				y = 1;
			else 
				y = r2;
		}
		
		assert r1==1;
		assert r2==1;
	}
	
}

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
 * Redundant write after read elimination: 
 * J.�Sevc�k and D.�Aspinall. On validity of program transformations in 
 * the Java memory model. In ECOOP �08, volume 5142 of LNCS, pages 27�51. Springer Berlin, 2008.
 * 
 * @author etorlak
 */
public final class RedundantWriteAfterReadElimination {
	static int x = 0;
	final static Object m1, m2;
	
	static {
		m1 = new Object();
		m2 = new Object();
	}
	
	public static final void thread1() {
		synchronized (m1) {
			x = 2;
		}
	}
	
	public static final void thread2() {
		synchronized (m2) {
			x = 1;
		}
	}
	
	public static final void thread3() {
		final int r1, r2;
		synchronized (m1) {
			synchronized (m2) {
				r1 = x;
				x = r1;
				r2 = x;
			}
		}
		assert r1!=r2;
	}
	
	public static final void thread3T() {
		final int r1, r2;
		synchronized (m1) {
			synchronized (m2) {
				r1 = x;
				r2 = x;
			}
		}
		assert r1!=r2;
	}
	
}

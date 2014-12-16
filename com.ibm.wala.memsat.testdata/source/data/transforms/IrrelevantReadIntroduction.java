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
 * Irrelevant read introduction: 
 * J.ÊSevc“k and D.ÊAspinall. On validity of program transformations in 
 * the Java memory model. In ECOOP Õ08, volume 5142 of LNCS, pages 27Ð51. Springer Berlin, 2008.
 * 
 * @author etorlak
 */
public final class IrrelevantReadIntroduction {
	static int x = 0, y = 0, z = 0;
	
	public static final void thread1() {
		final int r1 = z;
		if (r1 == 0) { 
			final int r3 = x;
			if (r3 == 1)
				y = 1;
		} else {
			final int r4 = 1;
			y = r1;
		}
		
		assert r1==1;
	}
	
	public static final void thread2() {
		x = 1;
		final int r2 = y;
		z = r2;
		
		assert r2==1;
	}
	
	public static final void thread1T() {
		final int r1 = z;
		if (r1 == 0) { 
			final int r3 = x;
			if (r3 == 1)
				y = 1;
		} else {
			int r4 = x;
			r4 = 1;
			y = r1;
		}
		
		assert r1==1;
	}
}

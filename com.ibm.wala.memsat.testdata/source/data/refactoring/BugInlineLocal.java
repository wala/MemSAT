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
package data.refactoring;

public final class BugInlineLocal {

	static volatile int x = 0;
		
	public static void thread1() {
		final int r1 = x;
		final int r2 = r1 + r1;
		assert r2 == 1;
	}
	
	public static void thread2() {
		x = 1;
	}
}

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
package data.concurrent;

/**
 * Some basic concurrency tests.
 * 
 * @author Emina Torlak
 */
public final class Basics {

	int i = 0, j = 0;
	
	/**
	 * Stores i in a temporary variable,
	 * increments it by 1 and asserts that
	 * i is greater than the value in the temporary
	 * variable.
	 */
	public void inc() { 
		int old = i;
		i = i+1;
		assert i > old;
	}
	
	/**
	 * Stores i in a temporary variable,
	 * decrements it by 1 and asserts that
	 * i is less than the value in the temporary
	 * variable.
	 */
	public void dec() { 
		int old = i;
		i = i-1;
		assert i < old;
	}
	
}

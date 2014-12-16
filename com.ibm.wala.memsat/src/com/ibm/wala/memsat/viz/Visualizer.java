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
package com.ibm.wala.memsat.viz;


/**
 * Displays the results of a Miniatur analysis in a user-friendly format.
 * @specfield translation: Translation<?>
 * @specfield solution: Solution
 * @invariant solution.formula = translation.formula && solution.bounds = translation.bounds
 * @author etorlak
 */
public interface Visualizer<V> {

	/**
	 * Evaluates this.translation with respect to this.solution and returns the result
	 * as a visualization object of type V.
	 * @return a visualization of this.translation with respect to this.solution
	 */
	public abstract V visualize();
	
}

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
package data.angelic;

/**
 * Chooses values non-deterministically.
 * 
 * @author etorlak
 */
public class NonDetChoice {
	
	/**
	 * Returns a non-deterministically chosen object.
	 * @return a non-deterministically chosen object.
	 */
	public static native Object chooseObject();
	
	/**
	 * Returns a non-deterministically chosen integer.
	 * @return a non-deterministically chosen integer.
	 */
	public static native int chooseInt();
	
	/**
	 * Returns a non-deterministically chosen boolean value.
	 * @return a non-deterministically chosen boolean value.
	 */
	public static native boolean chooseBoolean();
	
}

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
package com.ibm.wala.memsat.frontEnd;

import com.ibm.wala.types.TypeReference;

/**
 * Represents the type of a value in the Wala
 * intermediate representation.
 * 
 * @author Emina Torlak
 */
public enum IRType {
	
	/** An object type. */
	OBJECT,
	
	/** A boolean type.*/
	BOOLEAN,
	
	/** An integer type (byte, char, short, int, or long)**/
	INTEGER,
	
	/** A rational real type (float or double) */
	REAL;
	
	/**
	 * Returns the IRType corresponding to the given type reference, or 
	 * null if there is IRType representation for it.
	 * @return 
	 * type.isReferenceType => OBJECT else 
	 * type = Boolean => BOOLEAN else
	 * type in Int + Long + Short + Char + Byte  => INTEGER else
	 * type in Float + Double => REAL else 
	 * null
	 */
	public static IRType convert(TypeReference type) { 
		if (type.isReferenceType()) return OBJECT;
		else if (type.equals(TypeReference.Boolean)) return BOOLEAN;
		else if (type.equals(TypeReference.Int) || 
				 type.equals(TypeReference.Long) || 
				 type.equals(TypeReference.Short) || 
				 type.equals(TypeReference.Char) || 
				 type.equals(TypeReference.Byte)) return INTEGER;
		else if (type.equals(TypeReference.Float) || 
				 type.equals(TypeReference.Double)) return REAL;
		else return null;
	}

}

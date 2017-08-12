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
package com.ibm.wala.memsat.frontEnd.types;

import com.ibm.wala.analysis.typeInference.JavaPrimitiveType;
import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.memsat.frontEnd.IRType;

class MiniaturJavaTypeData implements MiniaturTypeData {
  
  private final TypeInference TI;

  MiniaturJavaTypeData(CGNode node, IClassHierarchy cha) {
    TI =  new AstJavaTypeInference(node.getIR(), true);
    TI.solve();
  }

  public String toString() {
    return TI.toString();
  }

  public IRType typeOf(int valueNumber) {
    if (TI.getType(valueNumber) instanceof PrimitiveType) { 
      if (TI.getType(valueNumber) == JavaPrimitiveType.BOOLEAN) {
	return IRType.BOOLEAN;

      } else if (TI.getType(valueNumber) == JavaPrimitiveType.FLOAT ||
		 TI.getType(valueNumber) == JavaPrimitiveType.DOUBLE) 
      {
	return IRType.REAL;

      } else {
	  return IRType.INTEGER;
      }

    } else {
      return IRType.OBJECT;
    }
  }
}

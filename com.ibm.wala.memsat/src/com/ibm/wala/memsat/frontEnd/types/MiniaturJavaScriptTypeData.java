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

import java.util.Iterator;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

class MiniaturJavaScriptTypeData implements MiniaturTypeData {

  private final CGNode node;

  private final PointerAnalysis<InstanceKey> PA;

  MiniaturJavaScriptTypeData(CGNode node, PointerAnalysis<InstanceKey> PA) {
    this.node = node;
    this.PA = PA;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer("types for " + node + "\n");
    IR ir = node.getIR();
    SymbolTable st = ir.getSymbolTable();
    for(int vn = 1; vn <= st.getMaxValueNumber(); vn++) {
      if (st.isNullConstant(vn)) {
	  buf.append(vn + " --> <null constant>\n");
      }
	    
      PointerKey pk = new LocalPointerKey(node, vn);
      OrdinalSet<? extends InstanceKey> types = PA.getPointsToSet( pk );

      buf.append(vn + " --> " + types + "\n");
    }

    return buf.toString();
  }
	
  private boolean isTypeValue(int valueNumber, TypeReference type) {
    IR ir = node.getIR();
    if (ir.getSymbolTable().isNullConstant(valueNumber)) {
	return false;
    }

    PointerKey pk = new LocalPointerKey(node, valueNumber);
    OrdinalSet<? extends InstanceKey> types = PA.getPointsToSet( pk );
    if (types.isEmpty()) {
      assert
        !types.isEmpty() :
        "no types for " + valueNumber + " of " + node;
    }
    boolean result = true;
    for(Iterator<? extends InstanceKey> ts = types.iterator(); ts.hasNext(); ) {
      InstanceKey t = ts.next();
      if (! t.getConcreteType().getReference().equals(type)) {
	result = false;
      }
    }
    return result;
  }

  public IRType typeOf(int valueNumber) {
    if (isTypeValue(valueNumber, JavaScriptTypes.Boolean)) {
      return IRType.BOOLEAN;
    } else if (isTypeValue(valueNumber, JavaScriptTypes.Number)) {
      // TODO: need to distinguish real/integer somehow
      return IRType.INTEGER;
    } else {
      return IRType.OBJECT;
    }
  }
}

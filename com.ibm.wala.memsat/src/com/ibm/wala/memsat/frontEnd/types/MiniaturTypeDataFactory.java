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

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.debug.Assertions;

public class MiniaturTypeDataFactory {
  
  private final PointerAnalysis<InstanceKey> PA;

  private final IClassHierarchy cha;

  public MiniaturTypeDataFactory(PointerAnalysis<InstanceKey> PA, IClassHierarchy cha) {
    this.cha = cha;
    this.PA = PA;
  }

  public MiniaturTypeData get(CGNode node) {
    Language L = 
      node.getMethod().getDeclaringClass().getClassLoader().getLanguage();
      if (L.equals(Language.JAVA)) {
      return new MiniaturJavaTypeData(node, cha);
    } else if (L.equals(JavaScriptLoader.JS)) {
      return new MiniaturJavaScriptTypeData(node, PA);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

}


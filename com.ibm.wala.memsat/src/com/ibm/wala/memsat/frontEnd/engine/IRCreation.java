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
package com.ibm.wala.memsat.frontEnd.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;

public class IRCreation {
  MiniaturAnalysisEngine engine;

  public IRCreation(MiniaturAnalysisEngine engine) {
    this.engine = engine;
  }

  public Map<IMethod, IR> createIR() throws java.io.IOException {
    Map<IMethod, IR> result = new HashMap<IMethod, IR>();
    
    engine.buildAnalysisScope();

    IClassHierarchy cha = engine.getClassHierarchy();

    SSAOptions options = new SSAOptions();
    IRFactory<IMethod> F = AstIRFactory.makeDefaultFactory();
    
    for(Iterator<IClass> clss = cha.iterator(); clss.hasNext(); ) {
      IClass cls = clss.next();
      ClassLoaderReference clr = cls.getClassLoader().getReference(); 
      if ( !(clr.equals(ClassLoaderReference.Primordial)  
	                    || 
	     clr.equals(ClassLoaderReference.Extension)))
      {		
	for (Iterator<? extends IMethod> ms = cls.getDeclaredMethods().iterator(); 
	     ms.hasNext(); ) 
	{
	  IMethod m = ms.next();
	  IR ir = F.makeIR(m, Everywhere.EVERYWHERE, options);
	  result.put(m, ir);
	}
      }
    }
    
    return result;
  }
}

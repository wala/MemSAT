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

import java.io.File;
import java.util.Collections;

import com.ibm.wala.cast.js.client.JavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.translator.CAstRhinoLoopUnwindingTranslatorFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class MiniaturJavaScriptAnalysisEngine 
  extends JavaScriptAnalysisEngine 
  implements MiniaturAnalysisEngine
{

  public MiniaturJavaScriptAnalysisEngine() {
    setTranslatorFactory(new CAstRhinoLoopUnwindingTranslatorFactory());
  }

  public void buildAnalysisScope() {
    super.buildAnalysisScope();
  }

  public CallGraphBuilder buildCallGraph(IClassHierarchy cha, AnalysisOptions options, boolean savePointerAnalysis, IProgressMonitor monitor) throws com.ibm.wala.util.CancelException {
    return super.buildCallGraph(cha, options, savePointerAnalysis, monitor);
  }

  public IClassHierarchy getClassHierarchy() {
    if (super.getClassHierarchy() == null) {
      setClassHierarchy( buildClassHierarchy() );
    }

    return super.getClassHierarchy();
  }

  public PointerAnalysis getPointerAnalysis() {
    return super.getPointerAnalysis();
  }

  public Iterable<Entrypoint> getEntrypoints() {
    return new JavaScriptEntryPoints(
      getClassHierarchy(), 
      loaderFactory.getTheLoader());
  }
  
}

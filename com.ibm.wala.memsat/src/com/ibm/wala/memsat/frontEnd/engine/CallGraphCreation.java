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

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import java.io.*;

public class CallGraphCreation {
  private final MiniaturAnalysisEngine engine;
  private AnalysisOptions options;

  public CallGraphCreation(MiniaturAnalysisEngine engine) {
    this.engine = engine;
  }

  public AnalysisOptions getOptions() {
    return options;
  }

  public CallGraph createCallGraph() throws com.ibm.wala.util.CancelException, IOException {
    engine.buildAnalysisScope();
    
    IClassHierarchy cha = engine.getClassHierarchy();

    Iterable<Entrypoint> entrypoints = engine.getEntrypoints();

    options = engine.getDefaultOptions(entrypoints);
    options.setReflectionOptions(ReflectionOptions.NONE);
    
    PropagationCallGraphBuilder builder = 
      (PropagationCallGraphBuilder)engine.buildCallGraph(cha, options, true, null);

    CallGraph CG = builder.makeCallGraph(options);

    return CG;
  }
}

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

import java.io.IOException;

import com.ibm.wala.client.AnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public interface MiniaturAnalysisEngine extends AnalysisEngine {
  
  CallGraphBuilder buildCallGraph(IClassHierarchy cha, AnalysisOptions options, boolean savePointerAnalysis, IProgressMonitor monitor) throws com.ibm.wala.util.CancelException;

  PointerAnalysis getPointerAnalysis();

  IClassHierarchy getClassHierarchy();

  Iterable<Entrypoint> getEntrypoints();

  void buildAnalysisScope() throws IOException;
  
  void setExclusionsFile(String file);
  
}

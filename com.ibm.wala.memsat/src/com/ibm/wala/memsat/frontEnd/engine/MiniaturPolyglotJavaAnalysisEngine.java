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
import java.io.IOException;
import java.util.List;

import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotUnwoundIRTranslatorExtension;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class MiniaturPolyglotJavaAnalysisEngine
	extends PolyglotJavaSourceAnalysisEngine
	implements MiniaturAnalysisEngine
{
	// private final static int defaultLoopUnrollDepth = 3;

	private final int unrollDepth;

	private List<MethodReference> methods;

	public MiniaturPolyglotJavaAnalysisEngine(int unrollDepth, List<MethodReference> methods) {
		super();
		this.unrollDepth = unrollDepth;
		this.methods = methods;
	}

	public IRTranslatorExtension getTranslatorExtension() {
		return new PolyglotUnwoundIRTranslatorExtension(unrollDepth) {
			@Override
			public boolean getReplicateForDoLoops() {
				return true;
			}
		};
	}

	public void buildAnalysisScope() throws IOException {
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
		return new MiniaturJavaEntrypoints(methods, getClassHierarchy());
	}

	public void addSourceModule(String relativeName) {
		String testSourceDirName  = 
			System.getProperty("user.dir") + File.separator + 
			relativeName;

		File srcFile = new File(testSourceDirName);
			
		assert srcFile.exists() : "cannot find " + srcFile;
		addSourceModule(new SourceDirectoryTreeModule( srcFile ));
	}
}

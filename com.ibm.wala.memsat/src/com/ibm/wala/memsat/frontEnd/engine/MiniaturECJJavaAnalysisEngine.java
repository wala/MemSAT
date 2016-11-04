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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.java.translator.jdt.JDTJava2CAstTranslator;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJSourceModuleTranslator;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.RangePosition;
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.SetOfClasses;

public class MiniaturECJJavaAnalysisEngine
	extends ECJJavaSourceAnalysisEngine
	implements MiniaturAnalysisEngine
{
	// private final static int defaultLoopUnrollDepth = 3;

	private final int unrollDepth;

	private List<MethodReference> methods;

	public MiniaturECJJavaAnalysisEngine(int unrollDepth, List<MethodReference> methods) {
		super();
		this.unrollDepth = unrollDepth;
		this.methods = methods;
	}

	@Override
	protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
		return new ECJClassLoaderFactory(exclusions) {
			@Override
			protected ECJSourceLoaderImpl makeSourceLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent) throws IOException {
				return new ECJSourceLoaderImpl(classLoaderReference, parent, getExclusions(), cha) {
					@Override
					protected SourceModuleTranslator getTranslator() {
						return new ECJSourceModuleTranslator(cha.getScope(), this) {
							@Override
							protected JDTJava2CAstTranslator makeCAstTranslator(CompilationUnit astRoot, String fullPath) {
								return new JDTJava2CAstTranslator<Position>(sourceLoader, astRoot, fullPath, true) { 
									@Override
									public CAstEntity translateToCAst() {
										CAstEntity ast = super.translateToCAst();
										AstLoopUnwinder unwind = new AstLoopUnwinder(new CAstImpl(), true, unrollDepth);
										return unwind.translate(ast);
									}

									@Override
								      public Position makePosition(int start, int end) {
								        try {
								          return new RangePosition(new URL("file://" + fullPath), this.cu.getLineNumber(start), start, end);
								        } catch (MalformedURLException e) {
								          throw new RuntimeException("bad file: " + fullPath, e);
								        }
								      }
									
								};
							}
						};
					}
				};
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

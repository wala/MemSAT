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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.java.translator.jdt.JDTClassLoaderFactory;
import com.ibm.wala.cast.java.translator.jdt.JDTJava2CAstTranslator;
import com.ibm.wala.cast.java.translator.jdt.JDTSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.JDTSourceModuleTranslator;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ide.util.JdtPosition;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.memsat.MiniaturPlugin;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.SetOfClasses;

public class MiniaturJDTJavaAnalysisEngine
	extends JDTJavaSourceAnalysisEngine
	implements MiniaturAnalysisEngine
{
	// private final static int defaultLoopUnrollDepth = 3;

	private final int unrollDepth;

	private List<MethodReference> methods;

	public MiniaturJDTJavaAnalysisEngine(String project, int unrollDepth, List<MethodReference> methods) throws IOException, CoreException {
		super(project);
		this.unrollDepth = unrollDepth;
		this.methods = methods;
		setExclusionsFile(
			(new EclipseFileProvider()).getFileFromPlugin(MiniaturPlugin.getDefault(), "data/MiniaturExclusions.txt").getAbsolutePath());
	}

	@Override
	protected ClassLoaderFactory makeClassLoaderFactory(SetOfClasses exclusions) {
		return new JDTClassLoaderFactory(exclusions) {
			@Override
			protected JDTSourceLoaderImpl makeSourceLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent) {
				return new JDTSourceLoaderImpl(classLoaderReference, parent, cha) {
					@Override
					protected SourceModuleTranslator getTranslator() {
						return new JDTSourceModuleTranslator(cha.getScope(), this) {
							@Override
							protected JDTJava2CAstTranslator makeCAstTranslator(CompilationUnit astRoot, final IFile sourceFile, String fullPath) {
								return new JDTJava2CAstTranslator(sourceLoader, astRoot, fullPath, true) { 
									@Override
									public CAstEntity translateToCAst() {
										CAstEntity ast = super.translateToCAst();
										AstLoopUnwinder unwind = new AstLoopUnwinder(new CAstImpl(), true, unrollDepth);
										return unwind.translate(ast);
									}
									
									@Override
									public JdtPosition makePosition(int start, int end) {
										return new JdtPosition(start, end, this.cu.getLineNumber(start), this.cu.getLineNumber(end), sourceFile, this.fullPath);
									}
								};
							}
						};
					}
				};
			}
		};
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

	public PointerAnalysis<? super InstanceKey> getPointerAnalysis() {
		return super.getPointerAnalysis();
	}

	public Iterable<Entrypoint> getEntrypoints() {
		return new MiniaturJavaEntrypoints(methods, getClassHierarchy());
	}
}

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
/**
 * 
 */
package com.ibm.wala.memsat.frontEnd;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.eclipse.core.runtime.CoreException;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.core.WalaInformationImpl;
import com.ibm.wala.memsat.frontEnd.engine.MiniaturAnalysisEngine;
import com.ibm.wala.memsat.frontEnd.engine.MiniaturJDTJavaAnalysisEngine;
import com.ibm.wala.memsat.frontEnd.engine.MiniaturECJJavaAnalysisEngine;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.Graph;

/**
 * Computes and analyzes the call graph for a given
 * {@linkplain AnalysisScope analysis scope} and a set of 
 * {@linkplain MethodReference methods}, using the wala library.
 * 
 * @see WalaInformation
 * 
 * @author Emina Torlak
 */
public final class WalaEngine {
	/**
	 * The engine cannot be instantiated from the outside.
	 */
	private WalaEngine() {}

	private static void doJavaLibDir(JavaSourceAnalysisEngine engine, String libDir) throws IOException {
		if (new File(libDir).exists()) {
			File[] libJarFiles = new File(libDir).listFiles(
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".jar");
						}
					});
			for(int i = 0; i < libJarFiles.length; i++) {
				try {
					JarFileModule m = new JarFileModule(new JarFile(libJarFiles[i]));
					engine.addSystemModule(m);
				} catch (ZipException e) {
					System.err.println("trouble with " + libJarFiles[i]);
				}
			}
		}
	}

	private static void setExclusions(MiniaturAnalysisEngine engine, List<File> relativeNames) {
		final String exclusions  = System.getProperty("com.ibm.wala.memsat.exclusions") ;
		if (exclusions != null) { 
			engine.setExclusionsFile(exclusions);
		} 
				
		if (exclusions == null) {
			for(File relativeName : relativeNames) {

				String testSourceDirName  = 
					System.getProperty("user.dir") + File.separator + 
					relativeName.getPath();

				File appExclusionsFile = new File(testSourceDirName + "_exclusions.xml");
				if (appExclusionsFile.exists()) {
					engine.setExclusionsFile(appExclusionsFile.getAbsolutePath());
				}
			} 
		}
	}

	private static void setPolyglotJavaEngineScope(JavaSourceAnalysisEngine engine, List<File> relativeNames) 
	{
		try {
			String home = System.getProperty("java.home");
			doJavaLibDir(engine, home + File.separator + "lib");
			doJavaLibDir(engine, home + "/../Classes");

			for(File relativeName : relativeNames) {
				engine.addSourceModule(new SourceDirectoryTreeModule(relativeName));

				String testSourceDirName  = 
					System.getProperty("user.dir") + File.separator + 
					relativeName.getPath();
						
				File appLibsDir = new File(testSourceDirName + "Libs");
				if (appLibsDir.exists() && appLibsDir.isDirectory()) {
					File[] appJarFiles = appLibsDir.listFiles(
							new FilenameFilter() {
								public boolean accept(File dir, String name) {
									return name.endsWith(".jar");
								}
							});
					for(int i = 0; i < appJarFiles.length; i++) {
						engine.addCompiledModule(
								new JarFileModule(new JarFile(appJarFiles[i])));
					}
				}
			}

		} catch (IOException e) {
			assert false : e.toString();
		}
	}

	/**
	 * Returns the {@linkplain WalaInformation result} of 
	 * analyzing the given methods in the specified source directory
	 * @requires all of the given method references are in the specified directory  
	 * @return { wInfo: WalaInformation | wInfo.methods = methods }
	 */
	public static WalaInformation analyze(Graph<MethodReference> methods, List<File> sourceDirs, Options options) throws com.ibm.wala.util.CancelException, java.io.IOException {
		MiniaturAnalysisEngine engine = null;
		
		if (options.getEclipseProjectName() == null) {
			MiniaturECJJavaAnalysisEngine e = new MiniaturECJJavaAnalysisEngine(
				options.loopUnrollDepth(),
				Iterator2Collection.toList(methods.iterator()));
			
			setPolyglotJavaEngineScope(e, sourceDirs);
			
			engine = e;
		} else {
			try {
				engine = new MiniaturJDTJavaAnalysisEngine(
					options.getEclipseProjectName(),
					options.loopUnrollDepth(),
					Iterator2Collection.toList(methods.iterator()));
			} catch (IllegalArgumentException e) {
				assert false : e;
			} catch (CoreException e) {
				assert false : e;
			}
		}	
		
		setExclusions(engine, sourceDirs);

		return new WalaInformationImpl(options, engine, methods);
	}
	
}

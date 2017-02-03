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
package com.ibm.wala.memsat;

import java.io.File;
import java.util.List;

import com.ibm.wala.memsat.frontEnd.WalaEngine;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.translation.concurrent.ConcurrentTranslation;
import com.ibm.wala.memsat.translation.concurrent.ConcurrentTranslator;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslation;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslator;
import com.ibm.wala.memsat.util.Graphs;
import com.ibm.wala.memsat.viz.StringVisualizer;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

import kodkod.engine.Solution;
import kodkod.engine.Solver;

/**
 * Provides a simple interface to the Miniatur analysis engine.
 * 
 * @specfield options: Options // translation and analysis options
 * @author Emina Torlak
 */
public final class Miniatur {
	private final Options options;
	
	/**
	 * Constructs a Miniatur instance that will use the given Options
	 * for translation and analysis.  
	 * @effects this.options' = options
	 * @throws NullPointerException - options = null
	 */
	public Miniatur(Options options) {
		if (options==null) 
			throw new NullPointerException();
		this.options = options;
	}
	
	/**
	 * Returns the analysis options used by this instance of Minatur.
	 * @return this.options
	 */
	public Options options() { return options; }
	
	/**
	 * Analyzes the given method from the specified source directory and
	 * returns the results. 
	 * 
	 * <p>Miniatur analysis works by looking for legal (bounded) executions of the given
	 * methods that violate the assertions contained in those methods.  The executions
	 * are finitized using the finitization parameters 
	 * (e.g. <tt>loopUnroll</tt>, <tt>recursionLimit</tt>, etc.) in this.Options.  The
	 * results of the analysis include a counterexample, if any, to violated assertions,
	 * and statistics about the various phases of the analysis.
	 * 
	 * @return Results of analyzing the given method
	 */
	public Results<SequentialTranslation> analyze(MethodReference method, List<File> sourceDirs)  throws com.ibm.wala.util.CancelException, java.io.IOException {	
		final long startWala = System.currentTimeMillis();
		final WalaInformation info = WalaEngine.analyze(Graphs.graph(method), sourceDirs, options);
		final long endWala = System.currentTimeMillis();
		
		assert info.threads().getNumberOfNodes()==1;
		
		final long startMiniatur = System.currentTimeMillis();
		final SequentialTranslation translation = SequentialTranslator.translate(info, options);
		final long endMiniatur = System.currentTimeMillis();
		
		final Solver solver = new Solver(options.kodkodOptions());
		final Solution sol = solver.solve(translation.formula(), translation.bounds());
		
		return new Results<SequentialTranslation>(translation, sol, 
				endWala-startWala, endMiniatur-startMiniatur, 
				StringVisualizer.viz(translation, sol));
	}
	
	/**
	 * Analyzes the given methods from the specified source directory and
	 * returns the results.  The code is analyzed as though each method is 
	 * running in a separate thread.  If there is an edge
	 * from A to B in the given graph, then the thread executing A is treated as
	 * finishing before the thread executing B. The graph must be acyclic.  
	 * 
	 * <p>Miniatur analysis works by looking for legal (bounded) executions of the given
	 * methods that violate the assertions contained in those methods.  The executions
	 * are finitized using the finitization parameters 
	 * (e.g. <tt>loopUnroll</tt>, <tt>recursionLimit</tt>, etc.) in this.Options.  The
	 * results of the analysis include a counterexample, if any, to violated assertions,
	 * and statistics about the various phases of the analysis.
	 * 
	 * @return Results of analyzing the given methods
	 */
	public Results<ConcurrentTranslation> analyze(Graph<MethodReference> methods, List<File> sourceDirs)  throws com.ibm.wala.util.CancelException, java.io.IOException {
		final long startWala = System.currentTimeMillis();
		final WalaInformation info = WalaEngine.analyze(methods, sourceDirs, options);
		final long endWala = System.currentTimeMillis();
		
		final long startMiniatur = System.currentTimeMillis();
		final ConcurrentTranslation translation = ConcurrentTranslator.translate(info, options);
		final long endMiniatur = System.currentTimeMillis();
		
		final Solver solver = new Solver(options.kodkodOptions());
		final Solution sol = solver.solve(translation.formula(), translation.bounds());
		
		return new Results<ConcurrentTranslation>(translation, sol, 
				endWala-startWala, endMiniatur-startMiniatur, 
				StringVisualizer.viz(translation, sol));
	}
	
	/**
	 * Returns a string view of this instance of Miniatur.
	 * @return a string view of this instance of Miniatur.
	 */
	public String toString() {
		return "Miniatur 2.0 configured with the following options\n" + options;
	}
	
}

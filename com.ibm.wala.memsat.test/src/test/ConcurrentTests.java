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
package test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.Results;
import com.ibm.wala.memsat.concurrent.Execution;
import com.ibm.wala.memsat.concurrent.Justification;
import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.memsat.translation.concurrent.ConcurrentTranslation;
import com.ibm.wala.memsat.util.Strings;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.Graph;

import kodkod.ast.Expression;
import kodkod.ast.Relation;
import kodkod.engine.Solution;
import kodkod.engine.Solution.Outcome;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.ucore.RCEStrategy;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;


/**
 * Common harness for testing concurrent code.
 * @author etorlak
 */
public abstract class ConcurrentTests {
	
	private final void addDisplayStrings(Execution exec, String suffix, Map<String,String> display) { 
		final Class<? extends Execution> c = exec.getClass();
		for(Method m : c.getMethods()) {
			if (m.getParameterTypes().length==0) { 
				if (Expression.class.isAssignableFrom(m.getReturnType())) { 
					try {
						m.setAccessible(true);
						final Expression expr = (Expression)m.invoke(exec, new Object[0]);
						if (!(expr instanceof Relation))
							display.put(expr.toString(), m.getName()+suffix);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
 			}
		}
	}
	
	private final Map<String,String> printProblem(ConcurrentTranslation transl) { 
		System.out.println();
		final Justification just = transl.context();
		final Map<String, String> display = new LinkedHashMap<String, String>();
		addDisplayStrings(just.execution(), "", display);
		for(int i = 0, max = just.speculations().size(); i < max; i++) { 
			addDisplayStrings(just.speculations().get(i), String.valueOf(i), display);
		}
		/*
		System.out.println(Strings.prettyPrint(transl.formula(),2,200,display));
		System.out.println("BOUNDS: ");
		final Bounds bounds = transl.bounds();
		for(Relation r : bounds.relations()) { 
			final TupleSet lower = bounds.lowerBound(r);
			final TupleSet upper = bounds.upperBound(r);
			if (lower.equals(upper)) 
				System.out.println(r + " = " + lower);
			else
				System.out.println(r + " : [" + lower  + ", "+ upper +"]");
		}
		*/
		return display;
	}

	protected final void test(Miniatur miniatur, File srcPath, Graph<MethodReference> methods, boolean sat) { 
		test(miniatur, Collections.singletonList(srcPath), methods, sat);
	}
	
	/**
	 * Checks that the result produced by applying the given miniatur instance to the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	protected final void test(Miniatur miniatur, List<File> srcPath, Graph<MethodReference> methods, boolean sat) { 
		
		try {

			
			final Results<ConcurrentTranslation> results = miniatur.analyze(methods, srcPath);
			final Solution solution = results.solution();
			final Map<String,String> display = printProblem(results.translation());
			
			final long core;
			if (solution.instance()==null) { 
				System.out.println("Outcome: "+solution.outcome());
				System.out.println(solution.stats());
				final long corestart = System.currentTimeMillis();
				solution.proof().minimize(new RCEStrategy(solution.proof().log()));
				core = System.currentTimeMillis() - corestart;
				System.out.println("CORE: ");
				System.out.println(Strings.prettyPrint(solution.proof().highLevelCore().values(), 2, 200, display));
			
			} else {
				core = 0;
				System.out.println("Outcome: "+solution.outcome());
				System.out.println(solution.stats());
				/*
				for(Relation r : solution.instance().relations()) { 
					System.out.println(r + " = " + solution.instance().tuples(r));
				}
				System.out.println("\n****************");
				System.out.println(solution.instance().universe());
				System.out.println(results);
				*/
			}
//			System.out.println(results);
			
			final String expected = sat ? "y" : "n";
			final String found = solution.instance()!=null ? "y":"n";
			
			final long time = results.analysisTime() + results.translationTime() + solution.stats().translationTime() + solution.stats().solvingTime() + core;
			System.out.println("\nOUTPUT\t" + expected + "\t" + found + "\t"  + time + "\t" + solution.stats().variables() + "\t" + solution.stats().clauses());
			
			if (sat) {
				assert solution.outcome().equals(Outcome.SATISFIABLE) ||
				solution.outcome().equals(Outcome.TRIVIALLY_SATISFIABLE);
			} else {
				
				assert solution.outcome().equals(Outcome.UNSATISFIABLE) ||
				solution.outcome().equals(Outcome.TRIVIALLY_UNSATISFIABLE);
			}	
			
			
			
		} catch (CancelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Returns a miniatur analysis engine generated by calling
	 * {@linkplain #miniatur(int, Set) miniatur(maxSpeculations, Collections.EMPTY_SET)}.
	 * @return a miniatur analysis engine generated by calling
	 * {@linkplain #miniatur(int, Set) miniatur(maxSpeculations, Collections.EMPTY_SET)}.
	 */
	@SuppressWarnings("unchecked")
	protected final Miniatur miniatur(int maxSpeculations) {
		return miniatur(maxSpeculations, Collections.EMPTY_SET);
	}
	
	/**
	 * Returns a miniatur analysis engine that uses the memory model generated by calling 
	 * {@linkplain #memoryModel(int, Set) memoryModel(maxSpeculations, special)} and that treats user assertions as assumes.
	 * @return a miniatur analysis engine that uses the memory model generated by calling 
	 * {@linkplain #memoryModel(int, Set) memoryModel(maxSpeculations, special)} and that treats user assertions as assumes.
	 */
	protected Miniatur miniatur(int maxSpeculations, Set<MethodReference> special) { 
		return new Miniatur(getOptions(maxSpeculations, special));
	}

	protected Options getOptions(int maxSpeculations, Set<MethodReference> special) {
		final Options opts = new Options();
    opts.setMemoryModel(memoryModel(maxSpeculations, special));
		opts.setAssertsAreAssumptions(true);
		opts.kodkodOptions().setBitwidth(3);
		opts.kodkodOptions().setLogTranslation(1);
		opts.kodkodOptions().setSolver(SATFactory.MiniSatProver);
//		opts.kodkodOptions().setCoreGranularity(3);
		return opts;
	}
	
	/**
	 * Returns a memory model instance that allows at most the given number of speculations.
	 * This parameter may be ignored if the memory model permits no speculations.
	 * @return a memory model instance that allows at most the given number of speculations.
	 */
	protected abstract MemoryModel memoryModel(int maxSpeculations, Set<MethodReference> special);
}

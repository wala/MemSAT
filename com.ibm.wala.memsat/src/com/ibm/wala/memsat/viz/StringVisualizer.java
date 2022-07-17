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
package com.ibm.wala.memsat.viz;

import java.util.Set;

import com.ibm.wala.memsat.translation.Translation;
import com.ibm.wala.memsat.translation.TranslationWarning;
import com.ibm.wala.memsat.translation.concurrent.ConcurrentTranslation;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslation;
import com.ibm.wala.core.util.warnings.Warning;

import kodkod.engine.Evaluator;
import kodkod.engine.Solution;

/**
 * Displays the results of a Miniatur analysis as a human-readable string.
 * @specfield translation: T
 * @specfield solution: Solution
 * @invariant solution.formula = translation.formula && solution.bounds = translation.bounds
 * @author etorlak
 */
public abstract class StringVisualizer<T extends Translation<?>> implements Visualizer<String> {
	final T translation;
	final Evaluator eval;
	
	/**
	 * Constructs a new string visualizer from the given translation and solution.
	 * @requires solution.instance!=null
	 * @requires solution.formula = translation.formula && solution.bounds = translation.bounds
	 */
	StringVisualizer(T translation, Solution solution) {
		this.translation = translation;
		this.eval = new Evaluator(solution.instance(), translation.factory().options().kodkodOptions());
	}
	
	/**
	 * Returns a string visualizer for the given sequential translation and solution.
	 * @return a string visualizer for the given sequential translation and solution.
	 */
	public static Visualizer<String> viz(SequentialTranslation translation, Solution sol) { 
		return sol.instance()==null ? empty() : new SequentialStringVisualizer(translation, sol);
	}
	
	/**
	 * Returns a string visualizer for the given concurrent translation and solution.
	 * @return a string visualizer for the given concurrent translation and solution.
	 */
	public static Visualizer<String> viz(ConcurrentTranslation translation, Solution sol) { 
		return sol.instance()==null ? empty() : new ConcurrentStringVisualizer(translation, sol);
	}
	
	private static Visualizer<String> empty() { 
		return new Visualizer<String>() { 
			public String visualize() { return ""; }
		};
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.ibm.wala.memsat.viz.Visualizer#visualize()
	 */
	public abstract String visualize();
	
//	/**
//	 * Appends a visualization header to the given buffer.
//	 */
//	final void preamble(Appendable buffer) { 
//		
//	}
	
	/**
	 * Appends a string representation of the given warning set to the specified
	 * builder or leaves the buffer unchanged if the set is empty.
	 */
	final void warnings(StringBuilder str, Set<TranslationWarning> warnings) { 
		if (!warnings.isEmpty()) { 
			str.append("warnings:\n");
			for(Warning w : warnings) { 
				str.append(" "+w+"\n");
			}
		}
	}
}

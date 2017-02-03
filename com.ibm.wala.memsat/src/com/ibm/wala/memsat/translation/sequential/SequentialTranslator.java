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
package com.ibm.wala.memsat.translation.sequential;

import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.translation.Environment;
import com.ibm.wala.memsat.translation.MethodTranslation;
import com.ibm.wala.memsat.translation.Translator;
import com.ibm.wala.memsat.util.Nodes;

import kodkod.ast.Formula;
import kodkod.instance.Bounds;
import kodkod.instance.Universe;

/**
 * A translator for sequential Wala programs.
 * 
 * @author Emina Torlak
 */
public final class SequentialTranslator {
	private SequentialTranslator() {}
	
	/**
	 * Returns the translation of the method described by the 
	 * given wala information instance, with respect to the 
	 * specified translation options.
	 * @requires info.threads() = 1
	 * @return { t : Translation | t.info = info and t.options = options}
	 */
	public static SequentialTranslation translate(final WalaInformation info, final Options options) { 
		assert info.threads().getNumberOfNodes() == 1;

		final ExpressionFactory factory = new ExpressionFactory(info, options);
		final MethodTranslation transl = Translator.translate(
				(new Environment(factory)).push(info.threads().iterator().next()), 
				new SequentialMemoryHandler());				
		final Bounds bounds = new Bounds(new Universe(factory.atoms()));
		factory.boundAll(bounds);

		final Formula invariants = factory.invariants();
		final Formula assumptions = Formula.and(transl.assumptions());
		final Formula assertions;
		if (options.assertsAreAssumptions()) {
			assertions = Formula.and(transl.assertions());
		} else {
			assertions = Formula.or(transl.assertions());
		}

		final Formula formula = Nodes.simplify(Formula.and(invariants, assumptions, assertions), bounds);

		return new SequentialTranslation(formula, bounds, factory, transl);
	}
	
}

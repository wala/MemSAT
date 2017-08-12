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
package test.angelic;

import static test.TestUtil.method;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.Results;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslation;
import com.ibm.wala.util.CancelException;

import data.angelic.Angelic;
import data.angelic.TokenizerClient;
import kodkod.ast.BinaryExpression;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Node;
import kodkod.ast.Relation;
import kodkod.ast.operator.ExprOperator;
import kodkod.ast.visitor.AbstractReplacer;
import kodkod.engine.Solution;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.ucore.RCEStrategy;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;
import kodkod.util.ints.IntIterator;

/**
 * @author etorlak
 *
 */
public class AngelicTests {
	private static final File SRC_DATA = new File("source/data/angelic");
	
	@SuppressWarnings("unchecked")
	private Formula makeReadable(Formula formula) { 
		final List<Formula> replacements = new ArrayList<Formula>();
		final Map<Expression, Integer> levels = new LinkedHashMap<Expression, Integer>();
		final AbstractReplacer replacer = new AbstractReplacer(Collections.EMPTY_SET) { 
			protected <N extends Node> N cache(N node, N replacement) {
				cache.put(node, replacement);
				return replacement;
			}
			public Expression visit(BinaryExpression expr) { 
				Expression ret = lookup(expr);
				if (ret!=null) return ret;
				final Expression left = expr.left().accept(this);
				final Expression right = expr.right().accept(this);
				if (expr.op()==ExprOperator.OVERRIDE && left instanceof Relation) { 
					final int level = levels.containsKey(left) ? levels.get(left) : 0;
					final Relation r = Relation.nary(((Relation)left).name()+"'", expr.arity());
					replacements.add(r.eq(left.compose(expr.op(), right)));
					levels.put(left, level+1);
					return cache(expr, r);
				}
				return cache(expr, left.compose(expr.op(), right));
			}
		};
		
		int size;
		do {
			size = replacements.size();
			formula = formula.accept(replacer);
		} while (size < replacements.size());
		
		return formula.and(Formula.and(replacements));
	}

	private Solution test(File srcpath, Class<?> klass, String methodname, boolean expected){
		return test(Collections.singletonList(srcpath), klass, methodname, expected);
	}
	
	protected Options getOptions() {
		Options o = new Options();
		o.setOpenWorldScopeSize(1);
		o.setAssertsAreAssumptions(true);
		o.setNumberOfIndexAtoms(3);
		o.setloopUnrollDepth(3);
		o.kodkodOptions().setSolver(SATFactory.MiniSatProver);
		o.kodkodOptions().setCoreGranularity(2);
		o.kodkodOptions().setLogTranslation(1);
		return o;
	}
	
	private Solution test(List<File> srcpath, Class<?> klass, String methodname, boolean expected){
		try {
			final Miniatur miniatur = new Miniatur(getOptions());
						
			final Results<SequentialTranslation> results = miniatur.analyze(method(klass, methodname), srcpath);
			
			final Solution solution = results.solution();
			final boolean actual = (solution.instance()!=null); 
			
//			System.out.println("FORMULA: ");
//			System.out.println(Strings.prettyPrint(makeReadable(results.translation().formula()), 2, 200, Collections.EMPTY_MAP));
			
/*			System.out.println("BOUNDS: ");
			final Bounds bounds = results.translation().bounds();
			for(Relation r : bounds.relations()) { 
				final TupleSet lower = bounds.lowerBound(r);
				final TupleSet upper = bounds.upperBound(r);
				if (lower.equals(upper)) 
					System.out.println(r + " = " + lower);
				else
					System.out.println(r + " : [" + lower  + ", "+ upper +"]");
			}
			for(IntIterator itr = bounds.ints().iterator(); itr.hasNext(); ) {
				final int i = itr.next();
				System.out.println(i + " = " + bounds.exactBound(i));
			}
*/			
			
//			System.out.println(results.toString());
			if (!actual) { // get the core
				final long corestart = System.currentTimeMillis();
				solution.proof().minimize(new RCEStrategy(solution.proof().log()));
				final long core = System.currentTimeMillis() - corestart;
				System.out.println("CORE (" + core + " ms): " + solution.proof().highLevelCore().size());
//				System.out.println(Strings.prettyPrint(solution.proof().highLevelCore().values(), 2, 200, Collections.EMPTY_MAP));
			}
			
			Assert.assertEquals(expected, actual);
			
			return solution;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Test
	public void angelicTest00(){
		test(SRC_DATA, Angelic.class, "test00", false);
	}
	
	@Test
	public void angelicTest01(){
		test(SRC_DATA, Angelic.class, "test01", true);
	}
	
//	@Test
	public void angelicTest02(){
		test(SRC_DATA, Angelic.class, "test02", true);
	}

	@Test
	public void tokenizerTest00() {
		test(SRC_DATA, TokenizerClient.class, "test00", false);
	}
	
	@Test
	public void tokenizerTest01() {
		test(SRC_DATA, TokenizerClient.class, "test01", true);
	}
}

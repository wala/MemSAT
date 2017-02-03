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
package test.refactoring;

import static com.ibm.wala.memsat.util.Graphs.graph;
import static test.TestUtil.threadMethods;

import java.io.File;

import org.junit.Test;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

import data.refactoring.Bug10;
import data.refactoring.Bug11;
import data.refactoring.Bug11R;
import data.refactoring.BugInlineLocal;
import data.refactoring.BugInlineLocalR;
import test.ConcurrentTests;
import test.TestUtil;

/**
 * Runs the refactoring tests.
 * @author etorlak
 */
public abstract class RefactoringTests extends ConcurrentTests {
	private static final File REFACTORING_TESTS = new File("source/data/refactoring");
	
	/**
	 * Checks that the result produced by applying the given miniatur instance to the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Graph<MethodReference> methods, boolean sat) { 
		test(miniatur, REFACTORING_TESTS, methods, sat);
	}
	
	/**
	 * Checks that the result produced by applying the given miniatur instance to all 
	 * {@linkplain TestUtil#threadMethods(Class) thread methods } in the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Class<?> testCase, boolean sat) { 
		test(miniatur, graph(threadMethods(testCase)), sat);
	}

	@Test
	public final void testBug10() {
		test(miniatur(3), Bug10.class, false);	
		//test(miniatur(3), Bug10R.class, true);	
	}
	
	@Test
	public final void testBug11() {
		test(miniatur(7), Bug11.class, false);	
		test(miniatur(3), Bug11R.class, true);	
	}
	
	@Test
	public final void testBugInlineLocal() {
		test(miniatur(5), BugInlineLocal.class, false);	
		test(miniatur(5), BugInlineLocalR.class, true);	
	}
}

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
package test.linearizability;

import static com.ibm.wala.memsat.util.Graphs.graph;
import static test.TestUtil.threadMethods;

import java.io.File;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import test.ConcurrentTests;
import test.TestUtil;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.memsat.concurrent.memory.simple.SequentialConsistency;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

import data.linearizability.TreiberClient00;

/**
 * Executes linearizability tests using the {@linkplain SequentialConsistency} memory model.
 * @author etorlak
 */
@Ignore
public final class LinearizabilityTests extends ConcurrentTests {
	private static final File LINEARIZABILITY_TESTS = new File("source/data/linearizability");
	

	/**
	 * Checks that the result produced by applying the given miniatur instance to the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Graph<MethodReference> methods, boolean sat) { 
		test(miniatur, LINEARIZABILITY_TESTS, methods, sat);
	}
	
	/**
	 * Checks that the result produced by applying the given miniatur instance to all 
	 * {@linkplain TestUtil#threadMethods(Class) thread methods } in the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Class<?> testCase, boolean sat) { 
		test(miniatur, graph(threadMethods(testCase)), sat);
	}
	/**
	 * {@inheritDoc}
	 * @see test.ConcurrentTests#memoryModel(int, java.util.Set)
	 */
	@Override
	protected MemoryModel memoryModel(int maxSpeculations, Set<MethodReference> special) {
		return new SequentialConsistency();
	}
	
	@Test
	public final void test00() {
		test(miniatur(3), TreiberClient00.class, true);	
	}

}

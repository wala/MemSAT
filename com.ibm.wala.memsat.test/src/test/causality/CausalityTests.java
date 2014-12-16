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
package test.causality;

import static com.ibm.wala.memsat.util.Graphs.graph;
import static test.TestUtil.method;
import static test.TestUtil.threadMethods;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import test.ConcurrentTests;
import test.TestUtil;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.Graph;

import data.causality.Test00;
import data.causality.Test01;
import data.causality.Test02;
import data.causality.Test03;
import data.causality.Test04;
import data.causality.Test05;
import data.causality.Test06;
import data.causality.Test07;
import data.causality.Test08;
import data.causality.Test09;
import data.causality.Test10;
import data.causality.Test11;
import data.causality.Test12;
import data.causality.Test13;
import data.causality.Test14;
import data.causality.Test15;
import data.causality.Test16;
import data.causality.Test17;
import data.causality.Test18;
import data.causality.Test18a;
import data.causality.Test18b;
import data.causality.Test18c;
import data.causality.Test19;
import data.causality.Test20;
import data.causality.Test21;
import data.causality.Test22;
import data.causality.Test23;
import data.causality.Test24;
import data.causality.Test25;
import data.causality.Test26;

/**
 * Runs the causality tests.
 * @author etorlak
 */
public abstract class CausalityTests extends ConcurrentTests {
	private static final File CAUSALITY_TESTS = new File("source/data/causality");
	

	/**
	 * Checks that the result produced by applying the given miniatur instance to the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Graph<MethodReference> methods, boolean sat) { 
		test(miniatur, CAUSALITY_TESTS, methods, sat);
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
	public final void test00() {
		test(miniatur(3), Test00.class, true);	
	}
	
	@Test
	public final void test01() {
		test(miniatur(5), Test01.class, true);	
	}
	
	@Test
	public final void test02() {
		test(miniatur(5), Test02.class, true);	
	}
	
	@Test
	public final void test03() {
		test(miniatur(5), Test03.class, true);	
	}
	
	@Test
	public final void test04() {
		test(miniatur(5), Test04.class, false);	
	}
	
	@Test
	public final void test05() {
		test(miniatur(8), Test05.class, false);	
	}

	@Test
	public final void test06() {
		test(miniatur(5), Test06.class, true);	
	}
	
	@Test
	public final void test07() {
		test(miniatur(7), Test07.class, true);	
	}
	
	@Test
	public final void test08() {
		test(miniatur(6), Test08.class, true);	
	}
	
	@Test
	public final void test09() {
		test(miniatur(7), Test09.class, true);	
	}
	
	@Test
	public final void test10() {
		test(miniatur(8), Test10.class, false);	
	}
	
	@Test
	public final void test11() {
		test(miniatur(9), Test11.class, true);	
	}
	
	@Test
	public final void test12() {
		final Miniatur miniatur = miniatur(7);
		miniatur.options().setNumberOfIndexAtoms(2);
		test(miniatur, Test12.class, false);	
	}
	
	@Test
	public final void test13() {
		test(miniatur(5), Test13.class, false);	
	}
	
	@Test
	public final void test14() {
		final Miniatur miniatur = miniatur(10);
		miniatur.options().setloopUnrollDepth(1);
		test(miniatur, Test14.class, false);	
	}
	
	@Test
	public final void test15() {
		final Miniatur miniatur = miniatur(11);
		miniatur.options().setloopUnrollDepth(2);
		test(miniatur, Test15.class, false);	
	}
	
	@Test
	public final void test16() {
		test(miniatur(4), Test16.class, true);	
	}
	
	@Test
	public final void test17() {
		test(miniatur(7), Test17.class, true);	
	}
	
	@Test
	public final void test18() {
		test(miniatur(7), Test18.class, true);	
	}
	
	@Test
	public void test19() {
		try {
			final Graph<MethodReference> methods = graph(threadMethods(Test19.class));
			methods.addEdge(method(Test19.class,"thread3"), method(Test19.class,"thread1"));
			test(miniatur(5), methods, true);	
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void test20() {
		try {
			final Graph<MethodReference> methods = graph(threadMethods(Test20.class));
			methods.addEdge(method(Test20.class,"thread3"), method(Test20.class,"thread1"));
			test(miniatur(7), methods, true);	
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public final void test18a() {
		test(miniatur(8), Test18a.class, false);	
	}
	
	@Ignore
	@Test
	public final void test18b() {
		test(miniatur(8), Test18b.class, false);	
	}
	
	@Ignore
	@Test
	public final void test18c() {
		test(miniatur(8), Test18c.class, false);	
	}
	
	@Test
	public final void test21() {
		test(miniatur(7), Test21.class, true);	
	}
	
	@Test
	public final void test22() {
		test(miniatur(7), Test22.class, true);	
	}
	
	@Test
	public final void test23() {
		test(miniatur(6), Test23.class, true);	
	}
	
	@Test
	public final void test24() {
		test(miniatur(11), Test24.class, true);	
	}
	
	@Test
	public final void test25() {
		test(miniatur(3), Test25.class, true);	
	}
	
	
	@Test
	public final void test26() {
		test(miniatur(8), Test26.class, true);	
	}
}

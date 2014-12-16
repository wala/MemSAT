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
package test.nemos;

import static com.ibm.wala.memsat.util.Graphs.graph;
import static test.TestUtil.threadMethods;

import java.io.File;
import java.util.Set;

import org.junit.Test;

import test.ConcurrentTests;
import test.TestUtil;

import com.ibm.wala.memsat.Miniatur;

import data.nemos.Test01;
import data.nemos.Test02;
import data.nemos.Test03;
import data.nemos.Test04;
import data.nemos.Test05;
import data.nemos.Test06;
import data.nemos.Test07;
import data.nemos.Test08;
import data.nemos.Test09;
import data.nemos.Test10;
import data.nemos.Test11;
import data.nemos.Test12;
import data.nemos.Test13;
import data.nemos.Test14;
import data.nemos.Test15;

/**
 * Runs the nemos tests.
 * @author etorlak
 */
public abstract class NemosTests extends ConcurrentTests {
	private static final File NEMOS_TESTS = new File("source/data/nemos");
	private final Set<String> sat;
	
	/**
	 * Constructs a NemosTests driver with the given set of satisfiable tests.
	 * In particular, the test with name "test*" will assert that its litmus program
	 * has an execution iff the given set contains the string "*".  Otherwise, 
	 * it will assert that the litmus program has no executions
	 * @param sat
	 */
	protected NemosTests(Set<String> sat) { this.sat = sat; }
	
	
	/**
	 * Checks that the result produced by applying the given miniatur instance to all 
	 * {@linkplain TestUtil#threadMethods(Class) thread methods } in the given
	 * methods is satisfiable or unsatisfiable, depending on the value of the specified flag.
	 */
	final void test(Miniatur miniatur, Class<?> testCase, boolean sat) { 
		test(miniatur, NEMOS_TESTS, graph(threadMethods("p", testCase)), sat);
	}

	private final boolean sat(String test) { return sat.contains(test); }
	
	@Test
	public final void test01() {
		test(miniatur(3), Test01.class, sat("01"));	
	}
	
	@Test
	public final void test02() {
		test(miniatur(5), Test02.class, sat("02"));	
	}
	
	@Test
	public final void test03() {
		test(miniatur(17), Test03.class, sat("03"));	
	}
	
	@Test
	public final void test04() {
		test(miniatur(7), Test04.class, sat("04"));	
	}
	
	@Test
	public final void test05() {
		test(miniatur(6), Test05.class, sat("05"));	
	}
	
	@Test
	public final void test06() {
		test(miniatur(5), Test06.class, sat("06"));	
	}

	@Test
	public final void test07() {
		test(miniatur(9), Test07.class, sat("07"));	
	}
	
	@Test
	public final void test08() {
		test(miniatur(7), Test08.class, sat("08"));	
	}
	
	@Test
	public final void test09() {
		test(miniatur(11), Test09.class, sat("09"));	
	}

	@Test
	public final void test10() {
		test(miniatur(5), Test10.class, sat("10"));	
	}
	
	@Test
	public void test11() {
		test(miniatur(5), Test11.class, sat("11"));	
	}
	
	@Test
	public final void test12() {
		test(miniatur(6), Test12.class, sat("12"));	
	}
	
	@Test
	public void test13() {
		test(miniatur(7), Test13.class, sat("13"));	
	}
	
	@Test
	public void test14() {
		test(miniatur(7), Test14.class, sat("14"));	
	}
	
	@Test
	public void test15() {
		test(miniatur(9), Test15.class, sat("15"));	
	}
}

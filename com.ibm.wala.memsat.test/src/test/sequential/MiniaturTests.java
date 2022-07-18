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
package test.sequential;

import static test.TestUtil.method;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.memsat.Miniatur;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.Results;
import com.ibm.wala.memsat.translation.sequential.SequentialTranslation;
import com.ibm.wala.util.CancelException;

import data.little.Little;
import kodkod.ast.Relation;
import kodkod.engine.Evaluator;
import kodkod.engine.Solution;
import kodkod.engine.Solution.Outcome;
import kodkod.instance.Instance;

public class MiniaturTests {
	private static final File SRC_DATA_LITTLE = new File("source/data/little");
	private Miniatur miniatur;
	
	protected Options getOptions() {
		return new Options();
	}
	
	@Before public void setup() {
		miniatur = new Miniatur(getOptions());
	}

	static Solution test(Miniatur miniatur, File srcpath, Class<?> klass, String methodname, boolean sat){
		return test(miniatur, Collections.singletonList(srcpath), klass, methodname, sat);
	}
	
	static Solution test(Miniatur miniatur, List<File> srcpath, Class<?> klass, String methodname, boolean sat){
		try {
			
			Results<SequentialTranslation> results = miniatur.analyze(method(klass, methodname), srcpath);
			Solution solution = results.solution();
			
//			System.out.println(results.translation().toString());		
			System.out.println(solution.toString());		
			
			if (sat) {
				assert solution.outcome().equals(Outcome.SATISFIABLE) ||
				solution.outcome().equals(Outcome.TRIVIALLY_SATISFIABLE);
			} else {
				
				assert solution.outcome().equals(Outcome.UNSATISFIABLE) ||
				solution.outcome().equals(Outcome.TRIVIALLY_UNSATISFIABLE);
			}	
			return solution;
		} catch (SecurityException | CancelException | IOException | OutOfMemoryError | NoSuchMethodException e) {
			e.printStackTrace();
			Assert.assertTrue(e.getMessage(), false);
		}
		return null;
	}
		
	@Test
	public void testFloats(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloats", true);
	}

	@Test
	public void testFloats2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloats2", true);
	}

	@Test
	public void testFloatsRound(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloatsRound", false);
	}

	@Test
	public void testFloatsRound2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloatsRound2", true);
	}

	@Test
	public void testIntsRound(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testIntsRound", false);
	}

	@Test
	public void testFloatsNot(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloatsNot", false);
	}

	@Test
	public void testFloatBox(){
		miniatur.options().kodkodOptions().setBitwidth(32);
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloatBox", true);
	}

	@Test
	public void testFloatBoxNot(){
		miniatur.options().kodkodOptions().setBitwidth(32);
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFloatBoxNot", false);
	}

	@Test
	public void testFieldAccess(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFieldAccess", true);
	}

	@Test
	public void testMyLinkedList1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyLinkedList1", false);
	}
	
	@Test
	public void testMyLinkedList2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyLinkedList2", true);
	}
	
	@Test
	public void testMyLinkedList3(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyLinkedList3", true);
	}
	
	@Test
	public void testEquals(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testEquals", false);
	}
	
	@Test
	public void testEquals1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testEquals1", false);
	}

	@Test
	public void testEquals2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testEquals2", false);
	}

	@Test
	public void testIrrelevant(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testIrrelevant", false);
	}

	@Test
	public void testObjectHashCodeCall(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testObjectHashCodeCall", true);
	}

	@Test
	public void testObjectHashCodeCall1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testObjectHashCodeCall1", true);
	}

	@Test
	public void testObjectHashCodeCall2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testObjectHashCodeCall2", true);
	}

	@Test
	public void testConstructorSubclass(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testConstructorSubclass", false);
	}


	@Test
	public void testCell1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testCell1", false);
	}

	@Test
	public void testCell2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testCell2", true);
	}

	@Test
	public void testIntFields0(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testIntFields0", false);
	}

	@Test
	public void testIntFields1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testIntFields1", false);
	}

	@Test
	public void testIntFields2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testIntFields2", true);
	}

	@Test
	public void testMyArrayList1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList1", false);
	}

	@Test
	public void testMyArrayList2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList2", true);
	}

	@Test
	public void testMyArrayList3(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList3", false);
	}

	@Test
	public void testMyArrayList4(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList4", false);
	}

	@Test
	public void testMyArrayList5(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList5", false);
	}

	@Test
	public void testMyArrayList6(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList6", false);
	}

	@Test
	public void testMyArrayList7(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testMyArrayList7", false);
	}

	@Test
	public void testFields(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFields", false);
	}

	@Test
	public void testArrayWrite1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite1", false);
	}

	@Test
	public void testArrayWrite2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite2", true);
	}

	@Test
	public void testArrayWrite3(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite3", false);
	}

	@Test
	public void testArrayWrite4(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite4", true);
	}

	@Test
	public void testArrayWrite5(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite5", true);
	}

	@Test
	public void testArrayWrite6(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite6", false);
	}

	@Test
	public void testArrayWrite7(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite7", true);
	}

	@Test
	public void testArrayWrite8(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite8", false);
	}

	@Test
	public void testArrayWrite9(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite9", true);
	}

	@Test
	public void testArrayWrite10(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite10", true);
	}

	@Test
	public void testArrayWrite11(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayWrite11", true);
	}

	@Test
	public void testArrayCopy1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayCopy1", false);
	}

	@Test
	public void testArrayAssign(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayAssign", false);
	}

	@Test
	public void testArrayParam(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayParam", false);
	}

	@Test
	public void testArrayParam1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayParam1", true);
	}

	@Test
	public void testArrayParam2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayParam2", false);
	}


	@Test
	public void testArrayParam3(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayParam3", false);
	}

	@Test
	public void testArrayParam4(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayParam4", true);
	}

	@Test
	public void testFieldParam(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testFieldParam", false);
	}

	@Test
	public void testArrayCopy2(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArrayCopy2", false);
	}

	@Test
	public void testNestedIfs(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testNestedIfs", true);
	}

	@Test
	public void testStatic() {
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testStatic", true);
	}

	@Test
	public void testStaticBoolean(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testStaticBoolean", false);
	}

	@Test
	public void testStaticInt(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testStaticInt", false);
	}

	@Test
	public void testStaticInt0(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testStaticInt0", false);
	}

	@Test
	public void testStaticObject(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testStaticObject", false);
	}

	@Test
	public void testArray() {
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArray", false);
	}

	@Test
	public void testArray1() {
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testArray1", true);
	}

	@Test
	public void testInts() {
		Solution sol = test(miniatur, SRC_DATA_LITTLE, Little.class, "testInts", true);
		Instance instance = sol.instance();
		Evaluator evaluator = new Evaluator(instance, miniatur.options().kodkodOptions());
		int x = 0;
		int y = 0;
		int z = 0;
		for(Relation R: instance.relations()){
			if (R.name().equals("testInts_arg1"))
				x = evaluator.evaluate(R.sum());
			else if (R.name().equals("testInts_arg2"))
				y = evaluator.evaluate(R.sum());
			else if (R.name().equals("testInts_arg3"))
				z = evaluator.evaluate(R.sum());
		}
		System.out.println("x = " + x + " y = " + y + " z = " + z);
		assert x > y && y > z;
	}

	@Test
	public void testLoop() {
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testLoop", false);
	}

	@Test
	public void testLoop1(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testLoop1", true);
	}


	@Test
	public void testList(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testList", false);
	}

	@Test
	public void testCell(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testCell", false);
	}

	@Test
	public void testListRemoveOther(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testListRemoveOther", false);
	}

	@Test
	public void testListRemoveFirst(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testListRemoveFirst", false);
	}

	@Test
	public void testListRemoveCounter(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testListRemoveCounter", true);
	}

	@Test
	public void testSimpleFields(){
		test(miniatur, SRC_DATA_LITTLE, Little.class, "testSimpleFields", false);
	}
	
}

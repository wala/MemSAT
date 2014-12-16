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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.memsat.concurrent.memory.simple.CausalConsistency;
import com.ibm.wala.types.MethodReference;

/**
 * Executes nemos tests using the {@linkplain CausalConsistency} memory model.
 * @author etorlak
 */
public final class CausalConsistencyNemosTests extends NemosTests {

	public CausalConsistencyNemosTests() {
		super(
			new HashSet<String>(
				Arrays.asList("01", "03", "04", "06", "07", "08", "09")
			)
		);
	}
	
	/**
	 * {@inheritDoc}
	 * @see test.ConcurrentTests#memoryModel(int, java.util.Set)
	 */
	@Override
	protected MemoryModel memoryModel(int maxSpeculations,
			Set<MethodReference> special) {
		return new CausalConsistency();
	}

	@Test(expected = AssertionError.class)
	public final void test11() {
		super.test11();
	}

	@Test(expected = AssertionError.class)
	public final void test13() {
		super.test13();
	}

	@Test(expected = AssertionError.class)
	public final void test14() {
		super.test14();
	}
	
	@Test(expected = AssertionError.class)
	public final void test15() {
		super.test15();
	}
}

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
package test.refactoring;

import java.util.Set;

import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.memsat.concurrent.memory.jmm.JMMAlt;
import com.ibm.wala.types.MethodReference;

/**
 * Executes refactoring tests using the {@linkplain JMMAlt} memory model.
 * @author etorlak
 */
public final class JMMAltRefactoringTests extends RefactoringTests {

	@Override
	protected MemoryModel memoryModel(int maxSpeculations, Set<MethodReference> special) {
		return new JMMAlt(maxSpeculations, special);
	}

}

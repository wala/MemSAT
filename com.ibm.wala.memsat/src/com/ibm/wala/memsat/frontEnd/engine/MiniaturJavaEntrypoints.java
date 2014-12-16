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
package com.ibm.wala.memsat.frontEnd.engine;

import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class MiniaturJavaEntrypoints implements Iterable<Entrypoint> {

	private final List<MethodReference> methods;

	private final IClassHierarchy cha;


	public MiniaturJavaEntrypoints(List<MethodReference> methods, IClassHierarchy cha) {
		super();
		this.methods = methods;
		this.cha = cha;
	}

	public Iterator<Entrypoint> iterator() {

		return new Iterator<Entrypoint>() {
			private int index = 0;
			private final int subtypesBound = 10;

			public void remove() {
				Assertions.UNREACHABLE();
			}

			public boolean hasNext() {
				return index < methods.size();
			}

			public Entrypoint next() {
			  return
				new SubtypesEntrypoint(methods.get(index++), cha)
				{
					protected TypeReference[] makeParameterTypes(IMethod method, int i) 
					{
						TypeReference[] all = super.makeParameterTypes(method, i);
						if (all.length > subtypesBound) {
							TypeReference[] some = new TypeReference[ subtypesBound ];
							for(int j = 0; j < subtypesBound; j++) {
								some[j] = all[j];
							}

							return some;
						} else {
							return all;
						}
					}
				};
			}
		};
	}
}

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
package data.angelic;


import data.angelic.Tokenizer.Token;

public final class TokenizerClient {


	/** Failing test case due to the bug seeded in {@linkplain Tokenizer#token(Object)} */
	public static void test00() {
		
		final Tokenizer tokenizer = new Tokenizer();
		final LinkedList objs = new LinkedList();
		objs.add(new Object());
		objs.add(tokenizer.plus);
		objs.add(new Object());
		
		while(objs.size()>0) { 
			final Object obj = objs.removeFirst();
			final Token token = tokenizer.token(obj);
			final Object kind = token.kind;
			if (kind==tokenizer.operator) { 
				final Object nextObj = objs.removeFirst();
				final Token nextToken = tokenizer.token(nextObj);
				assert nextToken.tokenized == nextObj;
				assert nextToken.kind == tokenizer.operand;
			}
		}
		
	}
	
	/** 
	 * Passing test case, since the bug seeded in {@linkplain Tokenizer#token(Object)} is bypassed through the use of an angelic value.
	 * Note that this forces all iterations of the loop to use the same angelic value, but it's ok in this case since only one iteration
	 * is relevant. 
	 **/
	public static void test01(Token angelic) {
		assert angelic != null;
		
		final Tokenizer tokenizer = new Tokenizer();
		final LinkedList objs = new LinkedList();
		objs.add(new Object());
		objs.add(tokenizer.plus);
		objs.add(new Object());

		while(objs.size()>0) { 
			final Object obj = objs.removeFirst();
			final Token token = tokenizer.token(obj);
			final Object kind = token.kind;
			if (kind==tokenizer.operator) { 
				final Object nextObj = objs.removeFirst();
				final Token nextToken = angelic;
				assert nextToken.tokenized == nextObj;
				assert nextToken.kind == tokenizer.operand;
			}
		}
	
	}

	public static void test02() {
		final Tokenizer tokenizer = new Tokenizer();
		final LinkedList objs = new LinkedList();
		objs.add(new Object());
		objs.add(tokenizer.plus);
		objs.add(new Object());
		
		int blockBalance = 0;
		while(objs.size()>0) { 
			final Token token = tokenizer.token(objs.removeFirst());
			final Object kind = token.kind;
			if (token==tokenizer.open) {
				blockBalance++;
			} else if (token==tokenizer.close) {
				blockBalance--;
			} else if (kind==tokenizer.operator) { 
				assert objs.size()>0;
				assert tokenizer.token(objs.removeFirst()).kind == tokenizer.operand;
			}
		}
		assert blockBalance==0;
	}
	
	
}

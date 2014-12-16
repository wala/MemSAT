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
package data.angelic;


final class Tokenizer {

	final Object open, close, plus;
	final Object delimiter, operator, operand;
	
	Tokenizer() {
		this.open = new Object();
		this.close = new Object();
		this.plus = new Object();
		this.delimiter = new Object();
		this.operator = new Object();
		this.operand = new Object();
	}
	
	Token token(Object tokenizable) { 
		if (tokenizable==open || tokenizable==close)
			return new Token(tokenizable, delimiter);
		else if (tokenizable==plus)
			return new Token(tokenizable, operator);
		else 
			return new Token(tokenizable, tokenizable); //bug, should be "operand"
	}
		
	static final class Token {
		final Object tokenized;
		final Object kind;
		
		private Token(Object tokenized, Object kind) { 
			this.tokenized = tokenized;
			this.kind = kind;
		}
	}
}

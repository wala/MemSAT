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




/**
 * @author etorlak
 *
 */
public final class Angelic {
	
	static final class Entry { Entry next, f; }
	
	
	public static final void test00() {
		// prestate
		final Entry x = new Entry(); // OBJ0
		x.next = new Entry(); // OBJ1
		x.f = new Entry(); // OBJ2
		x.next.f = null;
		// end of prestate
		final Entry y = x; // but it should have been x.next or x.f
		assert (y.f == null);
	}
	
	public static final void test01(Entry angelicChoice) {
		// prestate
		final Entry x = new Entry(); // OBJ0
		x.next = new Entry(); // OBJ1
		x.f = new Entry(); // OBJ2
		x.next.f = null;
		// end of prestate
		final Entry y = angelicChoice; // angelically chosen
		assert (y.f == null);
	}

	
	public static final void test02() {
		// prestate
		final Entry x = new Entry(); // OBJ0
		x.next = new Entry(); // OBJ1
		x.f = new Entry(); // OBJ2
		x.next.f = null;
		// end of prestate
		final Entry y = (Entry)NonDetChoice.chooseObject(); // angelically chosen
		assert (y.f == null);
	}

}

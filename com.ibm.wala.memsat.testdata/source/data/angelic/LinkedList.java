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


public class LinkedList {
	private Entry header;
	private int size;

	/**
	 * Constructs an empty list.
	 */
	public LinkedList() {
		header = new Entry(null, null, null);
		header.next = header.previous = header;
		size = 0;
	}

	public Object getFirst() {
		return header.next.element;
	}

	public Object getLast() {
		return header.previous.element;
	}

	public Object removeFirst() {
		Object first = header.next.element;
		remove(header.next);
		return first;
	}

	public Object removeLast() {
		Object last = header.previous.element;
		remove(header.previous);
		return last;
	}

	public void addFirst(Object o) {
		addBefore(o, header.next);
	}

	public void addLast(Object o) {
		addBefore(o, header);
	}

	public int size() {
		return size;
	}

	public void add(Object o) {
		addBefore(o, header);
	}
	
	private Entry addBefore(Object o, Entry e) {
		Entry newEntry = new Entry(o, e, e.previous);
		newEntry.previous.next = newEntry;
		newEntry.next.previous = newEntry;
		size++;
		return newEntry;
	}

	private void remove(Entry e) {
		e.previous.next = e.next;
		e.next.previous = e.previous;
		size--;
	}

	private static class Entry {
		Object element;
		Entry next;
		Entry previous;

		Entry(Object element, Entry next, Entry previous) {
			this.element = element;
			this.next = next;
			this.previous = previous;
		}
	}
	
}

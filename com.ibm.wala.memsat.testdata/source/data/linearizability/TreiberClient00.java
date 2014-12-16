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
package data.linearizability;

public final class TreiberClient00 {

	private static final Object item = new Object();
	
	public static final void thread1() {
		final Object i = item;
		final Object o = pop();
//		assert o==null;
		assert o==i;
	}
	
	public static final void thread2() {
		final Object i = item;
		push(i);
		assert true;
	}
	
	private static class Lock {
		
	}
	
	private final static Object lock = new Lock();
	static Node head = null;
	 
	public static Object pop() {
		Node oldHead, newHead;
		do {
			oldHead = head;
			if (oldHead==null)
				return null;
			newHead = oldHead.next;
		} while (!casHead(oldHead, newHead));
		return oldHead.item;
	}
	
	public static void push(Object item) { 
		Node oldHead;
		Node newHead = new Node(item);
		do {
			oldHead = head;
			newHead.next = oldHead;
		} while (!casHead(oldHead, newHead));
	}
	
	private static boolean casHead(Node oldHead, Node newHead) { 
		synchronized(lock) { 
			final Node curHead = head;
			if (curHead==oldHead) {
				head = newHead;
				return true;
			} else {
				return false;
			}
		}
	}
	
	private static class Node {
		final Object item;
		Node next;
		
		Node(Object item) {
			this.item = item;
			this.next = null;
		}
	}
	
}

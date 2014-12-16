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
package data.linearizability;

/**
 * A simple implementation of Treiber's stack algorithm.
 * @author etorlak
 */
public final class TreiberStack {

	private Node head;
	
	public TreiberStack() { 
		this.head = null; 
	}
	
	public Object pop() {
		Node oldHead, newHead;
		do {
			oldHead = this.head;
			if (oldHead==null)
				return null;
			newHead = oldHead.next;
		} while (!casHead(oldHead, newHead));
		return oldHead.item;
	}
	
	public void push(Object item) { 
		Node oldHead;
		Node newHead = new Node(item);
		do {
			oldHead = this.head;
			newHead.next = oldHead;
		} while (!casHead(oldHead, newHead));
	}
	
	private boolean casHead(Node oldHead, Node newHead) { 
		synchronized(this) { 
			final Node curHead = this.head;
			if (curHead==oldHead) {
				this.head = newHead;
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

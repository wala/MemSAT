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
package data.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A Java implementation of the non-blocking concurrent list by Harris.
 * For simplicity, the implementation accepts integer keys.
 * @author Emina Torlak
 */
public final class HarrisList {

	private final Node head, tail;
	
	/**
	 * Constructs an empty Harris list.
	 */
	public HarrisList() { 
		head = new Node(Short.MIN_VALUE);
		tail = new Node(Short.MAX_VALUE);
		head.next.set(tail);
	}
	
	/**
	 * Adds the given key to this list if not already present.
	 * Returns true if the list was modified as a result of 
	 * this operation; otherwise returns false.
	 * @return true if the list was modified as a result of 
	 * this operation; otherwise returns false.
	 */
	public boolean insert(int key) { 
		Node newNode = new Node(key);
		Node rightNode, leftNode;
		
		do {
			Pair pair = search(key);
			leftNode = pair.fst;
			rightNode = pair.snd;
			
			if (rightNode != tail && rightNode.key==key) {
				return false;
			}
			newNode.next = new AtomicReference/*<Node>*/(rightNode);
			if (leftNode.next.compareAndSet(rightNode, newNode))
				return true;
		} while(true);
	}
	
	/**
	 * Removes the given key from this list, if present.
	 * Returns true if the list was modified as a result of 
	 * this operation; otherwise returns false.
	 * @return true if the list was modified as a result of 
	 * this operation; otherwise returns false.
	 */
	public boolean delete(int key) { 
		Node rightNode, rightNodeNext, leftNode;
		do {
			Pair pair = search(key);
			leftNode = pair.fst;
			rightNode = pair.snd;
			
			if (rightNode==tail || rightNode.key!=key) { 
				return false;
			}
			
			rightNodeNext = rightNode.next();
			if (!rightNode.isMarked()) { 
				if (rightNode.isMarked.compareAndSet(false, true)) { 
					break;
				}
			}
			
		} while(true);
		
		if (!leftNode.next.compareAndSet(rightNode, rightNodeNext)) { 
			search(rightNode.key);
		}
		
		return true;
	}
	
	/**
	 * Returns a pair of adjacent unmarked nodes
	 * such that the key of the left node is less 
	 * than searchKey and the key of the right node
	 * is greater than or equal to searchKey.
	 * @return a pair of adjacent unmarked nodes
	 * such that the key of the left node is less 
	 * than searchKey and the key of the right node
	 * is greater than or equal to searchKey.
	 */
	private Pair/*<Node, Node>*/ search(int searchKey) {
		Node leftNode = null, leftNodeNext = null, rightNode = null;
		
		search_again:
			do {
				Node t = head;
				Node tNext = head.next();
				
				do {
					if (!t.isMarked.get()) { 
						leftNode = t;
						leftNodeNext = t.next();
					}
					t = tNext;
					if (t==tail) break;
					tNext = t.next();
				} while (t.isMarked() || t.key < searchKey);
				rightNode = t;
				
				if (leftNodeNext == rightNode) { 
					if (rightNode != tail && rightNode.isMarked())
						continue search_again;
					else 
						return new Pair(leftNode,rightNode);
				}
				
				if (leftNode.next.compareAndSet(leftNodeNext, rightNode)) { 
					if (rightNode != tail && rightNode.isMarked())
						continue search_again;
					else 
						return new Pair(leftNode,rightNode);
				}
			} while (true);
	}
	
	
	/**
	 * Node in the linked list.
	 */
	private static final class Node { 
		int key;
		AtomicReference/*<Node>*/ next;
		AtomicBoolean isMarked;
		
		Node(int key) { 
			this.key = key;
			this.next = new AtomicReference/*<Node>*/();
			this.isMarked = new AtomicBoolean(false);
		}
		
		boolean isMarked() { return isMarked.get(); }
		
		Node next() { return (Node)next.get(); }
	}
	
	/**
	 * A pair of nodes.
	 * @author Emina Torlak
	 */
	private static final class Pair {
		final Node fst;
		final Node snd;
		
		Pair(Node fst, Node snd) { 
			this.fst = fst;
			this.snd = snd;
		}
	}
}

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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of Heller's lazy set algorithm.
 * 
 * @author Emina Torlak
 */
public final class HellerSet {
	private final Entry head;
	private final Entry tail;
	
	/**
	 * Constructs a new empty Heller set.
	 */
	public HellerSet() {
		this.head = new Entry(Integer.MIN_VALUE);
		this.tail = new Entry(Integer.MAX_VALUE);
		this.head.next = tail;
	}
	
	/**
	 * Returns true if this set contains the given key.
	 * @return true if this set contains the given key.
	 */
	public boolean contains(int key) { 
		Entry curr = this.head;
		while(curr.key < key) { 
			curr = curr.next;
		}
		return curr.key==key && !curr.marked;
	}
	
	private boolean validate(Entry pred, Entry curr) { 
		return !pred.marked && !curr.marked && pred.next == curr;
	}
	
	/**
	 * Adds the given key to this set if not already present.
	 * Returns true if the set was modified as a result of 
	 * this operation; otherwise returns false.
	 * @return true if the set was modified as a result of 
	 * this operation; otherwise returns false.
	 */
	public boolean add(int key) { 
		while(true) { 
			Entry pred = this.head;
			Entry curr = head.next;
			while(curr.key < key) { 
				pred = curr; 
				curr = curr.next;
			}
			pred.lock();
			try {
				curr.lock();
				try {
					if(validate(pred,curr)) { 
						if (curr.key==key) { 
							return false;
						} else {
							Entry entry = new Entry(key);
							entry.next = curr;
							pred.next = entry;
							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}
	
	/**
	 * Removes the given key from this set, if present.
	 * Returns true if the set was modified as a result of 
	 * this operation; otherwise returns false.
	 * @return true if the set was modified as a result of 
	 * this operation; otherwise returns false.
	 */
	public boolean remove(int key) { 
		while(true) { 
			Entry pred = this.head;
			Entry curr = head.next;
			while(curr.key < key) { 
				pred = curr;
				curr = curr.next;
			}
			pred.lock();
			try {
				curr.lock();
				try {
					if (validate(pred,curr)) { 
						if (curr.key != key) { 
							return false;
						} else {
							curr.marked = true;
							pred.next = curr.next;
							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}
	
	private static class Entry {
		int key;
		Entry next;
		boolean marked;
		Lock lock;
		
		Entry(int key) { 
			this.key = key; 
			this.lock = new ReentrantLock();
			this.marked = false;
		}
		
		void lock() { this.lock.lock(); }
		void unlock() { this.lock.unlock(); }
	}
	
}

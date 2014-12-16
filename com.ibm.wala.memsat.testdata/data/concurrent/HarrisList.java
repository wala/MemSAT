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
package data.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final public class HarrisList {
    final private Node head;
    final private Node tail;
    
    public HarrisList() {
        super();
        head = new Node(Short.MIN_VALUE);
        tail = new Node(Short.MAX_VALUE);
        head.next.set(tail);
    }
    
    public boolean insert(int key) {
        Node newNode = new Node(key);
        Node rightNode;
        Node leftNode;
        do  {
            Pair pair = this.search(key);
            leftNode = pair.fst;
            rightNode = pair.snd;
            if (rightNode != tail && rightNode.key == key) { return false; }
            newNode.next = new AtomicReference(rightNode);
            if (leftNode.next.compareAndSet(rightNode, newNode)) return true;
        }while(true); 
    }
    
    public boolean delete(int key) {
        Node rightNode;
        Node rightNodeNext;
        Node leftNode;
        do  {
            Pair pair = this.search(key);
            leftNode = pair.fst;
            rightNode = pair.snd;
            if (rightNode == tail || rightNode.key != key) { return false; }
            rightNodeNext = rightNode.next();
            if (!rightNode.isMarked()) {
                if (rightNode.isMarked.compareAndSet(false, true)) { break; }
            }
        }while(true); 
        if (!leftNode.next.compareAndSet(rightNode, rightNodeNext)) {
            this.search(rightNode.key);
        }
        return true;
    }
    
    private Pair search(int searchKey) {
        Node leftNode = null;
        Node leftNodeNext = null;
        Node rightNode = null;
        search_again: do  {
            Node t = head;
            Node tNext = head.next();
            do  {
                if (!t.isMarked.get()) {
                    leftNode = t;
                    leftNodeNext = t.next();
                }
                t = tNext;
                if (t == tail) break;
                tNext = t.next();
            }while(t.isMarked() || t.key < searchKey); 
            rightNode = t;
            if (leftNodeNext == rightNode) {
                if (rightNode != tail && rightNode.isMarked())
                    continue search_again;
                else return new Pair(leftNode, rightNode);
            }
            if (leftNode.next.compareAndSet(leftNodeNext, rightNode)) {
                if (rightNode != tail && rightNode.isMarked())
                    continue search_again;
                else return new Pair(leftNode, rightNode);
            }
        }while(true); 
    }
    
    final private static class Node {
        int key;
        AtomicReference next;
        AtomicBoolean isMarked;
        
        Node(int key) {
            super();
            this.key = key;
            this.next = new AtomicReference();
            this.isMarked = new AtomicBoolean(false);
        }
        
        boolean isMarked() { return isMarked.get(); }
        
        Node next() { return (Node) next.get(); }
    }
    
    final private static class Pair {
        final Node fst;
        final Node snd;
        
        Pair(Node fst, Node snd) {
            super();
            this.fst = fst;
            this.snd = snd;
        }
    }
    
}

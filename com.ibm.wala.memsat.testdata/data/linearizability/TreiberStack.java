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

final public class TreiberStack {
    private Node head;
    
    public TreiberStack() {
        super();
        this.head = null;
    }
    
    public Object pop() {
        Node oldHead;
        Node newHead;
        do  {
            oldHead = this.head;
            if (oldHead == null) return null;
            newHead = oldHead.next;
        }while(!this.casHead(oldHead, newHead)); 
        return oldHead.item;
    }
    
    public void push(Object item) {
        Node oldHead;
        Node newHead = new Node(item);
        do  {
            oldHead = this.head;
            newHead.next = oldHead;
        }while(!this.casHead(oldHead, newHead)); 
    }
    
    private boolean casHead(Node oldHead, Node newHead) {
        synchronized (this)  {
            final Node curHead = this.head;
            if (curHead == oldHead) {
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
            super();
            this.item = item;
            this.next = null;
        }
    }
    
}

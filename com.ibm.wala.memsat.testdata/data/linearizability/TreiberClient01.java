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

final public class TreiberClient01 {
    final static TreiberStack stack = new TreiberStack();
    
    final public static void thread1() {
        final TreiberStack s = stack;
        final Object o2 = s.pop();
        assert o2 == null;
    }
    
    public TreiberClient01() { super(); }
}

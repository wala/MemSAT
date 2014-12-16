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

final public class Basics {
    int i = 0;
    int j = 0;
    
    public void inc() {
        int old = i;
        i = i + 1;
        assert i > old;
    }
    
    public void dec() {
        int old = i;
        i = i - 1;
        assert i < old;
    }
    
    public Basics() { super(); }
}

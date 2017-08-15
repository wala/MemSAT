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
package data.little;
public class Value implements Comparable<Value> {
  final int myVal;
  final int herVal;
	
  public Value(int myVal, int herVal){
    this.myVal = myVal;
    this.herVal = herVal;
  }
	
  public boolean equals(Object o){
    return (o instanceof Value) &&
	   myVal == ((Value)o).myVal &&
	   herVal == ((Value)o).herVal;
  }
	
  public int hashCode(){
    return myVal + (5*herVal); 
  }

  public int compareTo(Value o){
    if (myVal != o.myVal) {
      return myVal - o.myVal;
    } else {
      return herVal - o.herVal;
    }
  }
}

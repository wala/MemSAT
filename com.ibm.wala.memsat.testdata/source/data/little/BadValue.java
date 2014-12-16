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
public class BadValue extends Value {
  int hisVal;
	
  public BadValue(int myVal, int herVal, int hisVal){
    super(myVal, herVal);
    this.hisVal = hisVal;
  }
	
  public boolean equals(Object o){
    return super.equals(o) && 
	   (o instanceof BadValue) &&
	   hisVal == ((BadValue)o).hisVal;
  }
	
  public int hashCode(){
    return super.hashCode() + hisVal<<12;
  }

}

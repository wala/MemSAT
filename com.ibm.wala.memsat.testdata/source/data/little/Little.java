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

class NullKey {
    NullKey() {

    }
}

public class Little {
    static int foo;
    int[] a = new int[3];

	/**
	 * @param args
	 */

    public void testFloats(float x, float y, float z) {
    	assert !(x > y && y > z) ;
    }

    public void testFloats2(float x, float y, float z) {
    	assert !(x == y + 1.1f || y == z + 1.1f || x == z + 2.2f) ;
    }

    public void testIsNaN(float x) {
    	assert Float.isNaN(x) ;
    }
    
    public void testIsNotNaN(float x) {
    	assert !Float.isNaN(x) ;
    }
    
    public void testIsNotNaN2(float x) {
    	assert x<=0f ;
    }
    
    public void testFloatsRound(float x, float y) {
    	if (!isSpecial(x) && !isSpecial(y))
    		assert x + y == y + x ;
    }

    public void testFloatsRound2(float x, float y, float z) {
    	if (!isSpecial(x) && !isSpecial(y) && !isSpecial(z)) {
    		assert x + y + z == z + y + x ;
    	}
    }

    public void testFloatsRound3(float x, float y, float z) {
    	if (!isSpecial(x) && !isSpecial(y) && !isSpecial(z)) {
    		assert x - y - z == z - y - x ;
    	}
    }

    public void testFloatsRound4(float x, float y) {
    	assert x + y == y + x ;
    }

    public void testFloatsRound5(float x, float y, float z) {
    	if (!isSpecial(x) && !isSpecial(y) && !isSpecial(z)) {
    		float value_1 = x + y + z;
			float value_2 = z + y + x;
			assert value_1 != value_2;
    	}
    }

    public void testIntsRound(int x, int y, int z) {
    	assert x + y + z == z + y + x ;
    }

    public void testFloatsNot(float x, float y, float z) {
    	assert !(x > y && y > z && z > x) ;
    }
 
    public void testFloatBox() {
    	FloatCell fb = new FloatCell(4.0f);
    	float f = fb.value;
    	assert 5.0f == f - 1.0 ;
    }

    public void testFloatBoxNot() {
    	FloatCell fb = new FloatCell(4.0f);
    	float f = fb.value;
    	assert 5.0f == f + 1.0f ;
    }

	public void testFields() {
		List l = new List(1);
		l.next = new List(2);
		assert l.value == 1 ;	
	}

	public void testFields3(int a) {
		List l = new List(1);
		l.next = new List(a);
		assert l.next.value == 1 ;	
	}

	public void testFields2() {
		List l = new List(1);
		l.next = new List(2);
		assert l.next.value != 1;
	}
	
	public void testArrayWrite1(){
		int[] arr = new int[3];
		arr[1] = 1;
		//assert (arr.length == 1) && (arr[1]==1);
		assert arr[1] == 1 ;
		//assert (arr.length ==3);
	}
	
	public void testArrayWrite2(){
		int[] arr = new int[3];
		arr[1] = 1;
		//assert (arr.length == 1) && (arr[1]==1);
		assert arr[1] == 2 ;
		//assert (arr.length ==3);
	}
	
	public void testArrayWrite3(){
		int[] arr = new int[3];
		arr[1] = 1;
		assert arr.length == 3 ;
	}
	
	public void testArrayWrite4(){
		int[] arr = new int[3];
		arr[1] = 1;
		assert arr.length == 7 ;
	}
	
	public void testArrayWrite5(){
		int[] arr = new int[3];
		arr[1] = 1;
	    assert arr.length == 1 && arr[1]==1 ;
	}
	
	public void testArrayWrite6(){
		int[] arr = new int[5];
		arr[1] = 1;
		arr[2] = arr[1];
		arr[3] = 2;
		assert arr[1] == 1 && arr[2] == 1 && arr[3] == 2 ;
	}
	
	public void testArrayWrite7(){
		int[] arr = new int[5];
		arr[1] = 1;
		arr[2] = arr[1];
		arr[3] = 2;
		assert arr[1] == 10 && arr[2] == 2 && arr[3] == 2 ;
	}
	
	public void testArrayWrite8(){
		int[] arr = new int[5];
		arr[0] = 1;
		assert arr[0] == 1 ;
	}
	
	public void testArrayWrite9(){
		int[] arr = new int[5];
		arr[0] = 1;
		assert arr[0] == 2 ;
	}
	
	public void testArrayWrite10(){
		int[] arr = new int[3];
		arr[0] = 1;
		assert arr[1] == 1 ;
	}
	
	public void testArrayWrite11(){
		int[] arr1 = new int[3];
		int[] arr2 = new int[3];
		arr1[0] = 1;
		arr2[0] = 2;
		assert arr2[0] == 2 && arr1[0] == 2;
	}
	
	public void testMatrix(int x1, int y1, int v1) {
		if (x1 < 5 && y1 < 5) {
			int[][] x = makeIntMatrix();
			x[y1][x1] = v1;
			assert x[y1][x1] == v1;
		}
	}

	private static int[][] makeIntMatrix() {
		int[][] x = new int[5][];
		x[0]= new int[5];
		x[1]= new int[5];
		x[2]= new int[5];
		x[3]= new int[5];
		x[4]= new int[5];
		return x;
	}

	private static float[][] makeFloatMatrix() {
		float[][] x = new float[5][];
		x[0]= new float[5];
		x[1]= new float[5];
		x[2]= new float[5];
		x[3]= new float[5];
		x[4]= new float[5];
		return x;
	}

	public void testNestedIfs(int i, int j){
		int k = foo(i,j);
		assert k != 10;
	}
	
	public void testNestedIfs2(int i, int j){
		int k = foo(i,j);
		assert k != 9;
	}
	
	public void testNestedIfs3(int i, int j){
		int k = foo(i,j);
		assert k != 0;
	}
	
	private static int sum_yx(int[][] a, int y, int x) {
		int total = 0;
		for(int i = y - 1; i <= y + 1; i++) {
			for(int j = x - 1; j <= x + 1; j++) {
				total += a[i][j];
			}
		}
		return total;
	}
	
	private static int sum_xy(int[][] a, int y, int x) {
		int total = 0;
		for(int j = x - 1; j <= x + 1; j++) {
			for(int i = y - 1; i <= y + 1; i++) {
				total += a[i][j];
			}
		}
		return total;
	}

	private static float sum_yx(float[][] a, int y, int x) {
		float total = 0f;
		for(int i = y - 1; i <= y + 1; i++) {
			for(int j = x - 1; j <= x + 1; j++) {
				total += a[i][j];
			}
		}
		return total;
	}
	
	private static float sum_xy(float[][] a, int y, int x) {
		float total = 0f;
		for(int j = x - 1; j <= x + 1; j++) {
			for(int i = y - 1; i <= y + 1; i++) {
				total += a[i][j];
			}
		}
		return total;
	}
	
	public void testMatrix2(int x1, int y1, int v1, int x2, int y2, int v2) {
		if (x1 < 5 && y1 < 5 && x2 < 5 && y2 < 5 && v1 > 0 && v2 > 0) {
			int[][] a = makeIntMatrix();
			a[y1][x1] = v1;
			a[y2][x2] = v2;
			int t1 = sum_yx(a, 1, 1);
			int t2 = sum_yx(a, 2, 2);
			int t3 = sum_yx(a, 3, 3);
			assert !(t1 != 0 && t3 != 0 && t1+t3 == t2 && t2 == 9);
		}
	}
	
	public void testMatrix3(int x1, int y1, int v1, int x2, int y2, int v2) {
		if (x1 < 5 && y1 < 5 && x2 < 5 && y2 < 5 && v1 > 0 && v2 > 0) {
			int[][] a = makeIntMatrix();
			a[y1][x1] = v1;
			a[y2][x2] = v2;
			int t1 = sum_yx(a, 1, 1);
			int t2 = sum_xy(a, 1, 1);
			assert t1 == t2;
		}
	}

	public void testMatrix4(int x1, int y1, int v1, int x2, int y2, int v2, int x3, int y3, int v3) {
		if (x1 < 5 && y1 < 5 && x2 < 5 && y2 < 5 && x3 < 5 && y3 < 5 && v1 > 0 && v2 > 0 && v3 > 0) {
			int[][] a = makeIntMatrix();
			a[y1][x1] = v1;
			a[y2][x2] = v2;
			a[y3][x3] = v3;
			int t1 = sum_yx(a, 1, 1);
			int t2 = sum_xy(a, 1, 1);
			assert t1 == t2;
		}
	}

	public void testFloatMatrix1(int x1, int y1, float v1, int x2, int y2, float v2, int x3, int y3, float v3, float a1, float a2) {
		if (x1 < 5 && y1 < 5 && x2 < 5 && y2 < 5 && x3 < 5 && y3 < 5 && v1 > 0f && v2 > 0f && v3 > 0f) {
			float[][] a = makeFloatMatrix();
			a[y1][x1] = v1;
			a[y2][x2] = v2;
			a[y3][x3] = v3;
			float t1 = sum_yx(a, 1, 1);
			float t2 = sum_xy(a, 1, 1);
			assert t1 == t2 || t1 != a1 || t2 != a2;
		}
	}

	public void testFloatMatrix2(int x1, int y1, float v1, int x2, int y2, float v2, float a1, float a2) {
		if (x1 < 5 && y1 < 5 && x2 < 5 && y2 < 5 && v1 > 0f && v2 > 0f) {
			float[][] a = makeFloatMatrix();
			a[y1][x1] = v1;
			a[y2][x2] = v2;
			float t1 = sum_yx(a, 1, 1);
			float t2 = sum_xy(a, 1, 1);
			assert t1 == t2 || t1 != a1 || t2 != a2;
		}
	}

	private int foo(int i, int j) {
		int k = 0;
		if (i == 3){
			k = 9;
			if (j != 4){
				k = k+1;
			}
		}
		return k;
	}
	
	public void testStatic() {
		foo = 7;
		assert foo != 7;
	}
	
	public void testStaticBoolean(){
		Value.myResult = true;
		assert Value.myResult;
	}
	
	public void testStaticInt(){
		Value.myInt = 1;
		assert Value.myInt == 1;
	}
	
	public void testStaticInt0(){
		Value.myInt = 0;
		assert Value.myInt == 0;
	}
	
	public void testStaticObject(){
		Object o = new Object();
		Value.myObject = o;
		assert Value.myObject == o;
	}
	
	public void testStaticInt1(){
		Value.myInt = 2;
		assert Value.myInt == 3;
	}
	
	public void testArray() {
		Little l = new Little();
		assert l.a[0] != 1;
	}
	
	public void testArray1(int[] b){
		assert b[0] != 1;
	}
	
	public void testInts(int x, int y, int z){
	    assert !(x > y && y > z);
	}
	
	public void testLoop(){
		for(int i = 0; i < 3; i++){
			assert i < 3;
		}
	}
	
	public void testLoop1(){
		int j = 0;
		while (j < 3){
			j++;
		}
		assert j == 5;
	}

	public static class IntCell {
		static final NullKey NULL_KEY = new NullKey();
		
		int value;
		
		IntCell() {
			value = 1;
		}
		
		IntCell(int value){
			this.value = value;
		}
		
		public boolean equals(Object o){
			return (o instanceof IntCell && value == ((IntCell) o).value);
		}
		public int hashCode(){
		    return super.hashCode() + value; 
		}
	}

	public static class FloatCell {
		static final NullKey NULL_KEY = new NullKey();
		
		private float value;
		
		FloatCell(float value){
			this.value = value;
		}
		
		public boolean equals(Object o){
			return (o instanceof FloatCell && value == ((FloatCell) o).value);
		}
		public int hashCode(){
		    return super.hashCode() + (int)(1031*value); 
		}
	}

	private static class SubCell extends IntCell {
		int myVal;
		SubCell() {
			myVal = 1;
		}
	}
	
	private static class Value  {
		public static boolean myResult;
		public static int myInt;
		public static Object myObject;
		
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
	}
	
	private static class BValue extends Value {
		  int hisVal;
			
		  public BValue(int myVal, int herVal, int hisVal){
		    super(myVal, herVal);
		    this.hisVal = hisVal;
		  }
			
		  public boolean equals(Object o){
		    return super.equals(o) && 
			   (o instanceof BValue) &&
			   hisVal == ((BValue)o).hisVal;
		  }
			
		  public int hashCode(){
		    return super.hashCode() + hisVal<<12;
		  }
	}
	private static class List {
		int value;
		List next;
		List last;
		
		List(int value) {
			this.value = value;
			last = this;
			//next = null;
		}
		
		void add(int value){
			List list = new List(value);
			last.next = list;
			last = list;
		}
		
		List remove(int value){
			List temp = this;
			List prev = null;
			while(temp != null){
				 if (temp.value == value){
					if (prev != null) {
						prev.next = temp.next;
						if (temp == last){
							last = prev;
							List temp1 = this;
							while(temp1 != null){
								temp1.last = prev;
								temp1 = temp1.next;
							}
						}
						return this;
					} else {
						temp.next.last = last;
						return temp.next;
					}
				}
				prev = temp;
				temp = temp.next;
			}
			return this;
		}
		
	}
	
	public void testCell(){
		IntCell l = new IntCell(1);
		assert l.value == 1;
	}
	
	public void testList(){
		List l = new List(1);
		l.add(2);
		l.add(3);
		assert l.next.last.value == 2;
	}
	
	public void testListRemoveOther(){
		List l = new List(1);
		l.add(2); l.add(3);
		assert l.remove(2).last.value == 3;
	}
	
	public void testListRemoveFirst(){
		List l = new List(1);
		l.add(2); l.add(3);
		assert l.remove(1).last.value == 3;
	}

	public void testSimpleFields(){
		List l = new List(1);
		l.add(2);
		List l2 = l.next;
		l.next = l2.next;
		assert l.next == null;
	}
	
	public void testListRemoveCounter(){
		List l = new List(1);
		l.add(2); l.add(3);
		assert l.last.value == 13;
	}
	
	
	static class MyArrayList {
		int[] arr;
		int last;
		
		MyArrayList(int capacity){
			arr = new int[capacity];
			//last = 0;
		}
		
		void add(int v){
			arr[last++] = v;
		}
		
		int get(int i){
			return arr[i];
		}
	}
	
	static class MyArrayListObj {
		Object[] arr;
		int last;
		
		MyArrayListObj(int capacity){
			arr = new Object[capacity];
			last = 0;
		}
		void add(Object o) { arr[last++] = o;}
		boolean contains(Object o){
			return indexOf(o) >= 0;
		}
		int indexOf(Object o){
			if (o == null) {
			    for (int i = 0; i < last; i++)
			    	if (arr[i]==null)
			    		return i;
			} else {
				for (int i = 0; i< last; i++){
					if (arr[i].equals(o)) return i;
				}
			}
			return -1;
		}
		boolean ensureCapacity(int minCapacity){
			int oldCapacity = arr.length;
			if (minCapacity > oldCapacity) {
				return true;
			}
			return false;
		}
	}
	
	
	public void testMyArrayList1(){
		MyArrayList arr = new MyArrayList(3);
		arr.add(1);
		assert arr.last == 1 && arr.arr[0] == 1;
	}
	
	public void testMyArrayList2(){
		MyArrayListObj arr = new MyArrayListObj(3);
		arr.add(new IntCell(1));
		assert !arr.contains(new IntCell(1));
	}
	
	public void testMyArrayList3(){
		MyArrayListObj arr  = new MyArrayListObj(3);
		arr.add(new IntCell(1));
		arr.add(new IntCell(2));
		assert arr.contains(new IntCell(1));
	}
	
	public void testMyArrayList4(){
		MyArrayListObj arr = new MyArrayListObj(3);
		arr.add(new IntCell(1));
		assert !arr.contains(new IntCell(2));
	}
	
	public void testMyArrayList5(){
		MyArrayListObj arr = new MyArrayListObj(3);
		arr.add(new IntCell(1));
		assert arr.indexOf(new IntCell(1)) == 0;
	}
	
	public void testMyArrayList6(){
		MyArrayListObj arr = new MyArrayListObj(3);
		arr.add(new IntCell(1));
		assert arr.indexOf(new IntCell(2)) == -1;
	}
	
	public void testMyArrayList7(){
		MyArrayListObj arr = new MyArrayListObj(2);
		Value v1 = new Value(1,2);
		Value v2 = new Value(3,4);
		Value v3 = new Value(1,2);
		arr.add(v1);
		arr.add(v2);
		assert arr.contains(v3);
	}
	

	public void testIntFields0(){
		IntCell c = new IntCell(0);
		assert c.value == 0;
	}
	
	public void testIntFields1(){
		IntCell c = new IntCell(1);
		IntCell c1 = new IntCell(2);
		assert c.value == 1 && c1.value == 2;
	}
	
	public void testIntFields2(){
		IntCell c = new IntCell(1);
		IntCell c1 = new IntCell(2);
		assert c.value == 2 || c1.value == 1;
	}
	
	public void testCell1(){
		IntCell c1 = new IntCell(1);
		IntCell c2 = new IntCell(2);
		assert !c1.equals(c2);
	}
	
	public void testCell2(){
		IntCell c1 = new IntCell(3);
		IntCell c2 = new IntCell(3);
		assert !c1.equals(c2);
	}
	
	
	public void testArrayCopy1(){
		int[] arr1 = new int[3];
		arr1[0] = 1; arr1[1] = 2;
		int[] arr2 = new int[3];
		for(int i = 0; i < arr1.length; i++){
			arr2[i] = arr1[i];
		}
		assert arr2[0] == 1 && arr2[1] == 2;
	}

	public void testArrayCopy2(){
		IntCell[] arr1 = new IntCell[3];
		arr1[0] = new IntCell(1); arr1[1] = new IntCell(2);
		IntCell[] arr2 = new IntCell[3];
		for(int i = 0; i < arr1.length; i++){
			arr2[i] = arr1[i];
		}
		assert arr2[0].equals(new IntCell(1)) && arr2[1].equals(new IntCell(2));
	}
	
	public void testArrayAssign(){
		int[] arr = new int[3];
		arr[1 + 0] = 3;
		assert arr[1] == 3;
	}
	
	public void testArrayParam(){
		int[] arr1 = new int[3];
		int[] arr2 = new int[3];
		arr1[0] = 1; arr1[1] = 2; arr1[2] = 3;
		foo(arr1, arr2);
		assert arr2[0] == 1 && arr2[1] == 2 && arr2[2] == 3 && arr1[0] == 1 && arr1[1] == 2 && arr1[2] == 3 ;
	}
	
	public void testArrayParam4(){
		int[] arr1 = new int[3];
		int[] arr2 = new int[3];
		arr1[0] = 1; arr1[1] = 2; arr1[2] = 3;
		foo(arr1, arr2);
		assert arr2[0] == 1 && arr2[1] == 2 && arr2[2] == 4 && arr1[0] == 1 && arr1[1] == 2 && arr1[2] == 3 ;
	}
	
	private void foo(int[] arr1, int[] arr2){
		for(int i = 0; i< arr1.length; i++){
			arr2[i] = arr1[i];
		}
	}
	
	public void testArrayParam1(){
		int[] arr = new int[3];
		bar1(arr);
		assert arr[0] == 1 && arr[1] != 1;
	}
	
	private void bar1(int[] arr){
		arr[0] = 1;
		arr[1] = 1;
	}
	
	public void testArrayParam2(){
		int[] arr1 = new int[2];
		int[] arr2 = new int[2];
		foo1(arr2, arr1);
		assert arr2[0] == 1 && arr1[1] == 2;
	}
	
	private void foo1(int[] arr2, int[] arr1){
		arr2[0] = 1;		
		arr1[1] = 2;
	}
	
	public void testArrayParam3(){
		int[] arr1 = new int[2];
		int[] arr2 = new int[2];
		foo2(arr2, arr1);
		assert arr2[0] == 1 && arr1[1] == 2;
	}
	
	private void foo2(int[] arr2, int[] arr1){
		arr1[1] = 2;
		arr2[0] = 1;		
		
	}
	
	public void testFieldParam(){
		IntCell c = new IntCell(0);
		bar(c);
		assert c.value == 1;
	}
	
	private void bar(IntCell c){
		c.value = 1;
	}
	
	public void testConstructorSubclass(){
		SubCell c = new SubCell();
		assert c.myVal == 1;
	}
	
	public void testObjectHashCodeCall(){
		IntCell c = new IntCell(1);
		assert c.hashCode() == 0;
	}
	
	public void testObjectHashCodeCall1(){
		Object o = new IntCell();
		int k = hash(o);
		assert k == 1;
	}
	
	public void testObjectHashCodeCall2(){
		Object k = maskNull(new IntCell());
        int hash = hash(k);
        assert hash == 2;
	}
	
	static Object maskNull(Object key) {
	    return (key == null ? IntCell.NULL_KEY : key);
	}
	
	private static int hash(Object o){
		return o.hashCode();
	}
	
	public void testIrrelevant(){
		Value v = new Value(1,2);
		assert true;
	}
	
	public void testEquals(){
		Value v = new Value(1,2);
		Value v1 = new Value(1,2);
		assert v.equals(v1);
	}
	
	public void testEquals1(){
		BValue v = new BValue(1,2,3);
		BValue v1 = new BValue(1,2,3);
		assert v.equals(v1);
	}
	
	public void testEquals2(){
		BadValue v = new BadValue(1,2,3);
		BadValue v1 = new BadValue(1,2,3);
		assert v.equals(v1);
	}
	
	private static class MyEntry {
		Object element;
		MyEntry next;
		MyEntry previous;

		MyEntry(Object element, MyEntry next, MyEntry previous) {
			this.element = element;
			this.next = next;
			this.previous = previous;
		}
	}
	
	private static class MyLinkedList {
		private MyEntry header; 
	    private int size = 0;
		
	    public MyLinkedList() {
	    	header = new MyEntry(null, null, null);
	    	header.next = header;
	    	header.previous = header;
	    	return;
	    }
	    
	    public boolean add(Object o) {
	    	//addBefore(o, header);
	    	//MyEntry newEntry = new MyEntry(o, null, null);
	    	//newEntry.previous.next = newEntry;
	    	//newEntry.next.previous = newEntry;
	        return true;
	    }
	    
	    private MyEntry addBefore(Object o, MyEntry e) {
	    	MyEntry newEntry = new MyEntry(o, e, e.previous);
	    	newEntry.previous.next = newEntry;
	    	newEntry.next.previous = newEntry;
	    	size++;
	    	return newEntry;
	    }
	}
	
	
	public void testMyLinkedList1(){
		MyLinkedList l = new MyLinkedList();
		assert l.header.next == l.header.previous;
	}
	
	public void testMyLinkedList2(){
		MyLinkedList l = new MyLinkedList();
		assert l.header.next != l.header.previous;
	}
	
	public void testMyLinkedList3(){
		MyLinkedList l = new MyLinkedList();
		assert l.header == null;
	}
	
	public void testFieldAccess(){
		MyEntry e1 = new MyEntry(new Value(1,2), null, null);
		MyEntry e2 = new MyEntry(new Value(3,4), null, null);
		e1.next = e2;
		e2.previous = e1;
		assert e2.next == e1;
	}
	
	private boolean isSpecial(float x) {
		return Float.isNaN(x) || Float.isInfinite(x);
	}	
}

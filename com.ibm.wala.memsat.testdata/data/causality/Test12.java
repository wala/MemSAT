package data.causality;

final public class Test12 {
    static int x = 0;
    static int y = 0;
    final static int[] a = { 1, 2 };
    
    final public static void thread1() {
        final int r1 = x;
        a[r1] = 0;
        final int r2 = a[0];
        y = r2;
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread2() {
        final int r3 = y;
        x = r3;
        assert r3 == 1;
    }
    
    public Test12() { super(); }
}

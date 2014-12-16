package data.nemos;

final public class Test12 {
    static int a = 0;
    static int b = 0;
    
    final public static void p1() { a = 1; }
    
    final public static void p2() {
        final int r1 = a;
        b = 1;
        assert r1 == 1;
    }
    
    final public static void p3() {
        final int r2 = b;
        final int r3 = a;
        assert r2 == 1;
        assert r3 == 0;
    }
    
    public Test12() { super(); }
}

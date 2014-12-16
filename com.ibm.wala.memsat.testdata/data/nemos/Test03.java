package data.nemos;

final public class Test03 {
    static int a = 0;
    static int b = 0;
    static int c = 0;
    
    final public static void p1() { b = 0; }
    
    final public static void p2() { b = 1; }
    
    final public static void p3() {
        final int r1 = b;
        a = 0;
        final int r2 = c;
        assert r1 == 0;
        assert r2 == 0;
    }
    
    final public static void p4() {
        final int r3 = b;
        final int r4 = a;
        final int r5 = c;
        assert r3 == 1;
        assert r4 == 0;
        assert r5 == 1;
    }
    
    final public static void p5() {
        final int r6 = c;
        final int r7 = a;
        final int r8 = b;
        assert r6 == 1;
        assert r7 == 1;
        assert r8 == 1;
    }
    
    final public static void p6() {
        final int r9 = c;
        a = 1;
        final int r10 = b;
        assert r9 == 0;
        assert r10 == 0;
    }
    
    final public static void p7() { c = 1; }
    
    final public static void p8() { c = 0; }
    
    public Test03() { super(); }
}

package data.nemos;

final public class Test15 {
    static int a = 0;
    static int b = 0;
    static int c = 0;
    
    final public static void p1() {
        a = 1;
        c = 1;
        final int r1 = c;
        final int r2 = b;
        assert r1 == 1;
        assert r2 == 0;
    }
    
    final public static void p2() {
        b = 1;
        c = 2;
        final int r3 = c;
        final int r4 = a;
        assert r3 == 2;
        assert r4 == 0;
    }
    
    public Test15() { super(); }
}

package data.nemos;

final public class Test08 {
    static int x = 0;
    static int y = 0;
    
    final public static void p1() {
        x = 0;
        y = 0;
    }
    
    final public static void p2() {
        final int r1 = y;
        x = 1;
        assert r1 == 0;
    }
    
    final public static void p3() {
        final int r2 = x;
        final int r3 = x;
        assert r2 == 1;
        assert r3 == 0;
    }
    
    public Test08() { super(); }
}

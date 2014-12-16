package data.nemos;

final public class Test10 {
    static int x = 0;
    static int y = 0;
    
    final public static void p1() {
        x = 1;
        y = 1;
    }
    
    final public static void p2() {
        final int r1 = y;
        final int r2 = x;
        assert r1 == 1;
        assert r2 == 0;
    }
    
    public Test10() { super(); }
}

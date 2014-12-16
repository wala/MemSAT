package data.nemos;

final public class Test11 {
    static int x = 0;
    static int y = 0;
    
    final public static void p1() {
        x = 1;
        final int r1 = y;
        assert r1 == 0;
    }
    
    final public static void p2() {
        y = 1;
        final int r2 = x;
        assert r2 == 0;
    }
    
    public Test11() { super(); }
}

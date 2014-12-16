package data.nemos;

final public class Test07 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void p1() {
        x = 0;
        x = 1;
        z = 0;
        final int r1 = y;
        assert r1 == 0;
    }
    
    final public static void p2() {
        y = 0;
        y = 1;
        z = 1;
        final int r2 = x;
        assert r2 == 0;
    }
    
    public Test07() { super(); }
}

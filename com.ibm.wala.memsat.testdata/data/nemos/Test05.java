package data.nemos;

final public class Test05 {
    static int x = 0;
    static int y = 0;
    
    final public static void p1() {
        x = 0;
        x = 1;
        y = 2;
    }
    
    final public static void p2() {
        final int r1 = y;
        final int r2 = x;
        assert r1 == 2;
        assert r2 == 0;
    }
    
    public Test05() { super(); }
}

package data.nemos;

final public class Test06 {
    static int x = 0;
    
    final public static void p1() {
        x = 0;
        final int r1 = x;
        assert r1 == 1;
    }
    
    final public static void p2() {
        x = 1;
        final int r2 = x;
        assert r2 == 0;
    }
    
    public Test06() { super(); }
}

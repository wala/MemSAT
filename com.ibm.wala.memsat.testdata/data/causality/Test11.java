package data.causality;

public class Test11 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    static int w = 0;
    
    final public static void thread1() {
        final int r1 = z;
        w = r1;
        final int r2 = x;
        y = r2;
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread2() {
        final int r4 = w;
        final int r3 = y;
        z = r3;
        x = 1;
        assert r3 == 1;
        assert r4 == 1;
    }
    
    public Test11() { super(); }
}

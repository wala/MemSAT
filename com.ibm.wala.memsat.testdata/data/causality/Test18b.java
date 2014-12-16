package data.causality;

public class Test18b {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r3 = x;
        if (r3 == 0) x = 41;
        if (r3 == 0) x = 42;
        final int r1 = x;
        y = r1;
        assert r1 == 42;
        assert r3 == 42;
    }
    
    final public static void thread2() {
        final int r2 = y;
        x = r2;
        assert r2 == 42;
    }
    
    public Test18b() { super(); }
}

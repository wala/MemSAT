package data.causality;

final public class Test18a {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r3 = x;
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
    
    final public static void thread3() {
        final int r4 = x;
        if (r4 == 71) x = 71;
    }
    
    public Test18a() { super(); }
}

package data.causality;

final public class Test10 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void thread1() {
        final int r1 = x;
        if (r1 == 1) y = 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        if (r2 == 1) x = 1;
        assert r2 == 1;
    }
    
    final public static void thread3() { z = 1; }
    
    final public static void thread4() {
        final int r3 = z;
        if (r3 == 1) x = 1;
        assert r3 == 0;
    }
    
    public Test10() { super(); }
}

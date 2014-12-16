package data.causality;

final public class Test05 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void thread1() {
        final int r1 = x;
        y = r1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        x = r2;
        assert r2 == 1;
    }
    
    final public static void thread3() { z = 1; }
    
    final public static void thread4() {
        final int r3 = z;
        x = r3;
        assert r3 == 0;
    }
    
    public Test05() { super(); }
}

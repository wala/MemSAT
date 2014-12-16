package data.causality;

final public class Test24 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    static int v = 0;
    
    final public static void thread1() {
        x = 0;
        x = 1;
        y = 1;
    }
    
    final public static void thread2() {
        final int r1 = y;
        final int r2 = z;
        assert r1 == 1;
        assert r2 == 0;
    }
    
    final public static void thread3() {
        z = 0;
        z = 1;
        v = 1;
    }
    
    final public static void thread4() {
        final int r3 = v;
        final int r4 = x;
        assert r3 == 1;
        assert r4 == 0;
    }
    
    public Test24() { super(); }
}

package data.causality;

final public class Test03 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        final int r2 = x;
        if (r1 == r2) y = 1;
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread2() {
        final int r3 = y;
        x = r3;
        assert r3 == 1;
    }
    
    final public static void thread3() { x = 2; }
    
    public Test03() { super(); }
}

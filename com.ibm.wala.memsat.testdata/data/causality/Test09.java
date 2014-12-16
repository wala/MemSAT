package data.causality;

final public class Test09 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        final int r2 = 1 + r1 * r1 - r1;
        y = r2;
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread2() {
        final int r3 = y;
        x = r3;
    }
    
    final public static void thread3() { x = 2; }
    
    public Test09() { super(); }
}

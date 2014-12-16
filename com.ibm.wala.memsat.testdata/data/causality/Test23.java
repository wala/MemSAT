package data.causality;

final public class Test23 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() { x = 1; }
    
    final public static void thread2() {
        final int r1 = x;
        y = 1;
        assert r1 == 1;
    }
    
    final public static void thread3() {
        final int r2 = y;
        final int r3 = x;
        assert r2 == 1;
        assert r3 == 0;
    }
    
    public Test23() { super(); }
}

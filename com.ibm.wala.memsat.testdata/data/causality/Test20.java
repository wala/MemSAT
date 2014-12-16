package data.causality;

final public class Test20 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        y = r1;
        assert r1 == 42;
    }
    
    final public static void thread2() {
        final int r2 = y;
        x = r2;
        assert r2 == 42;
    }
    
    final public static void thread3() {
        final int r3 = x;
        if (r3 != 42) x = 42;
        assert r3 == 42;
    }
    
    public Test20() { super(); }
}

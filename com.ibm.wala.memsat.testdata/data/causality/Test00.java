package data.causality;

final public class Test00 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        y = 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        x = 1;
        assert r2 == 1;
    }
    
    public Test00() { super(); }
}

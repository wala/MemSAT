package data.causality;

final public class Test13 {
    static int x = 0;
    static int y = 0;
    
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
    
    public Test13() { super(); }
}

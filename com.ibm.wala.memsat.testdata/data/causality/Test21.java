package data.causality;

final public class Test21 {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        if (r1 != 0) y = r1; else y = 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        x = r2;
        assert r2 == 1;
    }
    
    public Test21() { super(); }
}

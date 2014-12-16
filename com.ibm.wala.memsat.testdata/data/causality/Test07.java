package data.causality;

final public class Test07 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void thread1() {
        final int r1 = z;
        final int r2 = x;
        y = r2;
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread2() {
        final int r3 = y;
        z = r3;
        x = 1;
        assert r3 == 1;
    }
    
    public Test07() { super(); }
}

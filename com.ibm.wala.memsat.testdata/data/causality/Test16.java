package data.causality;

final public class Test16 {
    static int x = 0;
    
    final public static void thread1() {
        final int r1 = x;
        x = 1;
        assert r1 == 2;
    }
    
    final public static void thread2() {
        final int r2 = x;
        x = 2;
        assert r2 == 1;
    }
    
    public Test16() { super(); }
}

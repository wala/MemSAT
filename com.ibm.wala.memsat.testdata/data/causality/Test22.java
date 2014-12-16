package data.causality;

final public class Test22 {
    static int a = 0;
    static int b = 0;
    static int c = 0;
    
    final public static void thread1() {
        a = 1;
        c = 0;
        final int r1 = b;
        assert r1 == 0;
    }
    
    final public static void thread2() {
        b = 1;
        c = 2;
        final int r2 = a;
        assert r2 == 0;
    }
    
    public Test22() { super(); }
}

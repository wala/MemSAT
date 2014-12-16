package data.causality;

final public class Test06 {
    static int A = 0;
    static int B = 0;
    
    final public static void thread1() {
        final int r1 = A;
        if (r1 == 1) B = 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = B;
        if (r2 == 1) A = 1;
        if (r2 == 0) A = 1;
        assert r2 == 1;
    }
    
    public Test06() { super(); }
}

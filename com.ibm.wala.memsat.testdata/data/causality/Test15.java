package data.causality;

final public class Test15 {
    static int a = 0;
    static int b = 0;
    static volatile int x = 0;
    static volatile int y = 0;
    
    final public static void thread1() {
        final int r0 = x;
        final int r1;
        if (r0 == 1) r1 = a; else r1 = 0;
        if (r1 == 0) y = 1; else b = 1;
        assert r0 == 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        int r2;
        int r3;
        do  {
            r2 = y;
            r3 = b;
        }while(r2 + r3 == 0); 
        a = 1;
        assert r2 == 0;
        assert r3 == 1;
    }
    
    final public static void thread3() { x = 1; }
    
    public Test15() { super(); }
}

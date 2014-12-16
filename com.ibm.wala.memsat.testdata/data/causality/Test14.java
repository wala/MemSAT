package data.causality;

final public class Test14 {
    static int a = 0;
    static int b = 0;
    static volatile int y = 0;
    
    final public static void thread1() {
        final int r1 = a;
        if (r1 == 0) y = 1; else b = 1;
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
    
    public Test14() { super(); }
}

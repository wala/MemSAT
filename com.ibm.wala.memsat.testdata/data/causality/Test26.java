package data.causality;

public class Test26 {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void thread1() {
        final int r1;
        final int r2;
        final int r3;
        r1 = x;
        if (r1 == 0) {
            r2 = x;
            r3 = z;
            y = r3;
        } else {
            r2 = 0;
            r3 = z;
            y = 1;
        }
        assert r1 == 0 && r2 == 1 && r3 == 1;
    }
    
    final public static void thread2() {
        x = 1;
        final int r4 = y;
        z = r4;
        assert r4 == 1;
    }
    
    public Test26() { super(); }
}

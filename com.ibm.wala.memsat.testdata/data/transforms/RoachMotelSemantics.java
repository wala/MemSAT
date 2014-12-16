package data.transforms;

final public class RoachMotelSemantics {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    final static Object m = new Object();
    
    final public static void thread1() { synchronized (m)  { x = 2; } }
    
    final public static void thread2() { synchronized (m)  { x = 1; } }
    
    final public static void thread3() {
        final int r1;
        final int r2;
        r1 = x;
        synchronized (m)  {
            r2 = z;
            if (r1 == 2) y = 1; else y = r2;
        }
        assert r1 == 1;
        assert r2 == 1;
    }
    
    final public static void thread4() {
        final int r3 = y;
        z = r3;
        assert r3 == 1;
    }
    
    final public static void thread3T() {
        final int r1;
        final int r2;
        synchronized (m)  {
            r1 = x;
            r2 = z;
            if (r1 == 2) y = 1; else y = r2;
        }
        assert r1 == 1;
        assert r2 == 1;
    }
    
    public RoachMotelSemantics() { super(); }
}

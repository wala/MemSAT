package data.transforms;

final public class RedundantWriteAfterReadElimination {
    static int x = 0;
    final static Object m1;
    final static Object m2;
    
    static {
        m1 = new Object();
        m2 = new Object();
    }
    
    final public static void thread1() { synchronized (m1)  { x = 2; } }
    
    final public static void thread2() { synchronized (m2)  { x = 1; } }
    
    final public static void thread3() {
        final int r1;
        final int r2;
        synchronized (m1)  {
            synchronized (m2)  {
                r1 = x;
                x = r1;
                r2 = x;
            }
        }
        assert r1 != r2;
    }
    
    final public static void thread3T() {
        final int r1;
        final int r2;
        synchronized (m1)  {
            synchronized (m2)  {
                r1 = x;
                r2 = x;
            }
        }
        assert r1 != r2;
    }
    
    public RedundantWriteAfterReadElimination() { super(); }
}

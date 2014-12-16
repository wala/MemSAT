package data.transforms;

public class RedundantReadAfterReadElimination {
    static int x = 0;
    static int y = 0;
    
    final public static void thread1() {
        final int r1 = x;
        y = r1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        if (r2 == 1) {
            final int r3 = y;
            x = r3;
        } else {
            x = 1;
        }
        assert r2 == 1;
    }
    
    final public static void thread2T() {
        final int r2 = y;
        if (r2 == 1) {
            final int r3 = r2;
            x = r3;
        } else {
            x = 1;
        }
        assert r2 == 1;
    }
    
    public RedundantReadAfterReadElimination() { super(); }
}

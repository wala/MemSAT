package data.transforms;

final public class IrrelevantReadIntroduction {
    static int x = 0;
    static int y = 0;
    static int z = 0;
    
    final public static void thread1() {
        final int r1 = z;
        if (r1 == 0) {
            final int r3 = x;
            if (r3 == 1) y = 1;
        } else {
            final int r4 = 1;
            y = r1;
        }
        assert r1 == 1;
    }
    
    final public static void thread2() {
        x = 1;
        final int r2 = y;
        z = r2;
        assert r2 == 1;
    }
    
    final public static void thread1T() {
        final int r1 = z;
        if (r1 == 0) {
            final int r3 = x;
            if (r3 == 1) y = 1;
        } else {
            int r4 = x;
            r4 = 1;
            y = r1;
        }
        assert r1 == 1;
    }
    
    public IrrelevantReadIntroduction() { super(); }
}

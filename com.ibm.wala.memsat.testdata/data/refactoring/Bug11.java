package data.refactoring;

final public class Bug11 {
    static volatile int x = 0;
    static volatile boolean y = false;
    
    final public static void m(int i, boolean b) {  }
    
    final public static void thread1() {
        final int r1;
        Bug11.m(r1 = x, y = true);
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final boolean r2 = y;
        x = 1;
        assert r2;
    }
    
    public Bug11() { super(); }
}

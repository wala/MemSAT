package data.refactoring;

public class Bug11R {
    static volatile int x = 0;
    static volatile boolean y = false;
    
    final public static void m(boolean b, int i) {  }
    
    final public static void thread1() {
        final int r1;
        Bug11R.m(y = true, r1 = x);
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final boolean r2 = y;
        x = 1;
        assert r2;
    }
    
    public Bug11R() { super(); }
}

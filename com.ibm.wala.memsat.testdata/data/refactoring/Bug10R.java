package data.refactoring;

public class Bug10R {
    static volatile int value = 0;
    static int x = 0;
    static int y = 0;
    final static Object m = new Object();
    
    final public static void thread1() {
        final int r1 = x;
        synchronized (m)  {  }
        y = 1;
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = y;
        final int r3 = value;
        value = r3 + 1;
        x = 1;
        assert r2 == 1;
    }
    
    public Bug10R() { super(); }
}

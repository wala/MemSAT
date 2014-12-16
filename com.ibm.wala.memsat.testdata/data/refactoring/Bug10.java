package data.refactoring;

final public class Bug10 {
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
        synchronized (m)  {  }
        x = 1;
        assert r2 == 1;
    }
    
    public Bug10() { super(); }
}

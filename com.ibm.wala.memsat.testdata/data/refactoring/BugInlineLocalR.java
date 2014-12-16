package data.refactoring;

final public class BugInlineLocalR {
    static volatile int x = 0;
    
    public static void thread1() {
        final int r2 = x + x;
        assert r2 == 1;
    }
    
    public static void thread2() { x = 1; }
    
    public BugInlineLocalR() { super(); }
}

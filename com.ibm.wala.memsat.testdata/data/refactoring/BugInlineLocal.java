package data.refactoring;

final public class BugInlineLocal {
    static volatile int x = 0;
    
    public static void thread1() {
        final int r1 = x;
        final int r2 = r1 + r1;
        assert r2 == 1;
    }
    
    public static void thread2() { x = 1; }
    
    public BugInlineLocal() { super(); }
}

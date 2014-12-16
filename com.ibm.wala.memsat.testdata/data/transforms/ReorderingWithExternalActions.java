package data.transforms;

final public class ReorderingWithExternalActions {
    static int x = 0;
    static int y = 0;
    
    public static void print(String s) {  }
    
    final public static void thread1() {
        final int r1 = y;
        if (r1 == 1)
            x = 1;
        else {
            ReorderingWithExternalActions.print("!");
            x = 1;
        }
        assert r1 == 1;
    }
    
    final public static void thread2() {
        final int r2 = x;
        y = r2;
        assert r2 == 1;
    }
    
    final public static void thread1T() {
        final int r1 = y;
        if (r1 == 1)
            x = 1;
        else {
            x = 1;
            ReorderingWithExternalActions.print("!");
        }
        assert r1 == 1;
    }
    
    public ReorderingWithExternalActions() { super(); }
}

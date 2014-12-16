package data.nemos;

final public class Test02 {
    static int a = 0;
    static int b = 0;
    
    final public static void p1() {
        final int r1 = b;
        a = 1;
        assert r1 == 1;
    }
    
    final public static void p2() {
        final int r2 = a;
        b = 1;
        assert r2 == 1;
    }
    
    public Test02() { super(); }
}

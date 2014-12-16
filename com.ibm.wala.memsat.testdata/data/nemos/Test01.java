package data.nemos;

final public class Test01 {
    static int a = 0;
    
    final public static void p1() {
        final int r1 = a;
        assert r1 == 0;
    }
    
    final public static void p2() { a = 0; }
    
    public Test01() { super(); }
}

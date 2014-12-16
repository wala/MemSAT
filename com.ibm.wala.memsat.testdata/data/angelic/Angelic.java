package data.angelic;

final public class Angelic {
    final static class Entry {
        Entry next;
        Entry f;
        
        public Entry() { super(); }
    }
    
    
    final public static void test00() {
        final Entry x = new Entry();
        x.next = new Entry();
        x.f = new Entry();
        x.next.f = null;
        final Entry y = x;
        assert y.f == null;
    }
    
    final public static void test01(Entry angelicChoice) {
        final Entry x = new Entry();
        x.next = new Entry();
        x.f = new Entry();
        x.next.f = null;
        final Entry y = angelicChoice;
        assert y.f == null;
    }
    
    final public static void test02() {
        final Entry x = new Entry();
        x.next = new Entry();
        x.f = new Entry();
        x.next.f = null;
        final Entry y = (Entry) NonDetChoice.chooseObject();
        assert y.f == null;
    }
    
    public Angelic() { super(); }
}

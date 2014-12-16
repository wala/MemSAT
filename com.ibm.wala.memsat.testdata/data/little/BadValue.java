package data.little;

public class BadValue extends Value {
    int hisVal;
    
    public BadValue(int myVal, int herVal, int hisVal) {
        super(myVal, herVal);
        this.hisVal = hisVal;
    }
    
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof BadValue && hisVal ==
          ((BadValue) o).hisVal;
    }
    
    public int hashCode() { return super.hashCode() + hisVal << 12; }
}

package data.little;

public class Value implements Comparable {
    final int myVal;
    final int herVal;
    
    public Value(int myVal, int herVal) {
        super();
        this.myVal = myVal;
        this.herVal = herVal;
    }
    
    public boolean equals(Object o) {
        return o instanceof Value && myVal == ((Value) o).myVal && herVal ==
          ((Value) o).herVal;
    }
    
    public int hashCode() { return myVal + 5 * herVal; }
    
    public int compareTo(Object o) {
        if (myVal != ((Value) o).myVal) {
            return myVal - ((Value) o).myVal;
        } else {
            return herVal - ((Value) o).herVal;
        }
    }
}

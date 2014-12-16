package data.angelic;

final class Tokenizer {
    final Object open;
    final Object close;
    final Object plus;
    final Object delimiter;
    final Object operator;
    final Object operand;
    
    Tokenizer() {
        super();
        this.open = new Object();
        this.close = new Object();
        this.plus = new Object();
        this.delimiter = new Object();
        this.operator = new Object();
        this.operand = new Object();
    }
    
    Token token(Object tokenizable) {
        if (tokenizable == open || tokenizable == close)
            return new Token(tokenizable, delimiter);
        else
                if (tokenizable == plus)
                    return new Token(tokenizable, operator);
                else return new Token(tokenizable, tokenizable);
    }
    
    final static class Token {
        final Object tokenized;
        final Object kind;
        
        private Token(Object tokenized, Object kind) {
            super();
            this.tokenized = tokenized;
            this.kind = kind;
        }
    }
    
}

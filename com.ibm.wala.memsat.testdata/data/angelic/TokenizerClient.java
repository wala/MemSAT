package data.angelic;

import data.angelic.Tokenizer.Token;

final public class TokenizerClient {
    
    public static void test00() {
        final Tokenizer tokenizer = new Tokenizer();
        final LinkedList objs = new LinkedList();
        objs.add(new Object());
        objs.add(tokenizer.plus);
        objs.add(new Object());
        while (objs.size() > 0) {
            final Object obj = objs.removeFirst();
            final Token token = tokenizer.token(obj);
            final Object kind = token.kind;
            if (kind == tokenizer.operator) {
                final Object nextObj = objs.removeFirst();
                final Token nextToken = tokenizer.token(nextObj);
                assert nextToken.tokenized == nextObj;
                assert nextToken.kind == tokenizer.operand;
            }
        }
    }
    
    public static void test01(Token angelic) {
        assert angelic != null;
        final Tokenizer tokenizer = new Tokenizer();
        final LinkedList objs = new LinkedList();
        objs.add(new Object());
        objs.add(tokenizer.plus);
        objs.add(new Object());
        while (objs.size() > 0) {
            final Object obj = objs.removeFirst();
            final Token token = tokenizer.token(obj);
            final Object kind = token.kind;
            if (kind == tokenizer.operator) {
                final Object nextObj = objs.removeFirst();
                final Token nextToken = angelic;
                assert nextToken.tokenized == nextObj;
                assert nextToken.kind == tokenizer.operand;
            }
        }
    }
    
    public static void test02() {
        final Tokenizer tokenizer = new Tokenizer();
        final LinkedList objs = new LinkedList();
        objs.add(new Object());
        objs.add(tokenizer.plus);
        objs.add(new Object());
        int blockBalance = 0;
        while (objs.size() > 0) {
            final Token token = tokenizer.token(objs.removeFirst());
            final Object kind = token.kind;
            if (token == tokenizer.open) {
                blockBalance++;
            } else
                      if (token == tokenizer.close) {
                          blockBalance--;
                      } else
                                if (kind == tokenizer.operator) {
                                    assert objs.size() > 0;
                                    assert tokenizer.token(
                                             objs.removeFirst()).kind ==
                                      tokenizer.operand;
                                }
        }
        assert blockBalance == 0;
    }
    
    public TokenizerClient() { super(); }
}

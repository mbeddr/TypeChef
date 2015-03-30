package de.fosd.typechef.lexer;

import de.fosd.typechef.LexerToken;

public class DefaultTokenSelector implements TokenSelector {

    public static final DefaultTokenSelector INSTANCE = new DefaultTokenSelector();

    private DefaultTokenSelector() {

    }

    @Override
    public boolean isAttachableToken(LexerToken token) {
        if (token instanceof Token) {
            int type = ((Token) token).getType();
            return type == Token.CCOMMENT || type == Token.CPPCOMMENT || type == Token.NL;
        } else {
            return false;
        }
    }

    @Override
    public boolean isLanguageToken(LexerToken token) {
        return token.isLanguageToken();
    }
}

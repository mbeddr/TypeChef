package de.fosd.typechef.lexer;

import de.fosd.typechef.LexerToken;

public interface TokenSelector {

    public boolean isLanguageToken(LexerToken token);

    public boolean isAttachableToken(LexerToken token);

}

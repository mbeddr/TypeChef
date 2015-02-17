package de.fosd.typechef.parser.c

import de.fosd.typechef.LexerToken
import de.fosd.typechef.error.Position
import de.fosd.typechef.featureexpr._
import de.fosd.typechef.lexer._
import de.fosd.typechef.parser._


/**
 * thin wrapper around jccp tokens to make them accessible to MultiFeatureParser
 * @author kaestner
 *
 */
class CToken(val token: LexerToken, val number: Int) extends ProfilingToken with AbstractToken {

    def getFeature = token.getFeature

    def isInteger = token.isNumberLiteral

    def isKeywordOrIdentifier = token.isKeywordOrIdentifier

    def getText: String = token.getText

    def isString: Boolean = token.isStringLiteral

    def isCharacter: Boolean = token.isCharacterLiteral

    def isComment: Boolean = token.isComment

    def isInclude: Boolean = token.isInclude

    def isDefine : Boolean = token.isDefine

    override def toString = "\"" + token.getText + "\"" + (if (!getFeature.isTautology) getFeature else "")

    private lazy val pos = new TokenPosition(
        if (token.getSourceName == null || token.getSourceName.isEmpty) null else token.getSourceName,
        token.getLine,
        token.getColumn,
        number
    )

    def getPosition = {
        pos
    }
}

class TokenPosition(file: String, line: Int, column: Int, tokenNr: Int) extends Position {
    def getFile = file
    def getLine = line
    def getColumn = column
    override def toString = "file: " + file + " token: " + tokenNr + " line: " + getLine
}


object CToken {

    /**
     * Factory method for the creation of TokenWrappers.
     */
    def apply(token: LexerToken, number: Int) = {
        new CToken(token, number)
    }

    val EOF = new CToken(new EOFToken(), -1) {
        override def getFeature = FeatureExprFactory.False
    }
}
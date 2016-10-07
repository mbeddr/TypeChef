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

    def isIdentifier: Boolean = {
        val value: Boolean = token.isIdentifier
        value
    }

    def isComment: Boolean = token.isComment

    def isInclude: Boolean = token.isInclude

    def isDefine: Boolean = token.isDefine

    def isUndefine: Boolean = token.isUndefine

    def isPragma: Boolean = token.isPragma

    def isHeaderElement: Boolean = {
        token.getSource != null && token.getSource.isInstanceOf[LexerSource] && token.getSource.asInstanceOf[LexerSource].getIdentifier.isHeaderFileSource
    }

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

    override def getTokenId: Int = number

    override def getAttachedTokens: List[Attachable] = {
        val result = (for {
            t <- token.getAttachedTokens
            if (t.isInstanceOf[Token])
            tokenType = t.asInstanceOf[Token].getType
            if (tokenType == Token.NL || tokenType == Token.CCOMMENT || tokenType == Token.CPPCOMMENT)
        } yield {
                if (tokenType == Token.NL) {
                    NewLine(t.getId())
                } else {
                    Comment(t.getText, t.getId())
                }
            }).toList;
        result
    }

    override def getBlockId: String = {
        this.token.getBlockId
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
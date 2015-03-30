package de.fosd.typechef.parser.test.parsers

import de.fosd.typechef.error.Position
import de.fosd.typechef.featureexpr.{FeatureExpr, FeatureExprFactory}
import de.fosd.typechef.parser._

class MyToken(val text: String, val feature: FeatureExpr) extends ProfilingToken {
    def t() = text

    def getText = text

    def getFeature = feature

    def getPosition = new Position {
        def getFile = "stream"

        def getLine = 1

        def getColumn = 1
    }

    override def getAttachedTokens: List[Attachable] = List()

    override def toString = "\"" + text + "\"" + (if (!feature.isTautology()) feature else "")

    def isInteger: Boolean = false

    def isIdentifier: Boolean = false

    def isString: Boolean = false

    def isCharacter: Boolean = false
}

object EofToken extends MyToken("EOF", FeatureExprFactory.True)

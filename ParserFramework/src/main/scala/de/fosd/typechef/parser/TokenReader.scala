package de.fosd.typechef.parser

import de.fosd.typechef.error.Position
import de.fosd.typechef.featureexpr.{FeatureExpr, FeatureExprFactory}

import scala.collection.mutable
import scala.math.min

/**
 * reader of elements that each have a feature expression (that can be accessed with the getFeature function)
 *
 * @author kaestner
 *
 */
class TokenReader[+T <: AbstractToken, U](val tokens: List[T], val offst: Int, val context: U = null, eofToken: T) {
    def offset: Int = offst

    def first: T = if (!tokens.isEmpty) tokens.head else eofToken

    def rest: TokenReader[T, U] = new TokenReader(tokens.tail, offst + 1, context, eofToken)

    /** position is for user output only. do not rely on this value.
      * use offset for comparing position in tokenstream
      */
    def pos: Position = first.getPosition

    def attachedTokens(rightMost: Int, context: FeatureExpr, featureSolverCache: FeatureSolverCache): Iterable[Attachable] = {
        val inRange = range(rightMost, context, featureSolverCache)

        if (inRange.isEmpty) {
            List()
        } else if (inRange.size == 1) {
            inRange.head.getAttachedTokens
        } else {
            val result = mutable.ListBuffer[Attachable]()
            result ++= inRange.head.getAttachedTokens
            val partial = inRange.drop(1).dropRight(1)
            for (t <- partial) {
                result ++= t.getAttachedTokens.filter(_.isInstanceOf[Comment])
            }
            result ++= inRange.last.getAttachedTokens
            result
        }
    }

    def firstBiggerOrEqual(arr: Array[AbstractToken], target: Int): Int = {
        if (arr(arr.length - 1).getTokenId < target) {
            -1
        } else {
            var l = 0
            var r = arr.length - 1
            var res = -1

            while (l <= r) {
                val m = l + ((r - l) >> 1)
                if (arr(m).getTokenId >= target) {
                    res = m
                    r = m - 1
                } else {
                    l = m + 1
                }
            }
            res
        }
    }

    def lastSmaller(arr: Array[AbstractToken], target: Int): Int = {
        if (arr(0).getTokenId >= target) {
            -1
        } else {
            var l = 0
            var r = arr.length - 1
            var res = -1

            while (l <= r) {
                val m = l + ((r - l) >> 1)
                if (arr(m).getTokenId >= target) {
                    r = m - 1
                } else {
                    l = m + 1
                    res = m
                }
            }
            res
        }
    }

    def range(rightMost: Int, context: FeatureExpr, featureSolverCache: FeatureSolverCache): IndexedSeq[AbstractToken] = {
        val arr = tokens.toArray[AbstractToken]
        val start = firstBiggerOrEqual(arr, offset)
        if (start == -1) {
            return mutable.IndexedSeq.empty
        }
        val end = lastSmaller(arr, rightMost)
        if (end == -1) {
            return mutable.IndexedSeq.empty
        }

        for {
            i <- start to end
            if !featureSolverCache.mutuallyExclusive(context, arr(i).getFeature)
        } yield arr(i)
    }

    def blockId(rightMost: Int, context: FeatureExpr, featureSolverCache: FeatureSolverCache): String = {
        val inRange = range(rightMost, context, featureSolverCache)

        if (inRange.isEmpty) {
            return null
        } else {
            val shortest = inRange.minBy(e => e.getBlockId.length)
            shortest.getBlockId
        }
    }

    /** true iff there are no more elements in this reader
      */
    def atEnd: Boolean = tokens.isEmpty

    override def toString: String = {
        val out = new StringBuilder
        out ++= "TokenReader(" + pos.getLine + ","
        var currFeat: FeatureExpr = FeatureExprFactory.True

        for (tok <- tokens.slice(0, min(tokens.size, 50))) {
            var newFeat: FeatureExpr = tok.getFeature
            if (newFeat != currFeat) {
                out ++= "[ PC -> "
                out ++= newFeat.toString
                out ++= "] "
                currFeat = newFeat
            }
            out ++= tok.getText
            out ++= " "
        }
        out ++= ", ...)"
        out.toString
    }

    override def hashCode = tokens.hashCode

    override def equals(that: Any) = that match {
        case other: TokenReader[_, _] => (this.tokens eq other.tokens) && this.context == other.context
        case _ => false
    }

    def skipHidden(context: FeatureExpr, featureSolverCache: FeatureSolverCache): TokenReader[T, U] = {
        var result = this
        while (!result.atEnd && featureSolverCache.mutuallyExclusive(context, result.first.getFeature))
            result = result.rest
        result
    }

    def setContext(newContext: U): TokenReader[T, U] = if (context == newContext) this else new TokenReader(tokens, offst, newContext, eofToken)
}

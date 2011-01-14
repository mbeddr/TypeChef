package de.fosd.typechef.parser.test


import org.scalacheck._
import Gen._
import de.fosd.typechef.parser._
import de.fosd.typechef.featureexpr.FeatureExpr
import de.fosd.typechef.featureexpr.FeatureExpr._
import de.fosd.typechef.parser.test.parsers._

object JoinAutoCheck extends Properties("MultiParseResult") {

    abstract class A

    case class L(v: Int) extends A

    case class V(f: FeatureExpr, left: A, right: A) extends A

    val joinV = (f, a, b) => V(f, a, b)

    var lCounter: Int = 0
    var fCounter: Int = 0

    implicit def arbFeatureExpr: Arbitrary[jt.MultiParseResult[A]] =
        Arbitrary{
            def genL: L = {
                lCounter += 1;
                L(lCounter)
            }
            def genFeature: FeatureExpr = {
                fCounter += 1
                createDefinedExternal("f" + fCounter)
            }
            val genIn = oneOf(jt.in1, jt.in2, jt.in3)
            val genSuccess: Gen[jt.ParseResult[A]] =
                for {
                    in <- genIn
                } yield jt.Success(genL, in)
            val genFail: Gen[jt.ParseResult[A]] =
                for (in <- genIn) yield jt.Failure("generic message", in, List())

            def genSplit(size: Int): Gen[jt.MultiParseResult[A]] =
                for {
                    a <- genResult(size)
                    b <- genResult(size)
                    f <- genFeature
                } yield jt.SplittedParseResult(f, a, b)

            def genResult(size: Int): Gen[jt.MultiParseResult[A]] =
                if (size <= 0) oneOf(genSuccess, genFail)
                else Gen.frequency((5, genSuccess), (1, genFail), (20, genSplit(size / 4)))
            Gen.sized(sz => genSplit(sz))
        }

    val jt = new JoinTest()

    class JoinTest extends MultiFeatureParser {
        type Elem = MyToken
        type TypeContext = Any

        val tokenStream = new TokenReader[MyToken, TypeContext](List.fill(3)(new MyToken("_", base)), 0, null, EofToken)
        val in1 = tokenStream
        val in2 = in1.rest
        val in3 = in2.rest


    }

    private def collectL(x: jt.MultiParseResult[A]): Map[L, FeatureExpr] =
        x.toList(base).map(x => collectL(x._1, x._2)).foldRight(Map[L, FeatureExpr]())(_ ++ _)
    private def collectL(xf: FeatureExpr, xr: jt.ParseResult[A]): Map[L, FeatureExpr] =
        xr match {
            case jt.Success(r, in) => collectL(xf, r)
            case _ => Map()
        }
    private def collectL(xf: FeatureExpr, r: A): Map[L, FeatureExpr] = r match {
        case V(f, a, b) =>
            collectL(xf and f, a) ++ collectL(xf and (f.not), b)
        case x: L => Map(x -> xf)
    }

    //checks recursively that there is an according V with the required presence condition
    private def assertContainsL(result: jt.MultiParseResult[A], f: FeatureExpr, l: L) = {
        var found = false

        def find(feature: FeatureExpr, r: A, expectedFeature: FeatureExpr, expectedL: L): Boolean =
            ((r == expectedL) && (feature equivalentTo expectedFeature)) || (r match {
                case V(f, a, b) => find(feature and f, a, expectedFeature, expectedL) || find(feature and (f.not), b, expectedFeature, expectedL)
                case _ => false
            })

        for ((feature, r) <- result.toList(base))
            r match {
                case jt.Success(r, in) => found |= find(feature, r, f, l)
                case _ =>
            }
        found
    }

    property("unique offsets after join") = Prop.forAll({
        (x: jt.MultiParseResult[A]) => {
            var knownSuccessOffsets: Set[Int] = Set()
            var knownFailureOffsets: Set[Int] = Set()
            val joined = x.join(base, joinV)
            //            println(x.toList(base).size+" => "+joined.toList(base).size)
            joined.toList(base).forall(p => {
                p._2 match {
                    case jt.Success(_, _) => {
                        val v = knownSuccessOffsets contains (p._2.next.offset);
                        knownSuccessOffsets += p._2.next.offset;
                        !v
                    }
                    case _ => {
                        val v = knownFailureOffsets contains (p._2.next.offset);
                        knownFailureOffsets += p._2.next.offset;
                        !v
                    }
                }

            })
        }
    })

    property("maintain presence conditions for entries") = Prop.forAll({
        (x: jt.MultiParseResult[A]) =>
            val mBefore: Map[L, FeatureExpr] = collectL(x)

//                        println(x)
            val joined = x.join(base, joinV)
//                        println(".")

            mBefore.forall((p: (L, FeatureExpr)) => assertContainsL(joined, p._2, p._1))
    })

}

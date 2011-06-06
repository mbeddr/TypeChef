package de.fosd.typechef.typesystem


import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import de.fosd.typechef.featureexpr.FeatureExpr

@RunWith(classOf[JUnitRunner])
class ChoiceTypesTest extends FunSuite with ShouldMatchers with CTypes with CExprTyping with TestHelper {

    val fx = FeatureExpr.createDefinedExternal("X")
    val fy = FeatureExpr.createDefinedExternal("Y")

    test("alternatives in declarations") {
        val ast = getAST("""
         #ifdef X
         int a;
         #else
         double a;
         #endif

         #ifdef X
         int x;
         #endif
         #ifdef Y
         double x;
         #endif

         double
         #ifdef X
         b
         #else
         c
         #endif
         ;""")
        println(ast)
        val env = ast.defs.last.entry -> varEnv

        env("a") should be(CChoice(fx, CSignUnspecified(CInt()), CChoice(fx.not, CDouble(), CUndefined())))
        env("x") should be(CChoice(fx, CSignUnspecified(CInt()), CChoice(fy, CDouble(), CUndefined())))
        env("b") should be(CChoice(fx, CDouble(), CUndefined()))
    }


}
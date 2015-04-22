package de.fosd.typechef.parser.c;

/**
 * Created by berndkolb on 22/04/15.
 * @mbeddr - Added so that we have access to the scala object in MPS
 */
public class PrettyPrinterAccess {

    public static String printAst(AST node) {
        return PrettyPrinter.print(node);
    }
}

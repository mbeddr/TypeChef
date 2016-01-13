import java.io.{File, FileInputStream}
import java.util.{List => JavaList, Scanner}

import de.fosd.typechef.conditional._
import de.fosd.typechef.featureexpr.sat.{False, True}
import de.fosd.typechef.featureexpr.{FeatureExpr, FeatureExprFactory}
import de.fosd.typechef.lexer.SourceIdentifier
import de.fosd.typechef.parser.c._
import de.fosd.typechef.parser.{Comment, NewLine}

class CParserWrapper extends CParser {

    private val EMPTY_LIST = List()

    private def read(file: File): String = {
        val scanner = new Scanner(new FileInputStream(file))
        val buffer = new StringBuffer()

        while (scanner.hasNext) {
            buffer.append(scanner.nextLine() + "\n")
        }

        scanner.close()
        buffer.toString
    }

    def parseCode(file: File): String = {
        parseCode(file, EMPTY_LIST)
    }

    def parseCode(file: File, includes: List[String]): String = {
        parseCode(read(file), file.getParentFile.getAbsolutePath :: includes, new SourceIdentifier(file))
    }

    def parseCode(code: String): String = {
        parseCode(code, EMPTY_LIST)
    }

    def parseCode(code: String, includes: List[String]): String = {
        parseCode(code, includes, SourceIdentifier.BASE_SOURCE)
    }

    private def parseCode(code: String, includes: List[String], identifier: SourceIdentifier): String = {
        try {
            val featureModel = FeatureExprFactory.empty
            val lexerResult = lex(code.stripMargin, includes, featureModel, identifier)
            val parserResult = parse(lexerResult, this.translationUnit)
            println(parserResult)
            visitResultRecursive(parserResult)
        }
        catch {
            case e: ParseException => throw e
            case e: Any => {
                e.printStackTrace()
                throw new ParseException(e.getMessage)
            }
        }
    }

    private def visitResultRecursive(node: MultiParseResult[TranslationUnit]): String = {
        node match {
            case Success(node: TranslationUnit, nextInput) if nextInput.atEnd => {
                visitTranslationUnit(simplifyPresenceConditions(node))
            }
            case SplittedParseResult(feature: FeatureExpr, ra, rb) => {
                "Splitted Result:\n" + visitResultRecursive(ra) + "\n" + visitResultRecursive(rb)
            }
            case _ => {
                println(node)
                throw new ParseException(node.toString)
            }
        }
    }

    private def visitTranslationUnit(unit: TranslationUnit): String = {
        collectTrueOptions(unit.defs, visitExternalDef, "\n")
    }

    private def visitInitDeclarator(initDeclarator: InitDeclarator): String = {
        initDeclarator match {
            case InitDeclaratorI(declarator: Declarator, attributes: List[Opt[AttributeSpecifier]], initializer: Option[Initializer]) => {
                visitDeclarator(declarator) +
                    (if (initializer.isDefined) {
                        " = " + visitInitializer(initializer.get)
                    } else {
                        " No Initializer"
                    })
            }
            case InitDeclaratorE(declarator: Declarator, attributes: List[Opt[AttributeSpecifier]], e: Expr) => {
                visitDeclarator(declarator) + visitExpression(e)
            }

        }
    }

    private def visitDeclarator(declarator: Declarator): String = {
        declarator match {
            case AtomicNamedDeclarator(pointers: List[Opt[Pointer]], id: Id, extensions: List[Opt[DeclaratorExtension]]) => {
                collectTrueOptions(pointers, visitPointer) + visitExpression(id) + collectTrueOptions(extensions, visitDeclaratorExtension)
            }
            case NestedNamedDeclarator(pointers: List[Opt[Pointer]], nestedDecl: Declarator, extensions: List[Opt[DeclaratorExtension]], attr: List[Opt[AttributeSpecifier]]) => {
                "NestedNamedDeclarator"
            }
        }
    }

    private def visitDeclaratorExtension(extension: DeclaratorExtension): String = {
        extension match {
            case DeclArrayAccess(expr: Option[Expr]) => {
                "[" + collectTrueOption(expr, visitExpression) + "]"
            }
            case DeclParameterDeclList(parameterDecls: List[Opt[ParameterDeclaration]]) => {
                "(" + collectTrueOptions(parameterDecls, visitParameterDeclaration, ",") + ")"
            }
            case DeclIdentifierList(idList: List[Opt[Id]]) => {
                "(" + collectTrueOptions(idList, visitExpression) + ")"
            }
            case _ => {
                "Unknown DeclaratorExtension " + extension.getClass.getName
            }
        }
    }

    private def visitPointer(pointer: Pointer): String = {
        "*" + collectTrueOptions(pointer.specifier, visitSpecifier)
    }

    private def visitExternalDef(externalDef: ExternalDef): String = {
        visitAttachables(externalDef) + (
            externalDef match {
                case FunctionDef(specifiers: List[Opt[Specifier]], declarator: Declarator, parameters: List[Opt[OldParameterDeclaration]], statements: CompoundStatement) => {
                    collectTrueOptions(specifiers, visitSpecifier) + "  " + visitDeclarator(declarator) + " {\n" +
                        visitStatement(statements) + "\n}"
                }
                case Declaration(specifiers: List[Opt[Specifier]], initDeclarators: List[Opt[InitDeclarator]]) => {
                    collectTrueOptions(specifiers, visitSpecifier) + " " + collectTrueOptions(initDeclarators, visitInitDeclarator)
                }
                case Include(path: String, fromHeader: Boolean) => {
                    "#include " + path + " " + fromHeader
                }
                case Define(key: String, value: String, fromHeader: Boolean) => {
                    "#define " + key + " " + value
                }
                case Pragma(value : String, fromHeader : Boolean) => {
                    "#pragma " + value
                }
                case _ => {
                    "Unknown ExternalDef"
                }
            }
            )
    }


    private def visitOldParameterDeclaration(parameter: OldParameterDeclaration): String = {
        "Unknown OldParameterDeclaration " + parameter.getClass.getName
    }

    private def visitParameterDeclaration(parameter: ParameterDeclaration): String = {
        parameter match {
            case ParameterDeclarationD(specifiers: List[Opt[Specifier]], decl: Declarator, attr: List[Opt[AttributeSpecifier]]) => {
                collectTrueOptions(specifiers, visitSpecifier) + visitDeclarator(decl)
            }
            case PlainParameterDeclaration(specifiers: List[Opt[Specifier]], attr: List[Opt[AttributeSpecifier]]) => {
                collectTrueOptions(specifiers, visitSpecifier)
            }
            case _ => {
                "Unknown ParameterDeclaration " + parameter.getClass.getName
            }
        }
    }

    private def visitInitializer(initializer: Initializer): String = {
        (if (initializer.initializerElementLabel.isDefined) visitInitializerElementLabel(initializer.initializerElementLabel.get) else "") +
            visitExpression(initializer.expr)
    }

    private def visitInitializerElementLabel(initializerElementLabel: InitializerElementLabel): String = {
        "Unknown InitializerElementLabel"
    }

    private def visitStatement(statement: Statement): String = {
        visitAttachables(statement) + (
            statement match {
                case IfStatement(condition: Conditional[Expr], thenBranch: Conditional[Statement], elifs: List[Opt[ElifStatement]], elseBranch: Option[Conditional[Statement]]) => {
                    "if (" + collectTrueOptions(condition.toOptList, visitExpression) + ") {" +
                        collectTrueOptions(thenBranch.toOptList, visitStatement) +
                        "} " +
                        collectTrueOptions(elifs, visitStatement) +
                        (if (elseBranch.isDefined) {
                            "else {" +
                                collectTrueOptions(elseBranch.get.toOptList, visitStatement) +
                                "}"
                        } else "")
                }
                case ElifStatement(condition: Conditional[Expr], thenBranch: Conditional[Statement]) => {
                    "else if (" + collectTrueOptions(condition.toOptList, visitExpression) + ") {" +
                        collectTrueOptions(thenBranch.toOptList, visitStatement) +
                        "} "
                }
                case CompoundStatement(statements: List[Opt[Statement]]) => {
                    "{" + collectTrueOptions(statements, visitStatement, "\n") + "}"
                }
                case ReturnStatement(expr: Option[Expr]) => {
                    "return " + (if (expr.isDefined) visitExpression(expr.get) else "") + ";"
                }
                case ExprStatement(expr: Expr) => {
                    visitExpression(expr) + ";"
                }
                case DeclarationStatement(decl: Declaration) => {
                    visitExternalDef(decl) + ";"
                }
                case SwitchStatement(expr: Expr, s: Conditional[Statement]) => {
                    "switch (" + visitExpression(expr) + ") {" +
                        collectTrueOptions(s.toOptList, visitStatement, "\n") +
                        "}"
                }
                case CaseStatement(expr: Expr) => {
                    "case " + visitExpression(expr) + ":"
                }
                case DoStatement(expr: Expr, s: Conditional[Statement]) => {
                    "do {" +
                        collectTrueOptions(s.toOptList, visitStatement, "\n") +
                        "} while (" + visitExpression(expr) + ");"
                }
                case ForStatementDecl(expr1: Option[Declaration], expr2: Option[Expr], expr3: Option[Expr], s: Conditional[Statement]) => {
                    "for (" + collectTrueOption(expr1, visitExternalDef) + ";" + collectTrueOption(expr2, visitExpression) + ";" + collectTrueOption(expr3, visitExpression) + ") {" +
                        collectTrueOptions(s.toOptList, visitStatement, "\n") +
                        "}"
                }
                case ForStatement(expr1: Option[Expr], expr2: Option[Expr], expr3: Option[Expr], s: Conditional[Statement]) => {
                    "for (" + collectTrueOption(expr1, visitExpression) + ";" + collectTrueOption(expr2, visitExpression) + ";" + collectTrueOption(expr3, visitExpression) + ") {" +
                        collectTrueOptions(s.toOptList, visitStatement, "\n") +
                        "}"
                }
                case WhileStatement(expr: Expr, s: Conditional[Statement]) => {
                    "while (" + visitExpression(expr) + ") {" +
                        collectTrueOptions(s.toOptList, visitStatement, "\n") +
                        "}"
                }
                case GotoStatement(target: Expr) => {
                    "goto " + visitExpression(target) + ";"
                }
                case LabelStatement(id: Id, attribute: Option[AttributeSpecifier]) => {
                    visitExpression(id) + ":"
                }
                case BreakStatement() => {
                    "break;"
                }
                case ContinueStatement() => {
                    "continue";
                }
                case _ => {
                    "Unknown Statement " + statement.getClass.getName
                }
            }
            )
    }

    private def visitSpecifier(specifier: Specifier): String = {
        "(" + visitAttachables(specifier) + (
        specifier match {
            case VoidSpecifier(_) => {
                "void"
            }
            case IntSpecifier(_) => {
                "int32"
            }
            case ShortSpecifier(_) => {
                "int16"
            }
            case FloatSpecifier(_) => {
                "float"
            }
            case Int128Specifier(_) => {
                "long long"
            }
            case CharSpecifier(_) => {
                "int8"
            }
            case DoubleSpecifier(_) => {
                "double"
            }
            case LongSpecifier(_) => {
                "long"
            }
            case ConstSpecifier() => {
                "const"
            }
            case TypedefSpecifier() => {
                "typedef"
            }
            case EnumSpecifier(id: Option[Id], enumerators: Option[List[Opt[Enumerator]]], _) => {
                "enum" + collectTrueOption(id, visitExpression) + "{" +
                    (if (enumerators.isDefined)
                        collectTrueOptions(enumerators.get, visitEnumerator)
                    else "") +
                    "}"
            }
            case TypeDefTypeSpecifier(name: Id) => {
                visitExpression(name)
            }
            case ExternSpecifier() => {
                "extern"
            }
            case SignedSpecifier() => {
                "signed"
            }
            case UnsignedSpecifier() => {
                "unsigned"
            }
            case StaticSpecifier() => {
                "static"
            }
            case StructOrUnionSpecifier(isUnion: Boolean, name: Option[Id], members: Option[List[Opt[StructDeclaration]]], attributesBeforeBody: List[Opt[AttributeSpecifier]], attributesAfterBody: List[Opt[AttributeSpecifier]]) => {
                if (isUnion) {
                    "union " + collectTrueOption(name, visitExpression) + " {" +
                        (if (members.isDefined) {
                            collectTrueOptions(members.get, visitStructDeclaration, "\n")
                        } else {
                            ""
                        }) +
                        "};"
                } else {
                    "struct " + collectTrueOption(name, visitExpression) + " {" +
                        (if (members.isDefined) {
                            collectTrueOptions(members.get, visitStructDeclaration, "\n")
                        } else {
                            ""
                        }) +
                        "};"
                }
            }
            case _ => {
                "Unknown Specifier " + specifier.getClass.getName
            }
        }
        ) + ")"
    }

    private def visitAttachables(node: AST): String = {
        val tokens = node.tokens
        val a1 = (for {
            t <- tokens
        } yield
            t match {
                case NewLine(_) => "(" + t.id + ") nl \n"
                case Comment(text: String, _) => "(" + t.id + ") //" + text + "\n"
            }).mkString

        val a2 = node.blockId

        val result = a1 + " " + a2 + " "
        result
    }

    private def visitStructDeclaration(declaration: StructDeclaration): String = {
        "(" + visitAttachables(declaration) + (
        declaration.declaratorList + " " + declaration.qualifierList
        ) + ")"
    }

    private def visitEnumerator(enumerator: Enumerator): String = {
        visitExpression(enumerator.id) + " = " + collectTrueOption(enumerator.assignment, visitExpression)
    }

    private def visitExpression(expression: Expr): String = {
        "(" + visitAttachables(expression) + (
        expression match {
            case Constant(value: String) => {
                value
            }
            case Id(name: String, _) => {
                name //+ " " + token.isHeaderElement
            }
            case PostfixExpr(p: Expr, s: PostfixSuffix) => {
                visitExpression(p) + visitPostfixSuffix(s)
            }
            case ParensExpr(e: Expr) => {
                "(" + visitExpression(e) + ")"
            }
            case AssignExpr(target: Expr, operation: String, source: Expr) => {
                visitExpression(target) + " " + operation + " " + visitExpression(source)
            }
            case UnaryExpr(kind: String, e: Expr) => {
                kind + visitExpression(e)
            }
            case UnaryOpExpr(kind: String, e: Expr) => {
                kind + visitExpression(e)
            }
            case ExprList(exprs: List[Opt[Expr]]) => {
                collectTrueOptions(exprs, visitExpression, ",")
            }
            case PointerDerefExpr(e: Expr) => {
                "*" + visitExpression(e)
            }
            case PointerCreationExpr(e: Expr) => {
                "&" + visitExpression(e)
            }
            case CastExpr(typeName: TypeName, e: Expr) => {
                "(cast)" + visitExpression(e)
            }
            case NAryExpr(e: Expr, others: List[Opt[NArySubExpr]]) => {
                visitExpression(e) + collectTrueOptions(others, visitNArySubExpr)
            }
            case StringLit(name: List[Opt[String]]) => {
                collectTrueOptions(name, (e: String) => e)
            }
            case Type(p : ParameterDeclaration) => {
                visitParameterDeclaration(p)
            }
            case _ => {
                "Unknown Expression " + expression.getClass.getName
            }
        }
        ) + ")"
    }

    private def visitNArySubExpr(expr: NArySubExpr): String = {
        expr.op + " " + visitExpression(expr.e)
    }

    private def visitPostfixSuffix(expression: PostfixSuffix): String = {
        expression match {
            case ArrayAccess(expr: Expr) => {
                "[" + visitExpression(expr) + "]"
            }
            case FunctionCall(params: ExprList) => {
                "(" + visitExpression(params) + ")"
            }
            case SimplePostfixSuffix(value: String) => {
                value
            }
            case PointerPostfixSuffix(kind: String, id: Id) => {
                kind + visitExpression(id)
            }
            case _ => {
                "Unknown PostFix " + expression.getClass.getName
            }
        }
    }

    private def collectTrueOption[T](option: Option[T], mapper: (T => String)): String = {
        if (option.isDefined) {
            mapper(option.get)
        } else {
            ""
        }
    }

    private def collectTrueOptions[T](options: List[Opt[T]], mapper: (T => String), separator: String = " "): String = {
        " " + options.filter(e => e.condition != False).map(e => {
            /*"(condition: " + e.condition.toString + ") " +*/ mapper(e.entry)
        }).mkString(separator)
    }

    private def evaluate(expression: FeatureExpr): Boolean = {
        if (expression == True) {
            true
        } else {
            false
        }
    }

}

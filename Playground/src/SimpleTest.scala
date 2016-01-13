import java.io.File

object SimpleTest {

    def main(args: Array[String]): Unit = {

        val importer = new CParserWrapper

        val code =
            """
            """
        val file = new File("/Users/szabta/git/mbeddr.importer/code/testcode/localVarRef/localVarRef.c")
        val includes = List[String]()

        val result = importer.parseCode(file, includes)

        println("Result: ")
        println(result)
    }

}

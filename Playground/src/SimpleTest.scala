import java.io.File

object SimpleTest {

    def main(args: Array[String]): Unit = {

        val importer = new CParserWrapper

        val code =
            """
            """
        val file = new File("/Users/szabta/git/mbeddr.internal/code/testcode/standalone_test_headers/against_articulated_least.h")
        val includes = List[String]()

        val result = importer.parseCode(file, includes)

        println("Result: ")
        println(result)
    }

}

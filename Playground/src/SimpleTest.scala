import java.io.File

object SimpleTest {

    def main(args: Array[String]): Unit = {

        val importer = new CParserWrapper

        val code =
            """
            """
        val file = new File("/Users/szabta/git/mbeddr.internal/code/testcode/issues/accentCI85_a.c")
        val includes = List[String]("/Users/szabta/git/mbeddr.internal/code/testcode/issues/")

        val result = importer.parseCode(file, includes)

        println("Result: ")
        println(result)
    }

}

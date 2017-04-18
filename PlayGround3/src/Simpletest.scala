import java.io.File

object SimpleTest {

  def main(args: Array[String]): Unit = {

    val importer = new CParserWrapper

    val code =
      """
      """
    val file = new File("C:\\mbeddr.internal\\code\\testcode\\mergingFiles\\mergingFiles.c")
    val includes = List[String]("C:\\mbeddr.internal\\code\\testcode\\mergingFiles")

    val result = importer.parseCode(file, includes)

    println("Result: ")
    println(result)
  }

}

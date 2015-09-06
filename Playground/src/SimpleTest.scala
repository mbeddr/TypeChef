import java.io.File

object SimpleTest {

    def main(args: Array[String]): Unit = {

        val importer = new CParserWrapper

        val code =
            """
              void va(int value, ...) {
                  va_list ap;
                  char *enum_name;
                  int enum_val;

                  va_start(ap, value);
                  while(1) {
                      enum_name = va_arg(ap,char*);
                      enum_val = va_arg(ap,int);
                  }
                  va_end(ap);
              }
            """
        val file = new File("/Users/szabta/git/mbeddr.importer/code/testcode/pp/truefalse.c")
        val paths = List[String]()

        val result = importer.parseCode(code, paths)

        println("Result: ")
        println(result)
    }

}

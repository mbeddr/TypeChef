import java.io.File

object SimpleTest {

    def main(args: Array[String]): Unit = {

        val importer = new CParserWrapper

        val code =
            """
              const struct af_info af_info_lavcac3enc = {
                  .info = "runtime encode to ac3 using libavcodec",
                  .name = "lavcac3enc",
                  .open = af_open,
                  .priv_size = sizeof(struct af_ac3enc_s),
                  .priv_defaults = &(const struct af_ac3enc_s){
                      .cfg_add_iec61937_header = 1,
                      .cfg_bit_rate = 640,
                      .cfg_min_channel_num = 3,
                  },
                  .options = (const struct m_option[]) {
                      OPT_FLAG("tospdif", cfg_add_iec61937_header, 0),
                      OPT_CHOICE_OR_INT("bitrate", cfg_bit_rate, 0, 32, 640, null),
                      OPT_INTRANGE("minch", cfg_min_channel_num, 0, 2, 6),
                      {0}
                  },
              };
            """
        val file = new File("/Users/szabta/git/mbeddr.importer/code/testcode/core/types.h")
        val includes = List[String]()

        val result = importer.parseCode(file, includes)

        println("Result: ")
        println(result)
    }

}

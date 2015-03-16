package de.fosd.typechef.parser

trait WithComment {
    var comments : List[String] = null

    def setComment(comments : List[String]): Unit = {
        this.comments = comments
    }
}

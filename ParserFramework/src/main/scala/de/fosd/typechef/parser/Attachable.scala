package de.fosd.typechef.parser

trait WithAttachables {
    var tokens : List[Attachable] = null

    def attach(tokens : List[Attachable]): Unit = {
        this.tokens = tokens
    }
}

abstract class Attachable
case object NewLine extends Attachable
case class Comment(val text : String) extends Attachable

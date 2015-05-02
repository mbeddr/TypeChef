package de.fosd.typechef.parser

trait WithAttachables {
    var tokens : Iterable[Attachable] = null

    def attach(tokens : Iterable[Attachable]): Unit = {
        this.tokens = tokens
    }
}

abstract class Attachable
case object NewLine extends Attachable
case class Comment(val text : String) extends Attachable

trait WithBlockId {
    var blockId : String = null

    def setBlockId(blockId : String) : Unit = {
        this.blockId = blockId
    }
}

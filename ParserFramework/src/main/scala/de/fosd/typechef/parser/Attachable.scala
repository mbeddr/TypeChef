package de.fosd.typechef.parser

import java.util.concurrent.atomic.AtomicLong

trait WithAttachables {
    var tokens : Iterable[Attachable] = null

    def attach(tokens : Iterable[Attachable]): Unit = {
        this.tokens = tokens
    }
}

abstract class Attachable(val id : Long)
case class NewLine(_id : Long) extends Attachable(_id)
case class Comment(val text : String, _id : Long) extends Attachable(_id)

trait WithBlockId {
    var blockId : String = null

    def setBlockId(blockId : String) : Unit = {
        this.blockId = blockId
    }
}

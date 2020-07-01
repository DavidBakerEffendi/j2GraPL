package za.ac.sun.grapl.domain.stack.block

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.stack.BlockItem
import za.ac.sun.grapl.domain.stack.OperandItem
import za.ac.sun.grapl.domain.stack.StackItem

class StoreBlock(order: Int, label: Label?) : BlockItem(order, label!!) {
    var l: OperandItem? = null
    var r: StackItem? = null

    override fun toString(): String {
        return "STORE {order: $order, left: $l, right: $r, label:$label}"
    }
}
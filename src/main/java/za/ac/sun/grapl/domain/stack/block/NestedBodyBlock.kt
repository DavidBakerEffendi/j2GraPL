package za.ac.sun.grapl.domain.stack.block

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.enums.JumpState
import za.ac.sun.grapl.domain.stack.BlockItem

class NestedBodyBlock(order: Int, label: Label?, val position: JumpState) : BlockItem(order, label!!) {
    fun setLabel(label: Label?): NestedBodyBlock {
        return NestedBodyBlock(order, label, position)
    }

    override fun toString(): String {
        return "NESTED BODY {order: " + order + ", position: " + position.name + ", label: " + label + "}"
    }

}
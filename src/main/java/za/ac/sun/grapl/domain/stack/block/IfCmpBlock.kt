package za.ac.sun.grapl.domain.stack.block

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.enums.JumpState

class IfCmpBlock(order: Int, label: Label?, destination: Label, position: JumpState) : JumpBlock(order, label, destination, position) {
    override fun toString(): String {
        return "IFCMP {order: " + order + ", position: " + position.name + ", destination: " + destination + ", label: " + label + "}"
    }
}
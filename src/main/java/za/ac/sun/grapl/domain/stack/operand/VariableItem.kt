package za.ac.sun.grapl.domain.stack.operand

import za.ac.sun.grapl.domain.stack.OperandItem

class VariableItem(id: String?, type: String?) : OperandItem(id!!, type!!) {
    override fun toString(): String {
        return "VAR {id: $id, type: $type}"
    }
}
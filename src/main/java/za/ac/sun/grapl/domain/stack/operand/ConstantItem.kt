package za.ac.sun.grapl.domain.stack.operand

import za.ac.sun.grapl.domain.stack.OperandItem

class ConstantItem(id: String?, type: String?) : OperandItem(id!!, type!!) {
    override fun toString(): String {
        return "CONST {value: $id, type: $type}"
    }
}
package za.ac.sun.grapl.domain.stack

import java.util.*

abstract class OperandItem(val id: String, val type: String) : StackItem {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as OperandItem
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

}
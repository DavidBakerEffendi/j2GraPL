package za.ac.sun.grapl.domain.meta

import org.objectweb.asm.Label

data class LineInfo(val lineNumber: Int) {

    val associatedLabels = emptyList<Label>().toMutableList()

    override fun toString(): String {
        return "$lineNumber: $associatedLabels"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineInfo

        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        return lineNumber
    }
}
package za.ac.sun.grapl.domain.meta

import org.objectweb.asm.Label

data class LineInfo(val lineNumber: Int, val label: Label) {
    override fun toString(): String {
        return "$lineNumber: $label"
    }
}
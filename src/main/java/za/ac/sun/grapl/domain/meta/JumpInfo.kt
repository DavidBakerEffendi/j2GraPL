package za.ac.sun.grapl.domain.meta

import org.objectweb.asm.Label

data class JumpInfo(val jumpOp: String, val destLabel: Label, val currLabel: Label, val pseudoLineNo: Int) {
    override fun toString(): String {
        return "[$pseudoLineNo] $jumpOp @ $currLabel -> $destLabel"
    }
}
package za.ac.sun.grapl.domain.meta

import org.objectweb.asm.Label

data class JumpInfo(val jumpOp: String, val destLabel: Label, val currLabel: Label) {
    override fun toString(): String {
        return "$jumpOp @ $currLabel -> $destLabel"
    }
}
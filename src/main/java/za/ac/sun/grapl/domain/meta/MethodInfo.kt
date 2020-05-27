package za.ac.sun.grapl.domain.meta

import org.objectweb.asm.Label
import za.ac.sun.grapl.util.ASMParserUtil

data class MethodInfo(
        val methodName: String,
        val methodSignature: String,
        val access: Int,
        var lineNumber: Int? = -1
) {
    private val allVariables = mutableListOf<LocalVarInfo>()
    private val allJumps = HashSet<JumpInfo>()
    private val allLabels = mutableListOf<LineInfo>()

    fun addVariable(frameId: Int) {
        allVariables.add(LocalVarInfo(frameId))
    }

    fun addVarDebugInfo(frameId: Int, debugName: String, descriptor: String, startLabel: Label, endLabel: Label) {
        val existingVar = allVariables.find { it.frameId == frameId }
        if (existingVar != null) {
            existingVar.debugName = debugName
            existingVar.descriptor = descriptor
            existingVar.startLabel = startLabel
            existingVar.endLabel = endLabel
        }
    }

    fun getVariable(frameId: Int): LocalVarInfo? {
        return allVariables.find { it.frameId == frameId }
    }

    fun addJump(jumpOp: String, label: Label) {
        allJumps.add(JumpInfo(jumpOp, label))
    }

    fun addLabel(lineNumber: Int, label: Label) {
        allLabels.add(LineInfo(lineNumber, label))
    }

    override fun toString(): String {
        return "$lineNumber: ${ASMParserUtil.determineModifiers(access)} $methodName $methodSignature"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodInfo

        if (methodName != other.methodName) return false
        if (methodSignature != other.methodSignature) return false
        if (access != other.access) return false

        return true
    }

    override fun hashCode(): Int {
        var result = methodName.hashCode()
        result = 31 * result + methodSignature.hashCode()
        result = 31 * result + access
        return result
    }
}
package za.ac.sun.grapl.domain.meta

import za.ac.sun.grapl.util.ASMParserUtil

data class MethodInfo(
        val methodName: String,
        val methodSignature: String,
        val access: Int,
        var lineNumber: Int? = -1
) {
    private val allVariables = mutableListOf<LocalVarInfo>()

    fun addVariable(frameId: Int, debugName: String?) {
        val existingVar = allVariables.find { it.frameId == frameId }
        if (existingVar != null) existingVar.debugName = debugName
        else allVariables.add(LocalVarInfo(frameId, debugName))
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
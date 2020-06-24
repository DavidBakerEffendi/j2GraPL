package za.ac.sun.grapl.controllers

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.meta.JumpInfo
import za.ac.sun.grapl.domain.meta.LineInfo
import za.ac.sun.grapl.domain.meta.LocalVarInfo
import za.ac.sun.grapl.domain.models.vertices.BlockVertex
import za.ac.sun.grapl.util.ASMParserUtil

data class MethodInfoController(
        val methodName: String,
        val methodSignature: String,
        val access: Int,
        var lineNumber: Int? = -1
) : OpStackController() {

    private val allVariables = mutableListOf<LocalVarInfo>()
    private val allJumps = LinkedHashSet<JumpInfo>()
    private val ternaryPairs = HashMap<JumpInfo, JumpInfo>()
    private val jumpRoot = HashMap<Int, String>()

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

    fun addJump(jumpOp: String, destLabel: Label, currentLabel: Label) =
            allJumps.add(JumpInfo(jumpOp, destLabel, currentLabel, super.pseudoLineNo))


    fun addTernaryPair(gotoOp: String, destLabel: Label, currentLabel: Label) {
        val lastJump = allJumps.last()
        addJump(gotoOp, destLabel, currentLabel)
        ternaryPairs[lastJump] = JumpInfo(gotoOp, destLabel, currentLabel, super.pseudoLineNo)
    }

    fun getAssociatedJumps(pseudoLineNo: Int): MutableList<JumpInfo> {
        val assocLineInfo = getLineInfo(pseudoLineNo) ?: return emptyList<JumpInfo>().toMutableList()
        return allJumps.filter { jInfo: JumpInfo -> assocLineInfo.associatedLabels.contains(jInfo.destLabel) }.toMutableList()
    }

    fun getPseudoLineNumber(label: Label): Int = getLineInfo(label)?.pseudoLineNumber ?: -1

    fun upsertJumpRootAtLine(lineNumber: Int, name: String) = if (jumpRoot.containsKey(lineNumber)) jumpRoot.replace(lineNumber, name) else jumpRoot.put(lineNumber, name)

    fun getJumpRootName(currentLabel: Label?) = jumpRoot.getOrDefault(currentLabel?.let { getPseudoLineNumber(it) }, "IF")

    fun isLabelAssociatedWithLoops(label: Label): Boolean {
        val loopNames = listOf("WHILE", "DO_WHILE", "FOR")
        val rootName = jumpRoot.getOrDefault(getPseudoLineNumber(label), "IF")
        return !loopNames.none { name -> name == rootName }
    }

    fun findJumpLineBasedOnDestLabel(destLabel: Label): Int? {
        // Find associated labels with the dest label
        val associatedLabels = getLineInfo(destLabel)?.associatedLabels ?: return null
        // Match this with a jump
        val matchedJump = allJumps.find { jumpInfo -> associatedLabels.contains(jumpInfo.destLabel) } ?: return null
        // Get the current line of the jump current line
        return getLineInfo(matchedJump.currLabel)?.pseudoLineNumber
    }

    fun isJumpVertexAssociatedWithGivenLine(jumpBlockPseudoLineNo: Int, lineNumber: Int) =
            !getAssociatedJumps(jumpBlockPseudoLineNo).none { jumpInfo -> getPseudoLineNumber(jumpInfo.currLabel) == lineNumber }


    override fun toString() = "$lineNumber: ${ASMParserUtil.determineModifiers(access)} $methodName $methodSignature"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodInfoController

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
package za.ac.sun.grapl.controllers

import org.objectweb.asm.Label
import za.ac.sun.grapl.domain.meta.ClassInfo
import za.ac.sun.grapl.domain.meta.JumpInfo
import za.ac.sun.grapl.domain.meta.MethodInfo

class ClassController(
        className: String,
        namespace: String,
        access: Int
) : AbstractController() {
    private val allJumps = HashSet<JumpInfo>()
    private val allLabels = mutableListOf<Label>()
    private var currentClass: ClassInfo = ClassInfo(className, namespace, access)
    private val classMethods = mutableListOf<MethodInfo>()

    fun addMethod(methodName: String, methodSignature: String, access: Int, lineNumber: Int) {
        classMethods.add(MethodInfo(methodName, methodSignature, access, lineNumber))
    }

    fun addLabel(label: Label) {
        allLabels.add(label)
    }

    fun pushJump(jumpOp: String, label: Label) {
        allJumps.add(JumpInfo(jumpOp, label))
    }

    override fun toString(): String {
        return """
            Class: $currentClass
            Methods: $classMethods
            Jumps: $allJumps
            """.trimIndent()
    }
}
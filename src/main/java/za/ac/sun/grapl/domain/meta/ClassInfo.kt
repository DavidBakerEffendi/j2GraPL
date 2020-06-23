package za.ac.sun.grapl.domain.meta

import za.ac.sun.grapl.controllers.AbstractController
import za.ac.sun.grapl.util.ASMParserUtil
import kotlin.properties.Delegates

class ClassInfo(
        val className: String,
        val namespace: String,
        private val access: Int
) : AbstractController {
    private val classMethods = mutableListOf<MethodInfo>()

    fun addMethod(methodName: String, methodSignature: String, access: Int, lineNumber: Int): MethodInfo {
        val methodInfo = MethodInfo(methodName, methodSignature, access, lineNumber)
        classMethods.add(methodInfo)
        return methodInfo
    }

    fun getMethod(methodName: String, methodSignature: String, access: Int): MethodInfo? {
        val hashCode = 31 * (31 * methodName.hashCode() + methodSignature.hashCode()) + access.hashCode()
        return classMethods.find { methodInfo -> methodInfo.hashCode() ==  hashCode }
    }

    fun clear() {
        classMethods.clear()
    }

    override fun toString(): String {
        return "${ASMParserUtil.determineModifiers(access, className)} ${namespace}.${className}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassInfo

        if (className != other.className) return false
        if (namespace != other.namespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + namespace.hashCode()
        return result
    }
}
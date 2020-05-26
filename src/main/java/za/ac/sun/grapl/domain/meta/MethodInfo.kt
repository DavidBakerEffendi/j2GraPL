package za.ac.sun.grapl.domain.meta

data class MethodInfo(val methodName: String, val methodSignature: String, val access: Int, var lineNumber: Int? = -1) {
    override fun toString(): String {
        return "${methodName}.${methodSignature} @ $lineNumber (access: $access)"
    }
}
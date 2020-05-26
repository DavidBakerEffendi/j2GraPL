package za.ac.sun.grapl.domain.meta

data class ClassInfo(val className: String, val namespace: String, val access: Int) {
    override fun toString(): String {
        return "${namespace}.${className} (access: $access)"
    }
}
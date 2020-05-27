package za.ac.sun.grapl.domain.meta

data class LocalVarInfo(
        val frameId: Int,
        var debugName: String? = null
) {
    override fun toString(): String {
        return "LOCAL VAR $frameId (name $debugName)"
    }
}
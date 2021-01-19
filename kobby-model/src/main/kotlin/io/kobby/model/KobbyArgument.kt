package io.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyArgument(
    val schema: KobbySchema,
    val node: KobbyNode,
    val field: KobbyField,

    val name: String,
    val nativeName: String,
    val type: KobbyType,
    val comments: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyArgument
        return field == other.field && name == other.name
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "$nativeName: $type"
    }
}

@KobbyBuilder
class KobbyArgumentBuilder(
    schema: KobbySchema,
    node: KobbyNode,
    field: KobbyField,
    name: String,
    nativeName: String,
    type: KobbyType
) {
    private val comments = mutableListOf<String>()
    private val result = KobbyArgument(schema, node, field, name, nativeName, type, comments)

    fun addComment(comment: String) {
        comments += comment
    }

    fun build(): KobbyArgument = result
}
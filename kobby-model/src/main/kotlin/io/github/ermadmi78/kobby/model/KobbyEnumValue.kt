package io.github.ermadmi78.kobby.model

/**
 * Created on 21.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyEnumValue internal constructor(
    val schema: KobbySchema,
    val node: KobbyNode,

    val name: String,
    val comments: List<String>
) {
    fun comments(action: (String) -> Unit) = comments.forEach(action)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyEnumValue
        return node == other.node && name == other.name
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return name
    }
}

@KobbyScope
class KobbyEnumValueScope internal constructor(
    val schema: KobbySchema,
    val node: KobbyNode,
    name: String
) {
    private val comments = mutableListOf<String>()
    private val enumValue = KobbyEnumValue(schema, node, name, comments)

    fun addComment(comment: String) {
        comments += comment
    }

    fun build(): KobbyEnumValue = enumValue
}
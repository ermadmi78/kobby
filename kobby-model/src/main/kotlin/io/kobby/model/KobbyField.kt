package io.kobby.model

import io.kobby.model.KobbyNodeKind.INTERFACE
import io.kobby.model.KobbyNodeKind.OBJECT

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyField internal constructor(
    val schema: KobbySchema,
    val node: KobbyNode,

    val name: String,
    val type: KobbyType,
    private val required: Boolean,
    private val default: Boolean,
    val comments: List<String>,
    val arguments: Map<String, KobbyArgument>
) {
    fun comments(action: (String) -> Unit) = comments.forEach(action)
    fun arguments(action: (KobbyArgument) -> Unit) = arguments.values.forEach(action)

    fun findOverriddenField(): KobbyField? = if (node.kind == OBJECT || node.kind == INTERFACE) {
        node.implements.values.asSequence().map { it.fields[name] }.filterNotNull().firstOrNull()
    } else {
        null
    }

    fun isOverride(): Boolean = findOverriddenField() != null

    fun isRequired(): Boolean = findOverriddenField()?.isRequired() ?: (type.isId() || required)

    fun isDefault(): Boolean = findOverriddenField()?.isDefault() ?: default

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyField
        return node == other.node && name == other.name
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "$name: $type"
    }
}

@KobbyScope
class KobbyFieldScope internal constructor(
    val schema: KobbySchema,
    val node: KobbyNode,
    name: String,
    type: KobbyType,
    required: Boolean,
    default: Boolean
) {
    private val comments = mutableListOf<String>()
    private val arguments = mutableMapOf<String, KobbyArgument>()
    private val field = KobbyField(
        schema,
        node,
        name,
        type,
        required,
        default,
        comments,
        arguments
    )

    fun addComment(comment: String) {
        comments += comment
    }

    fun addArgument(
        name: String,
        type: KobbyType,
        block: KobbyArgumentScope.() -> Unit
    ) = KobbyArgumentScope(schema, node, field, name, type).apply(block).build().also {
        arguments[it.name] = it
    }

    fun build(): KobbyField = field
}
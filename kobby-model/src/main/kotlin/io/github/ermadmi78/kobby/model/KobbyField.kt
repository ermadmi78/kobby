package io.github.ermadmi78.kobby.model

import io.github.ermadmi78.kobby.model.KobbyNodeKind.*

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
    val hasDefaultValue: Boolean,
    val number: Int,
    private val primaryKey: Boolean,
    private val required: Boolean,
    private val default: Boolean,
    private val selection: Boolean,
    private val _comments: List<String>,
    val arguments: Map<String, KobbyArgument>
) {
    val comments: List<String> by lazy {
        if (_comments.isNotEmpty()) {
            _comments
        } else {
            overriddenField?.comments ?: emptyList()
        }
    }

    fun comments(action: (String) -> Unit) = comments.forEach(action)
    fun arguments(action: (KobbyArgument) -> Unit) = arguments.values.forEach(action)

    val overriddenField: KobbyField? by lazy {
        if (node.kind == OBJECT || node.kind == INTERFACE) {
            node.implements.values.asSequence().map { it.fields[name] }.filterNotNull().firstOrNull()
        } else {
            null
        }
    }

    val isOverride: Boolean get() = overriddenField != null

    val isProperty: Boolean get() = arguments.isEmpty() && (type.node.kind == SCALAR || type.node.kind == ENUM)

    val isPrimaryKey: Boolean get() = isProperty && (overriddenField?.isPrimaryKey ?: primaryKey)

    val isRequired: Boolean get() = isProperty && (overriddenField?.isRequired ?: primaryKey || required)

    val isDefault: Boolean get() = isProperty && (overriddenField?.isDefault ?: default)

    val isSelection: Boolean by lazy {
        overriddenField?.isSelection ?: (selection && arguments.values.any { it.isInitialized })
    }

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
    hasDefaultValue: Boolean,
    number: Int,
    primaryKey: Boolean,
    required: Boolean,
    default: Boolean,
    selection: Boolean
) {
    private val comments = mutableListOf<String>()
    private val arguments = mutableMapOf<String, KobbyArgument>()
    private val field = KobbyField(
        schema,
        node,
        name,
        type,
        hasDefaultValue,
        number,
        primaryKey,
        required,
        default,
        selection,
        comments,
        arguments
    )

    fun addComment(comment: String) {
        comments += comment
    }

    fun addArgument(
        name: String,
        type: KobbyType,
        hasDefaultValue: Boolean,
        block: KobbyArgumentScope.() -> Unit
    ) = KobbyArgumentScope(schema, node, field, name, type, hasDefaultValue).apply(block).build().also {
        arguments[it.name] = it
    }

    fun build(): KobbyField = field
}
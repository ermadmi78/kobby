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
    val defaultValue: KobbyLiteral?,
    val number: Int,
    private val primaryKey: Boolean,
    private val required: Boolean,
    private val default: Boolean,
    private val selection: Boolean,
    private val resolve: Boolean,
    private val _comments: List<String>,
    val arguments: Map<String, KobbyArgument>
) {
    val hasDefaultValue: Boolean get() = defaultValue != null

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

    val isRequired: Boolean get() = isProperty && (overriddenField?.isRequired ?: (primaryKey || required))

    val isDefault: Boolean get() = isProperty && (overriddenField?.isDefault ?: default)

    val isSelection: Boolean by lazy {
        overriddenField?.isSelection ?: (selection && arguments.values.any { it.isInitialized })
    }

    val isResolve: Boolean by lazy {
        node.isOperation || (overriddenField?.isResolve ?: (resolve || arguments.isNotEmpty()))
    }

    internal fun validate(warnings: MutableList<String>) {
        if (!primaryKey && !required && !default && !selection && !resolve) {
            return
        }
        val topOverriddenField = findTopOverriddenField(node)

        var counter = 0
        if (primaryKey) {
            counter++
            warnings.addPropertyWarning(KobbyDirective.PRIMARY_KEY)
            topOverriddenField?.takeIf { !it.primaryKey }?.also {
                warnings.addOverriddenWarning(KobbyDirective.PRIMARY_KEY, it)
            }
        }

        if (required) {
            counter++
            warnings.addPropertyWarning(KobbyDirective.REQUIRED)
            topOverriddenField?.takeIf { !it.required }?.also {
                warnings.addOverriddenWarning(KobbyDirective.REQUIRED, it)
            }
        }

        if (default) {
            counter++
            warnings.addPropertyWarning(KobbyDirective.DEFAULT)
            topOverriddenField?.takeIf { !it.default }?.also {
                warnings.addOverriddenWarning(KobbyDirective.DEFAULT, it)
            }
        }

        if (counter > 1) {
            warnings += "Restriction violated [${node.name}.$name]: " +
                    "The field is marked with several directives at once - " +
                    "@${KobbyDirective.DEFAULT}, @${KobbyDirective.REQUIRED}, @${KobbyDirective.PRIMARY_KEY}, " +
                    "the behavior of the Kobby Plugin is undefined!"
        }

        if (selection) {
            if (!arguments.values.any { it.isInitialized }) {
                warnings += "Restriction violated [${node.name}.$name]: " +
                        "The @${KobbyDirective.SELECTION} directive can only be applied to a field that " +
                        "contains optional arguments - nullable arguments or arguments with default value."
            }
            topOverriddenField?.takeIf { !it.selection }?.also {
                warnings.addOverriddenWarning(KobbyDirective.SELECTION, it)
            }
        }

        if (resolve && !node.isOperation) {
            topOverriddenField?.takeIf { !it.resolve }?.also {
                warnings.addOverriddenWarning(KobbyDirective.RESOLVE, it)
            }
        }
    }

    private fun findTopOverriddenField(startNode: KobbyNode): KobbyField? =
        overriddenField?.takeIf { it.node != startNode }?.let {
            it.findTopOverriddenField(startNode) ?: it
        }

    private fun MutableList<String>.addPropertyWarning(directive: String) {
        if (arguments.isNotEmpty()) {
            this += "Restriction violated [${node.name}.$name]: " +
                    "The [@$directive] directive can only be applied to a field with no arguments."
        }

        if (type.node.kind != SCALAR && type.node.kind != ENUM) {
            this += "Restriction violated [${node.name}.$name]: " +
                    "The [@$directive] directive can only be applied to a field that returns a scalar or enum type."
        }
    }

    private fun MutableList<String>.addOverriddenWarning(directive: String, topField: KobbyField) {
        this += "Restriction violated [${node.name}.$name]: " +
                "The [@$directive] directive cannot be applied to overridden fields. " +
                "Please, apply [@$directive] directive to [${topField.node.name}.${topField.name}] field."
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
    defaultValue: KobbyLiteral?,
    number: Int,
    primaryKey: Boolean,
    required: Boolean,
    default: Boolean,
    selection: Boolean,
    resolve: Boolean
) {
    private val comments = mutableListOf<String>()
    private val arguments = mutableMapOf<String, KobbyArgument>()
    private val field = KobbyField(
        schema,
        node,
        name,
        type,
        defaultValue,
        number,
        primaryKey,
        required,
        default,
        selection,
        resolve,
        comments,
        arguments
    )

    fun addComment(comment: String) {
        comments += comment
    }

    fun addArgument(
        name: String,
        type: KobbyType,
        defaultValue: KobbyLiteral?,
        block: KobbyArgumentScope.() -> Unit
    ) = KobbyArgumentScope(schema, node, field, name, type, defaultValue).apply(block).build().also {
        arguments[it.name] = it
    }

    fun build(): KobbyField = field
}
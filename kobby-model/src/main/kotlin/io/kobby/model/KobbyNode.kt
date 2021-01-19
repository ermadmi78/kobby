package io.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyNode(
    val schema: KobbySchema,

    val name: String,
    val nativeName: String,
    val kind: KobbyNodeKind,
    private val _implements: List<String>,
    val comments: List<String>,
    val enumValues: List<String>,
    val fields: Map<String, KobbyField>
) {
    val implements: Map<String, KobbyNode> by lazy {
        _implements.asSequence()
            .map { schema.interfaces[it] }
            .filterNotNull()
            .map { it.name to it }
            .toMap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyNode
        return schema == other.schema && name == other.name
    }

    override fun hashCode(): Int {
        var result = schema.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind.name.toLowerCase()} $nativeName"
    }
}

enum class KobbyNodeKind {
    SCALAR,
    QUERY,
    MUTATION,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT
}

@KobbyBuilder
class KobbyNodeBuilder(
    private val schema: KobbySchema,
    name: String,
    nativeName: String,
    kind: KobbyNodeKind
) {
    private val _implements = mutableListOf<String>()
    private val comments = mutableListOf<String>()
    private val enumValues = mutableListOf<String>()
    private val fields = mutableMapOf<String, KobbyField>()
    private val result = KobbyNode(schema, name, nativeName, kind, _implements, comments, enumValues, fields)

    fun addImplements(interfaceName: String) {
        _implements += interfaceName
    }

    fun addComment(comment: String) {
        comments += comment
    }

    fun addEnumValue(name: String) {
        enumValues += name
    }

    fun addField(
        name: String, nativeName: String,
        type: KobbyType,
        required: Boolean,
        default: Boolean,
        block: KobbyFieldBuilder.() -> Unit
    ) = KobbyFieldBuilder(schema, result, name, nativeName, type, required, default).apply(block).build().also {
        fields[it.name] = it
    }

    fun build(): KobbyNode = result
}
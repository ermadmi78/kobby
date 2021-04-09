package io.github.ermadmi78.kobby.model

/**
 * Created on 19.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
sealed class KobbyType {
    abstract val schema: KobbySchema

    internal abstract val nodeName: String

    abstract val node: KobbyNode

    abstract val nullable: Boolean

    abstract val list: Boolean

    abstract val sourceName: String

    val nestedOrNull: KobbyType?
        get() = when (this) {
            is KobbyListType -> nested
            else -> null
        }
}

class KobbyListType(
    val nested: KobbyType,
    override val nullable: Boolean
) : KobbyType() {
    override val schema: KobbySchema get() = nested.schema
    override val nodeName: String get() = nested.nodeName
    override val node: KobbyNode get() = nested.node
    override val list: Boolean get() = true
    override val sourceName: String
        get() = "[${nested.sourceName}]".let {
            if (nullable) it else "$it!"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyListType
        return nested == other.nested && nullable == other.nullable
    }

    override fun hashCode(): Int {
        var result = nested.hashCode()
        result = 31 * result + nullable.hashCode()
        return result
    }

    override fun toString(): String = "[$nested]${if (nullable) "" else "!"}"
}

class KobbyNodeType(
    override val schema: KobbySchema,
    override val nodeName: String,
    override val nullable: Boolean
) : KobbyType() {
    override val node: KobbyNode by lazy {
        schema.all[nodeName]!!
    }
    override val list: Boolean get() = false
    override val sourceName: String
        get() = nodeName.let {
            if (nullable) it else "$it!"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KobbyNodeType
        return node == other.node && nullable == other.nullable
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + nullable.hashCode()
        return result
    }

    override fun toString(): String = "$node${if (nullable) "" else "!"}"
}
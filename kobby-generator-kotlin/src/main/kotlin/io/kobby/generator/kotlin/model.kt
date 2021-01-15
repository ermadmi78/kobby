package io.kobby.generator.kotlin

import java.io.File
import java.io.IOException
import java.io.Serializable
import java.nio.file.Path

/**
 * Created on 18.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class Decoration(val prefix: String?, val postfix: String?)

data class KotlinGeneratorLayout(
    val scalars: Map<String, KotlinType>,
    val context: KotlinContextLayout,
    val dto: KotlinDtoLayout,
    val entity: KotlinEntityLayout,
    val impl: KotlinImplLayout
)

class KotlinContextLayout(
    packageName: String,
    val name: String,
    val decoration: Decoration
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinDtoLayout(
    packageName: String,
    val decoration: Decoration,
    val jackson: KotlinDtoJacksonLayout,
    val builder: KotlinDtoBuilderLayout,
    val graphql: KotlinDtoGraphQLLayout
) {
    val packageName: String = packageName.validateKotlinPath()
}

data class KotlinDtoJacksonLayout(
    val enabled: Boolean
)

data class KotlinDtoBuilderLayout(
    val enabled: Boolean,
    val decoration: Decoration
)

class KotlinDtoGraphQLLayout(
    val enabled: Boolean,
    packageName: String,
    val decoration: Decoration
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinEntityLayout(
    val enabled: Boolean,
    packageName: String,
    val projection: KotlinEntityProjectionLayout
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinEntityProjectionLayout(
    val decoration: Decoration,
    val argument: String,
    val withDecoration: Decoration,
    val withoutDecoration: Decoration
)

class KotlinImplLayout(
    packageName: String,
    val decoration: Decoration
) {
    val packageName: String = packageName.validateKotlinPath()
}

interface KotlinFile {
    @Throws(IOException::class)
    fun writeTo(out: Appendable)

    @Throws(IOException::class)
    fun writeTo(directory: Path)

    @Throws(IOException::class)
    fun writeTo(directory: File)
}

class KotlinType(
    /** A fully-qualified package name. For example: kotlin.collections */
    packageName: String,

    /** A fully-qualified class name. For example: Map.Entry */
    className: String,

    val allowNull: Boolean = false,

    /** List of generics */
    val generics: List<KotlinType> = listOf()
) : Serializable {
    val packageName: String = packageName.validateKotlinPath()

    val className: String = className.validateKotlinPath()

    fun nullable(): KotlinType =
        if (allowNull) this else KotlinType(packageName, className, true, generics)

    fun parameterize(vararg arguments: KotlinType): KotlinType =
        if (arguments.isEmpty()) this else KotlinType(packageName, className, allowNull, generics + arguments)

    fun nested(nestedClassName: String): KotlinType =
        KotlinType(packageName, "$className.$nestedClassName", allowNull, generics)


    override fun toString(): String {
        return "$packageName.$className" +
                (if (generics.isNotEmpty()) "<${generics.joinToString { it.toString() }}>" else "") +
                (if (allowNull) "?" else "")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinType
        return packageName == other.packageName &&
                className == other.className &&
                allowNull == other.allowNull &&
                generics == other.generics
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + allowNull.hashCode()
        result = 31 * result + generics.hashCode()
        return result
    }
}

object KotlinTypes {
    val ANY by lazy { KotlinType("kotlin", "Any") }
    val NUMBER by lazy { KotlinType("kotlin", "Number") }
    val BYTE by lazy { KotlinType("kotlin", "Byte") }
    val SHORT by lazy { KotlinType("kotlin", "Short") }
    val INT = KotlinType("kotlin", "Int")
    val LONG = KotlinType("kotlin", "Long")
    val FLOAT by lazy { KotlinType("kotlin", "Float") }
    val DOUBLE = KotlinType("kotlin", "Double")
    val CHAR by lazy { KotlinType("kotlin", "Char") }
    val STRING = KotlinType("kotlin", "String")
    val CHAR_SEQUENCE by lazy { KotlinType("kotlin", "CharSequence") }
    val BOOLEAN = KotlinType("kotlin", "Boolean")

    val ENUM by lazy { KotlinType("kotlin", "Enum") }
    val THROWABLE by lazy { KotlinType("kotlin", "Throwable") }
    val COMPARABLE by lazy { KotlinType("kotlin", "Comparable") }

    val ITERABLE by lazy { KotlinType("kotlin.collections", "Iterable") }
    val COLLECTION by lazy { KotlinType("kotlin.collections", "Collection") }
    val LIST by lazy { KotlinType("kotlin.collections", "List") }
    val SET by lazy { KotlinType("kotlin.collections", "Set") }
    val MAP by lazy { KotlinType("kotlin.collections", "Map") }

    val MUTABLE_ITERABLE by lazy { KotlinType("kotlin.collections", "MutableIterable") }
    val MUTABLE_COLLECTION by lazy { KotlinType("kotlin.collections", "MutableCollection") }
    val MUTABLE_LIST by lazy { KotlinType("kotlin.collections", "MutableList") }
    val MUTABLE_SET by lazy { KotlinType("kotlin.collections", "MutableSet") }
    val MUTABLE_MAP by lazy { KotlinType("kotlin.collections", "MutableMap") }

    val ARRAY by lazy { KotlinType("kotlin", "Array") }
    val BOOLEAN_ARRAY by lazy { KotlinType("kotlin", "BooleanArray") }
    val BYTE_ARRAY by lazy { KotlinType("kotlin", "ByteArray") }
    val CHAR_ARRAY by lazy { KotlinType("kotlin", "CharArray") }
    val SHORT_ARRAY by lazy { KotlinType("kotlin", "ShortArray") }
    val INT_ARRAY by lazy { KotlinType("kotlin", "IntArray") }
    val LONG_ARRAY by lazy { KotlinType("kotlin", "LongArray") }
    val FLOAT_ARRAY by lazy { KotlinType("kotlin", "FloatArray") }
    val DOUBLE_ARRAY by lazy { KotlinType("kotlin", "DoubleArray") }

    val PREDEFINED_SCALARS: Map<String, KotlinType> = mapOf(
        "ID" to LONG,
        "Int" to INT,
        "Long" to LONG,
        "Float" to DOUBLE,
        "Double" to DOUBLE,
        "String" to STRING,
        "Boolean" to BOOLEAN
    )
}

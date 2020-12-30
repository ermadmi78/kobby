package io.kobby.generator.kotlin

import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * Created on 18.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

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

data class KotlinType(
    /** A fully-qualified package name. For example: kotlin.collections */
    val packageName: String,

    /** A fully-qualified class name. For example: Map.Entry */
    val className: String,

    /** Is type nullable */
    val allowNull: Boolean = false,

    /** List of generics */
    val generics: List<KotlinType> = listOf()
) {
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
}

class KotlinPackage(packageName: String, prefix: KotlinPackage? = null) {
    val path: List<String> = prefix?.let {
        it.path + packageName.toKotlinPath()
    } ?: packageName.toKotlinPath()

    val name: String by lazy {
        path.joinToString(".")
    }

    operator fun plus(packageName: String): KotlinPackage =
        KotlinPackage(packageName, this)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KotlinPackage
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String {
        return name
    }
}

interface KotlinFile {
    @Throws(IOException::class)
    fun writeTo(out: Appendable)

    @Throws(IOException::class)
    fun writeTo(directory: Path)

    @Throws(IOException::class)
    fun writeTo(directory: File)
}

data class KotlinGeneratorLayout(
    val dto: KotlinDtoLayout,
    val api: KotlinApiLayout,
    val impl: KotlinImplLayout,
    val scalars: Map<String, KotlinType>
)

data class KotlinDtoLayout(
    val packageLayout: KotlinPackage,
    val prefix: String? = null,
    val postfix: String? = "Dto",
    val jacksonized: Boolean = true,
    val builders: Boolean = true
) {
    val packageName: String get() = packageLayout.name
}

data class KotlinApiLayout(
    val enabled: Boolean,
    val packageLayout: KotlinPackage
) {
    val packageName: String get() = packageLayout.name
}

data class KotlinImplLayout(
    val packageLayout: KotlinPackage,
    val prefix: String? = null,
    val postfix: String? = "Impl"
) {
    val packageName: String get() = packageLayout.name
}

data class KotlinFilesLayout(
    val dtoFiles: List<KotlinFile> = listOf(),
    val apiFiles: List<KotlinFile> = listOf(),
    val implFiles: List<KotlinFile> = listOf()
)

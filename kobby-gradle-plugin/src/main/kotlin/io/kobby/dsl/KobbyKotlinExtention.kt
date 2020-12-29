package io.kobby.dsl

import org.gradle.api.Action
import org.gradle.api.file.Directory

@KobbyDsl
open class KobbyKotlinExtension {
    var enabled: Boolean = true

    var outputDirectory: Directory? = null

    var packageName: String? = null

    // *****************************************************************************************************************
    //                                       DTO
    // *****************************************************************************************************************
    private var dtoConfigured: Boolean = false

    internal val dtoExtension: KobbyKotlinDtoExtension by lazy {
        dtoConfigured = true
        KobbyKotlinDtoExtension()
    }

    internal fun isDtoConfigured(): Boolean = dtoConfigured

    /** Kotlin DSL DTO generator configuration */
    fun dto(action: Action<KobbyKotlinDtoExtension>) {
        action.execute(dtoExtension)
    }

    // *****************************************************************************************************************
    //                                       API
    // *****************************************************************************************************************

    private var apiConfigured: Boolean = false
    internal val apiExtension: KobbyKotlinApiExtension by lazy {
        apiConfigured = true
        KobbyKotlinApiExtension()
    }

    internal fun isApiConfigured(): Boolean = apiConfigured

    /** Kotlin DSL API generator configuration */
    fun api(action: Action<KobbyKotlinApiExtension>) {
        action.execute(apiExtension)
    }

    // *****************************************************************************************************************
    //                                         Implementation
    // *****************************************************************************************************************

    private var implConfigured: Boolean = false
    internal val implExtension: KobbyKotlinImplExtension by lazy {
        implConfigured = true
        KobbyKotlinImplExtension()
    }

    internal fun isImplConfigured(): Boolean = implConfigured

    /** Kotlin DSL implementation generator configuration */
    fun impl(action: Action<KobbyKotlinImplExtension>) {
        action.execute(implExtension)
    }

    // *****************************************************************************************************************
    //                                       Scalars
    // *****************************************************************************************************************

    var scalars: Map<String, KobbyTypeName> = mapOf()

    fun typeOf(packageName: String, className: String, vararg arguments: KobbyTypeName) =
        KobbyTypeName(packageName, className, false, arguments.toList())

    fun typeOf(packageName: String, className: String, nullable: Boolean, vararg arguments: KobbyTypeName) =
        KobbyTypeName(packageName, className, nullable, arguments.toList())

    val typeAny by lazy { KobbyTypeName("kotlin", "Any") }
    val typeBoolean by lazy { KobbyTypeName("kotlin", "Boolean") }
    val typeNumber by lazy { KobbyTypeName("kotlin", "Number") }
    val typeByte by lazy { KobbyTypeName("kotlin", "Byte") }
    val typeShort by lazy { KobbyTypeName("kotlin", "Short") }
    val typeInt by lazy { KobbyTypeName("kotlin", "Int") }
    val typeLong by lazy { KobbyTypeName("kotlin", "Long") }
    val typeChar by lazy { KobbyTypeName("kotlin", "Char") }
    val typeFloat by lazy { KobbyTypeName("kotlin", "Float") }
    val typeDouble by lazy { KobbyTypeName("kotlin", "Double") }
    val typeString by lazy { KobbyTypeName("kotlin", "String") }
    val typeCharSequence by lazy { KobbyTypeName("kotlin", "CharSequence") }

    val typeEnum by lazy { KobbyTypeName("kotlin", "Enum") }
    val typeThrowable by lazy { KobbyTypeName("kotlin", "Throwable") }
    val typeIterable by lazy { KobbyTypeName("kotlin.collections", "Iterable") }
    val typeComparable by lazy { KobbyTypeName("kotlin", "Comparable") }

    val typeArray by lazy { KobbyTypeName("kotlin", "Array") }
    val typeCollection by lazy { KobbyTypeName("kotlin.collections", "Collection") }
    val typeList by lazy { KobbyTypeName("kotlin.collections", "List") }
    val typeSet by lazy { KobbyTypeName("kotlin.collections", "Set") }
    val typeMap by lazy { KobbyTypeName("kotlin.collections", "Map") }

    val typeMutableIterable by lazy { KobbyTypeName("kotlin.collections", "MutableIterable") }
    val typeMutableCollection by lazy { KobbyTypeName("kotlin.collections", "MutableCollection") }
    val typeMutableList by lazy { KobbyTypeName("kotlin.collections", "MutableList") }
    val typeMutableSet by lazy { KobbyTypeName("kotlin.collections", "MutableSet") }
    val typeMutableMap by lazy { KobbyTypeName("kotlin.collections", "MutableMap") }
    val typeBooleanArray by lazy { KobbyTypeName("kotlin", "BooleanArray") }
    val typeByteArray by lazy { KobbyTypeName("kotlin", "ByteArray") }
    val typeCharArray by lazy { KobbyTypeName("kotlin", "CharArray") }
    val typeShortArray by lazy { KobbyTypeName("kotlin", "ShortArray") }
    val typeIntArray by lazy { KobbyTypeName("kotlin", "IntArray") }
    val typeLongArray by lazy { KobbyTypeName("kotlin", "LongArray") }
    val typeFloatArray by lazy { KobbyTypeName("kotlin", "FloatArray") }
    val typeDoubleArray by lazy { KobbyTypeName("kotlin", "DoubleArray") }

    //******************************************************************************************************************

    override fun toString(): String =
        "KobbyKotlinExtension(packageName=$packageName, scalars=$scalars)"
}

@KobbyDsl
open class KobbyKotlinDtoExtension {
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = "Dto"
    var jacksonized: Boolean = true
    var builders: Boolean = true

    override fun toString(): String = "KobbyKotlinDtoExtension(" +
            "packageName=$packageName, " +
            "prefix=$prefix, " +
            "postfix=$postfix, " +
            "jacksonized=$jacksonized, " +
            "builders=$builders)"
}

@KobbyDsl
open class KobbyKotlinApiExtension {
    var packageName: String? = "api"

    override fun toString(): String =
        "KobbyKotlinApiExtension(packageName=$packageName)"
}

@KobbyDsl
open class KobbyKotlinImplExtension {
    var packageName: String? = "impl"
    var prefix: String? = null
    var postfix: String? = "Impl"

    override fun toString(): String =
        "KobbyKotlinImplExtension(packageName=$packageName, prefix=$prefix, postfix=$postfix)"
}
package io.kobby

import io.kobby.generator.kotlin.KotlinType
import io.kobby.generator.kotlin.KotlinTypes
import org.gradle.api.Action
import org.gradle.api.file.Directory

@Kobby
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

    var scalars: Map<String, KotlinType> = mapOf()

    fun typeOf(packageName: String, className: String, vararg arguments: KotlinType) =
        KotlinType(packageName, className, false, arguments.toList())

    fun typeOf(packageName: String, className: String, nullable: Boolean, vararg arguments: KotlinType) =
        KotlinType(packageName, className, nullable, arguments.toList())

    val typeAny get() = KotlinTypes.ANY
    val typeNumber get() = KotlinTypes.NUMBER
    val typeByte get() = KotlinTypes.BYTE
    val typeShort get() = KotlinTypes.SHORT
    val typeInt get() = KotlinTypes.INT
    val typeLong get() = KotlinTypes.LONG
    val typeFloat get() = KotlinTypes.FLOAT
    val typeDouble get() = KotlinTypes.DOUBLE
    val typeChar get() = KotlinTypes.CHAR
    val typeString get() = KotlinTypes.STRING
    val typeCharSequence get() = KotlinTypes.CHAR_SEQUENCE
    val typeBoolean get() = KotlinTypes.BOOLEAN

    val typeEnum get() = KotlinTypes.ENUM
    val typeThrowable get() = KotlinTypes.THROWABLE
    val typeComparable get() = KotlinTypes.COMPARABLE

    val typeIterable get() = KotlinTypes.ITERABLE
    val typeCollection get() = KotlinTypes.COLLECTION
    val typeList get() = KotlinTypes.LIST
    val typeSet get() = KotlinTypes.SET
    val typeMap get() = KotlinTypes.MAP

    val typeMutableIterable get() = KotlinTypes.MUTABLE_ITERABLE
    val typeMutableCollection get() = KotlinTypes.MUTABLE_COLLECTION
    val typeMutableList get() = KotlinTypes.MUTABLE_LIST
    val typeMutableSet get() = KotlinTypes.MUTABLE_SET
    val typeMutableMap get() = KotlinTypes.MUTABLE_MAP

    val typeArray get() = KotlinTypes.ARRAY
    val typeBooleanArray get() = KotlinTypes.BOOLEAN_ARRAY
    val typeByteArray get() = KotlinTypes.BYTE_ARRAY
    val typeCharArray get() = KotlinTypes.CHAR_ARRAY
    val typeShortArray get() = KotlinTypes.SHORT_ARRAY
    val typeIntArray get() = KotlinTypes.INT_ARRAY
    val typeLongArray get() = KotlinTypes.LONG_ARRAY
    val typeFloatArray get() = KotlinTypes.FLOAT_ARRAY
    val typeDoubleArray get() = KotlinTypes.DOUBLE_ARRAY

    //******************************************************************************************************************

    override fun toString(): String =
        "KobbyKotlinExtension(packageName=$packageName, scalars=$scalars)"
}

@Kobby
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

@Kobby
open class KobbyKotlinApiExtension {
    var enabled: Boolean = true
    var packageName: String? = "api"

    override fun toString(): String =
        "KobbyKotlinApiExtension(enabled=$enabled, packageName=$packageName)"
}

@Kobby
open class KobbyKotlinImplExtension {
    var packageName: String? = "impl"
    var prefix: String? = null
    var postfix: String? = "Impl"

    override fun toString(): String =
        "KobbyKotlinImplExtension(packageName=$packageName, prefix=$prefix, postfix=$postfix)"
}
package io.kobby

import io.kobby.generator.kotlin.KotlinType
import io.kobby.generator.kotlin.KotlinTypes
import org.gradle.api.Action
import org.gradle.api.file.Directory

@Kobby
open class KobbyKotlinExtension {
    var enabled: Boolean = true

    var scalars: Map<String, KotlinType>? = null

    var relativePackage: Boolean? = null

    var packageName: String? = null

    var outputDirectory: Directory? = null

    internal val dtoExtension = lazy { KobbyKotlinDtoExtension() }
    internal val entityExtension = lazy { KobbyKotlinEntityExtension() }
    internal val implExtension = lazy { KobbyKotlinImplExtension() }

    /** Kotlin DSL DTO generator configuration */
    fun dto(action: Action<KobbyKotlinDtoExtension>) {
        action.execute(dtoExtension.value)
    }

    /** Kotlin DSL Entity generator configuration */
    fun entity(action: Action<KobbyKotlinEntityExtension>) {
        action.execute(entityExtension.value)
    }

    /** Kotlin DSL implementation generator configuration */
    fun impl(action: Action<KobbyKotlinImplExtension>) {
        action.execute(implExtension.value)
    }

    override fun toString(): String {
        return "KobbyKotlinExtension(" +
                "enabled=$enabled, " +
                "scalars=$scalars, " +
                "relativePackage=$relativePackage, " +
                "packageName=$packageName, " +
                "outputDirectory=$outputDirectory, " +
                "dtoExtension=$dtoExtension, " +
                "entityExtension=$entityExtension, " +
                "implExtension=$implExtension)"
    }

    // *****************************************************************************************************************

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
}

// *********************************************************************************************************************
//                                           Extensions
// *********************************************************************************************************************

@Kobby
open class KobbyKotlinDtoExtension {
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = null
    var dslAnnotation: String? = null

    internal val jacksonExtension = lazy { KobbyKotlinDtoJacksonExtension() }
    internal val builderExtension = lazy { KobbyKotlinDtoBuilderExtension() }
    internal val graphQLExtension = lazy { KobbyKotlinDtoGraphQLExtension() }

    /** Kotlin DSL Jackson support generator configuration */
    fun jackson(action: Action<KobbyKotlinDtoJacksonExtension>) {
        action.execute(jacksonExtension.value)
    }

    /** Kotlin DSL Builder generator configuration */
    fun builder(action: Action<KobbyKotlinDtoBuilderExtension>) {
        action.execute(builderExtension.value)
    }

    /** Kotlin DSL GraphQL DTO generation configuration */
    fun graphQL(action: Action<KobbyKotlinDtoGraphQLExtension>) {
        action.execute(graphQLExtension.value)
    }

    override fun toString(): String {
        return "KobbyKotlinDtoExtension(" +
                "packageName=$packageName, " +
                "prefix=$prefix, " +
                "postfix=$postfix, " +
                "dslAnnotation=$dslAnnotation, " +
                "jacksonExtension=$jacksonExtension, " +
                "builderExtension=$builderExtension, " +
                "graphQLExtension=$graphQLExtension)"
    }
}

// *********************************************************************************************************************

@Kobby
open class KobbyKotlinDtoJacksonExtension {
    var enabled: Boolean? = null

    override fun toString(): String {
        return "KobbyKotlinDtoJacksonExtension(enabled=$enabled)"
    }
}

@Kobby
open class KobbyKotlinDtoBuilderExtension {
    var enabled: Boolean? = null
    var prefix: String? = null
    var postfix: String? = null

    override fun toString(): String {
        return "KobbyKotlinDtoBuilderExtension(enabled=$enabled, prefix=$prefix, postfix=$postfix)"
    }
}

@Kobby
open class KobbyKotlinDtoGraphQLExtension {
    var enabled: Boolean? = null
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = null

    override fun toString(): String {
        return "KobbyKotlinDtoGraphQLExtension(enabled=$enabled, packageName=$packageName, " +
                "prefix=$prefix, postfix=$postfix)"
    }
}

@Kobby
open class KobbyKotlinEntityExtension {
    var enabled: Boolean? = null
    var packageName: String? = null

    override fun toString(): String =
        "KobbyKotlinEntityExtension(enabled=$enabled, packageName=$packageName)"
}

@Kobby
open class KobbyKotlinImplExtension {
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = null

    override fun toString(): String =
        "KobbyKotlinImplExtension(packageName=$packageName, prefix=$prefix, postfix=$postfix)"
}
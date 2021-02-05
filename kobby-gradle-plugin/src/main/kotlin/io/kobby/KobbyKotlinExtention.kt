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

    internal val contextExtension = lazy { KobbyKotlinContextExtension() }
    internal val dtoExtension = lazy { KobbyKotlinDtoExtension() }
    internal val entityExtension = lazy { KobbyKotlinEntityExtension() }
    internal val implExtension = lazy { KobbyKotlinImplExtension() }

    /** Kotlin DSL Context generator configuration */
    fun context(action: Action<KobbyKotlinContextExtension>) {
        action.execute(contextExtension.value)
    }

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
                "contextExtension=$contextExtension, " +
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
open class KobbyKotlinContextExtension {
    var packageName: String? = null
    var name: String? = null
    var prefix: String? = null
    var postfix: String? = null
    var query: String? = null
    var mutation: String? = null

    override fun toString(): String {
        return "KobbyKotlinContextExtension(" +
                "packageName=$packageName, " +
                "name=$name, " +
                "prefix=$prefix, " +
                "postfix=$postfix, " +
                "query=$query, " +
                "mutation=$mutation)"
    }
}

@Kobby
open class KobbyKotlinDtoExtension {
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = null

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
    var prefix: String? = null
    var postfix: String? = null
    var withCurrentProjectionFun: String? = null

    internal val projectionExtension = lazy { KobbyKotlinEntityProjectionExtension() }
    internal val selectionExtension = lazy { KobbyKotlinEntitySelectionExtension() }

    /** Kotlin DSL Projection generator configuration */
    fun projection(action: Action<KobbyKotlinEntityProjectionExtension>) {
        action.execute(projectionExtension.value)
    }

    /** Kotlin DSL Selection generator configuration */
    fun selection(action: Action<KobbyKotlinEntitySelectionExtension>) {
        action.execute(selectionExtension.value)
    }

    override fun toString(): String {
        return "KobbyKotlinEntityExtension(" +
                "enabled=$enabled, " +
                "packageName=$packageName, " +
                "prefix=$prefix, " +
                "postfix=$postfix, " +
                "withCurrentProjectionFun=$withCurrentProjectionFun, " +
                "projectionExtension=$projectionExtension, " +
                "selectionExtension=$selectionExtension)"
    }
}

@Kobby
open class KobbyKotlinEntityProjectionExtension {
    var projectionPrefix: String? = null
    var projectionPostfix: String? = null

    var projectionArgument: String? = null

    var withPrefix: String? = null
    var withPostfix: String? = null

    var withoutPrefix: String? = null
    var withoutPostfix: String? = null

    var qualificationPrefix: String? = null
    var qualificationPostfix: String? = null

    var qualifiedProjectionPrefix: String? = null
    var qualifiedProjectionPostfix: String? = null

    var onPrefix: String? = null
    var onPostfix: String? = null

    override fun toString(): String {
        return "KobbyKotlinEntityProjectionExtension(" +
                "projectionPrefix=$projectionPrefix, " +
                "projectionPostfix=$projectionPostfix, " +
                "projectionArgument=$projectionArgument, " +
                "withPrefix=$withPrefix, " +
                "withPostfix=$withPostfix, " +
                "withoutPrefix=$withoutPrefix, " +
                "withoutPostfix=$withoutPostfix, " +
                "qualificationPrefix=$qualificationPrefix, " +
                "qualificationPostfix=$qualificationPostfix, " +
                "qualifiedProjectionPrefix=$qualifiedProjectionPrefix, " +
                "qualifiedProjectionPostfix=$qualifiedProjectionPostfix, " +
                "onPrefix=$onPrefix, " +
                "onPostfix=$onPostfix)"
    }
}

@Kobby
open class KobbyKotlinEntitySelectionExtension {
    var selectionPrefix: String? = null
    var selectionPostfix: String? = null

    var selectionArgument: String? = null

    var queryPrefix: String? = null
    var queryPostfix: String? = null

    var queryArgument: String? = null

    override fun toString(): String {
        return "KobbyKotlinEntitySelectionExtension(" +
                "selectionPrefix=$selectionPrefix, " +
                "selectionPostfix=$selectionPostfix, " +
                "selectionArgument=$selectionArgument, " +
                "queryPrefix=$queryPrefix, " +
                "queryPostfix=$queryPostfix, " +
                "queryArgument=$queryArgument)"
    }
}

@Kobby
open class KobbyKotlinImplExtension {
    var packageName: String? = null
    var prefix: String? = null
    var postfix: String? = null
    var internal: Boolean? = null
    var innerPrefix: String? = null
    var innerPostfix: String? = null

    override fun toString(): String {
        return "KobbyKotlinImplExtension(" +
                "packageName=$packageName, " +
                "prefix=$prefix, " +
                "postfix=$postfix, " +
                "internal=$internal, " +
                "innerPrefix=$innerPrefix, " +
                "innerPostfix=$innerPostfix)"
    }
}
@file:Suppress(
    "unused"
)

package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.generator.kotlin.KotlinType
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes
import org.gradle.api.Action
import org.gradle.api.file.Directory

/**
 * Configuration of Kotlin DSL generation
 */
@Kobby
open class KobbyKotlinExtension {
    /**
     * Is Kotlin DSL generation enabled
     *
     * Default: true
     */
    var enabled: Boolean = true

    /** Mapping GraphQL scalars to Kotlin classes */
    var scalars: Map<String, KotlinType>? = null

    /**
     * Is root package name for generated DSL should be relative to GraphQL schema directory
     *
     * Default: true
     */
    var relativePackage: Boolean? = null

    /**
     * Root package name for generated DSL
     *
     * Default: "kobby.kotlin"
     */
    var packageName: String? = null

    /**
     * Output directory for generated DSL
     *
     * Default: project.layout.buildDirectory.dir("generated/sources/kobby/main/kotlin").get()
     */
    var outputDirectory: Directory? = null

    internal val contextExtension = lazy { KobbyKotlinContextExtension() }
    internal val dtoExtension = lazy { KobbyKotlinDtoExtension() }
    internal val entityExtension = lazy { KobbyKotlinEntityExtension() }
    internal val implExtension = lazy { KobbyKotlinImplExtension() }
    internal val adapterExtension = lazy { KobbyKotlinAdapterExtension() }

    /** Configuration of DSL context generation (entry point to DSL) */
    fun context(action: Action<KobbyKotlinContextExtension>) {
        action.execute(contextExtension.value)
    }

    /** Configuration of DTO classes generation */
    fun dto(action: Action<KobbyKotlinDtoExtension>) {
        action.execute(dtoExtension.value)
    }

    /** Configuration of DSL Entities interfaces generation */
    fun entity(action: Action<KobbyKotlinEntityExtension>) {
        action.execute(entityExtension.value)
    }

    /** Configuration of DSL Entities implementation classes generation */
    fun impl(action: Action<KobbyKotlinImplExtension>) {
        action.execute(implExtension.value)
    }

    /** Configuration of adapter classes generation */
    fun adapter(action: Action<KobbyKotlinAdapterExtension>) {
        action.execute(adapterExtension.value)
    }

    // *****************************************************************************************************************

    /**
     * Define Kotlin type for scalar mapping
     *
     * @param packageName package name of Kotlin type (for example "kotlin.collections")
     * @param className class name of Kotlin type (for example "Map")
     * @param arguments generic arguments of Kotlin type
     * @return Kotlin type definition
     */
    fun typeOf(packageName: String, className: String, vararg arguments: KotlinType) =
        KotlinType(packageName, className, false, arguments.toList(), null)

    /**
     * Define Kotlin type for scalar mapping
     *
     * @param packageName package name of Kotlin type (for example "kotlin.collections")
     * @param className class name of Kotlin type (for example "Map")
     * @param nullable is defining type nullable
     * @param arguments generic arguments of Kotlin type
     * @return Kotlin type definition
     */
    fun typeOf(packageName: String, className: String, nullable: Boolean, vararg arguments: KotlinType) =
        KotlinType(packageName, className, nullable, arguments.toList(), null)

    /** kotlin.Any */
    val typeAny get() = KotlinTypes.ANY

    /** kotlin.Number */
    val typeNumber get() = KotlinTypes.NUMBER

    /** kotlin.Byte */
    val typeByte get() = KotlinTypes.BYTE

    /** kotlin.Short */
    val typeShort get() = KotlinTypes.SHORT

    /** kotlin.Int */
    val typeInt get() = KotlinTypes.INT

    /** kotlin.Long */
    val typeLong get() = KotlinTypes.LONG

    /** kotlin.Float */
    val typeFloat get() = KotlinTypes.FLOAT

    /** kotlin.Double */
    val typeDouble get() = KotlinTypes.DOUBLE

    /** kotlin.Char */
    val typeChar get() = KotlinTypes.CHAR

    /** kotlin.String */
    val typeString get() = KotlinTypes.STRING

    /** kotlin.CharSequence */
    val typeCharSequence get() = KotlinTypes.CHAR_SEQUENCE

    /** kotlin.Boolean */
    val typeBoolean get() = KotlinTypes.BOOLEAN

    //******************************************************************************************************************

    /** kotlin.Enum */
    val typeEnum get() = KotlinTypes.ENUM

    /** kotlin.Throwable */
    val typeThrowable get() = KotlinTypes.THROWABLE

    /** kotlin.Comparable */
    val typeComparable get() = KotlinTypes.COMPARABLE

    //******************************************************************************************************************

    /** kotlin.collections.Iterable */
    val typeIterable get() = KotlinTypes.ITERABLE

    /** kotlin.collections.Collection */
    val typeCollection get() = KotlinTypes.COLLECTION

    /** kotlin.collections.List */
    val typeList get() = KotlinTypes.LIST

    /** kotlin.collections.Set */
    val typeSet get() = KotlinTypes.SET

    /** kotlin.collections.Map */
    val typeMap get() = KotlinTypes.MAP

    //******************************************************************************************************************

    /** kotlin.collections.MutableIterable */
    val typeMutableIterable get() = KotlinTypes.MUTABLE_ITERABLE

    /** kotlin.collections.MutableCollection */
    val typeMutableCollection get() = KotlinTypes.MUTABLE_COLLECTION

    /** kotlin.collections.MutableList */
    val typeMutableList get() = KotlinTypes.MUTABLE_LIST

    /** kotlin.collections.MutableSet */
    val typeMutableSet get() = KotlinTypes.MUTABLE_SET

    /** kotlin.collections.MutableMap */
    val typeMutableMap get() = KotlinTypes.MUTABLE_MAP

    //******************************************************************************************************************

    /** kotlin.Array */
    val typeArray get() = KotlinTypes.ARRAY

    /** kotlin.BooleanArray */
    val typeBooleanArray get() = KotlinTypes.BOOLEAN_ARRAY

    /** kotlin.ByteArray */
    val typeByteArray get() = KotlinTypes.BYTE_ARRAY

    /** kotlin.CharArray */
    val typeCharArray get() = KotlinTypes.CHAR_ARRAY

    /** kotlin.ShortArray */
    val typeShortArray get() = KotlinTypes.SHORT_ARRAY

    /** kotlin.IntArray */
    val typeIntArray get() = KotlinTypes.INT_ARRAY

    /** kotlin.LongArray */
    val typeLongArray get() = KotlinTypes.LONG_ARRAY

    /** kotlin.FloatArray */
    val typeFloatArray get() = KotlinTypes.FLOAT_ARRAY

    /** kotlin.DoubleArray */
    val typeDoubleArray get() = KotlinTypes.DOUBLE_ARRAY
}

// *********************************************************************************************************************
//                                           Extensions
// *********************************************************************************************************************

/**
 * Configuration of DSL context generation (entry point to DSL)
 */
@Kobby
open class KobbyKotlinContextExtension {
    /**
     * Context package name relative to root package name
     *
     * By default, is empty
     */
    var packageName: String? = null

    /**
     * Name of generated DSL context
     *
     * By default, is name of GraphQL schema file or "graphql" if there are multiple schema files
     */
    var name: String? = null

    /**
     * Prefix of generated "Context" interface
     *
     * By default, is capitalized context name
     */
    var prefix: String? = null

    /** Postfix of generated "Context" interface */
    var postfix: String? = null

    /**
     * Name of "query" function in "Context" interface
     *
     * Default: "query"
     */
    var query: String? = null

    /**
     * Name of "mutation" function in "Context" interface
     *
     * Default: "mutation"
     */
    var mutation: String? = null

    /**
     * Name of "subscription" function in "Context" interface
     *
     * Default: "subscription"
     */
    var subscription: String? = null

    /**
     * Is generation of the commit function enabled for subscription
     * [More details](https://github.com/ermadmi78/kobby/issues/31)
     *
     * Default: false
     */
    var commitEnabled: Boolean? = null
}

/**
 * Configuration of DTO classes generation
 */
@Kobby
open class KobbyKotlinDtoExtension {
    /**
     * Package name for DTO classes. Relative to root package name.
     *
     * Default: "dto"
     */
    var packageName: String? = null

    /** Prefix of DTO classes generated from GraphQL objects, interfaces and unions */
    var prefix: String? = null

    /**
     * Postfix of DTO classes generated from GraphQL objects, interfaces and unions.
     *
     * Default: "Dto"
     */
    var postfix: String? = null

    /** Prefix of DTO classes generated from GraphQL enums */
    var enumPrefix: String? = null

    /** Postfix of DTO classes generated from GraphQL enums */
    var enumPostfix: String? = null

    /** Prefix of DTO classes generated from GraphQL inputs */
    var inputPrefix: String? = null

    /** Postfix of DTO classes generated from GraphQL inputs */
    var inputPostfix: String? = null

    /**
     * Kobby can generate "equals" and "hashCode" functions for entities classes based on fields marked with
     * "@primaryKey" directive. This parameter provides an ability to apply the same generation logic to DTO classes.
     *
     * Default: false
     */
    var applyPrimaryKeys: Boolean? = null

    /**
     * Generate immutable DTO class if number of GraphQL type fields <= property value.
     * Otherwise, generate a mutable DTO class.
     * Set the property value to 0 to always generate mutable DTO classes.
     * [More details](https://github.com/ermadmi78/kobby/issues/43)
     *
     * Default: 245
     */
    var maxNumberOfFieldsForImmutableDtoClass: Int? = null

    /**
     * Generate immutable Input class if number of GraphQL Input type fields <= property value.
     * Otherwise, generate a mutable Input class.
     * Set the property value to 0 to always generate mutable Input classes.
     * [More details](https://github.com/ermadmi78/kobby/issues/43)
     *
     * Default: 245
     */
    var maxNumberOfFieldsForImmutableInputClass: Int? = null

    internal val serializationExtension = lazy { KobbyKotlinDtoSerializationExtension() }
    internal val jacksonExtension = lazy { KobbyKotlinDtoJacksonExtension() }
    internal val builderExtension = lazy { KobbyKotlinDtoBuilderExtension() }
    internal val graphQLExtension = lazy { KobbyKotlinDtoGraphQLExtension() }

    /**
     * Configuration of Kotlinx Serialization support for DTO classes.
     *
     * Note that Kotlinx serialization and Jackson serialization are not supported simultaneously.
     */
    fun serialization(action: Action<KobbyKotlinDtoSerializationExtension>) {
        action.execute(serializationExtension.value)
    }

    /**
     * Configuration of Jackson annotations generation for DTO classes.
     *
     * Note that Kotlinx serialization and Jackson serialization are not supported simultaneously.
     */
    fun jackson(action: Action<KobbyKotlinDtoJacksonExtension>) {
        action.execute(jacksonExtension.value)
    }

    /** Configuration of DTO builders generation */
    fun builder(action: Action<KobbyKotlinDtoBuilderExtension>) {
        action.execute(builderExtension.value)
    }

    /**
     * Configuration of helper DTO classes generation for implementing the GraphQL interaction protocol
     */
    fun graphQL(action: Action<KobbyKotlinDtoGraphQLExtension>) {
        action.execute(graphQLExtension.value)
    }
}

// *********************************************************************************************************************

/**
 * Configuration of Kotlinx Serialization support for DTO classes.
 *
 * Note that Kotlinx serialization and Jackson serialization are not supported simultaneously.
 */
@Kobby
open class KobbyKotlinDtoSerializationExtension {
    /**
     * Is Kotlinx Serialization enabled.
     * Note that Kotlinx serialization and Jackson serialization are not supported simultaneously.
     *
     * By default, "true" if "org.jetbrains.kotlinx:kotlinx-serialization-json" artifact is in the project dependencies.
     */
    var enabled: Boolean? = null

    /**
     * Name of the class descriptor property for polymorphic serialization.
     *
     * Default: "__typename"
     */
    var classDiscriminator: String? = null

    /**
     * Specifies whether encounters of unknown properties in the input JSON should be ignored
     * instead of throwing SerializationException.
     *
     * Default: true
     */
    var ignoreUnknownKeys: Boolean? = null

    /**
     * Specifies whether default values of Kotlin properties should be encoded to JSON.
     *
     * Default: false
     */
    var encodeDefaults: Boolean? = null

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *
     * Default: false
     */
    var prettyPrint: Boolean? = null
}

/** Configuration of Jackson annotations generation for DTO classes */
@Kobby
open class KobbyKotlinDtoJacksonExtension {
    /**
     * Is Jackson annotations generation enabled.
     * Note that Kotlinx serialization and Jackson serialization are not supported simultaneously.
     *
     * By default, "true" if "com.fasterxml.jackson.core:jackson-annotations" artifact is in the project dependencies.
     */
    var enabled: Boolean? = null

    /**
     * Customize the @JsonTypeInfo annotation's `use` property.
     *
     * Default: "NAME"
     */
    var typeInfoUse: String? = null

    /**
     * Customize the @JsonTypeInfo annotation's `include` property.
     *
     * Default: "PROPERTY"
     */
    var typeInfoInclude: String? = null

    /**
     * Customize the @JsonTypeInfo annotation's `property` property.
     *
     * Default: "__typename"
     */
    var typeInfoProperty: String? = null

    /**
     * Customize the @JsonInclude annotation's `value` property.
     *
     * Default: "NON_ABSENT"
     */
    var jsonInclude: String? = null
}

/**
 * Configuration of DTO builders generation
 */
@Kobby
open class KobbyKotlinDtoBuilderExtension {
    /**
     * Is DTO builders generation enabled
     *
     * Default: true
     */
    var enabled: Boolean? = null

    /** Prefix of DTO builder classes */
    var prefix: String? = null

    /**
     * Postfix of DTO builder classes
     *
     * Default: "Builder"
     */
    var postfix: String? = null

    /**
     * Name of DTO based "toBuilder" function for DTO classes
     *
     * Default: "toBuilder"
     */
    var toBuilderFun: String? = null

    /**
     * Name of builder based "toDto" function for DTO classes
     *
     * Default "toDto"
     */
    var toDtoFun: String? = null

    /**
     * Name of builder based "toInput" function for DTO input classes
     *
     * Default "toInput"
     */
    var toInputFun: String? = null

    /**
     * Name of builder based "copy" function for DTO classes
     *
     * Default: "copy"
     */
    var copyFun: String? = null
}

/**
 * Configuration of helper DTO classes generation for implementing the GraphQL interaction protocol
 */
@Kobby
open class KobbyKotlinDtoGraphQLExtension {
    /**
     * Is helper DTO classes generation enabled
     *
     * Default: true
     */
    var enabled: Boolean? = null

    /**
     * Package name for helper DTO classes relative to DTO package name
     *
     * Default: "graphql"
     */
    var packageName: String? = null

    /** Prefix for helper DTO classes */
    var prefix: String? = null

    /** Postfix for helper DTO classes */
    var postfix: String? = null
}

/**
 * Configuration of DSL Entities interfaces generation
 */
@Kobby
open class KobbyKotlinEntityExtension {
    /**
     * Is entities interfaces generation enabled
     *
     * Default: true
     */
    var enabled: Boolean? = null

    /**
     * Package name for entities interfaces relative to root package name
     *
     * Default: "entity"
     */
    var packageName: String? = null

    /** Prefix for entities interfaces */
    var prefix: String? = null

    /** Postfix for entities interfaces */
    var postfix: String? = null

    /**
     * GraphQL response errors access function generated for adapters with extended API.
     * See `adapter.extendedApi` and `adapter.throwException` properties.
     *
     * To enable GraphQL error propagation to the entity layer,
     * set `adapter.throwException` property to `false`.
     * [More details](https://github.com/ermadmi78/kobby/issues/48)
     * [More details](https://github.com/ermadmi78/kobby/issues/51)
     *
     * Default: "__errors"
     */
    var errorsFunName: String? = null

    /**
     * GraphQL response extensions access function generated for adapters with extended API.
     * See `adapter.extendedApi` property.
     *
     * To enable GraphQL extensions propagation to the entity layer,
     * set `adapter.extendedApi` property to `true`.
     * [More details](https://github.com/ermadmi78/kobby/issues/48)
     * [More details](https://github.com/ermadmi78/kobby/issues/51)
     *
     * Default: "__extensions"
     */
    var extensionsFunName: String? = null

    /**
     * Generate context access function in entity interface.
     * [More details](https://github.com/ermadmi78/kobby/issues/20)
     * [More details](https://github.com/ermadmi78/kobby/issues/29)
     * [More details](https://github.com/ermadmi78/kobby/issues/35)
     *
     * Default: true
     */
    var contextFunEnabled: Boolean? = null

    /**
     * Context access function name in entity interface.
     * [More details](https://github.com/ermadmi78/kobby/issues/20)
     *
     * Default: "__context"
     */
    var contextFunName: String? = null

    /**
     * Name of "withCurrentProjection" function in entity interface
     *
     * Default: "__withCurrentProjection"
     */
    var withCurrentProjectionFun: String? = null

    internal val projectionExtension = lazy { KobbyKotlinEntityProjectionExtension() }
    internal val selectionExtension = lazy { KobbyKotlinEntitySelectionExtension() }

    /** Configuration of DSL Entity Projection interfaces generation */
    fun projection(action: Action<KobbyKotlinEntityProjectionExtension>) {
        action.execute(projectionExtension.value)
    }

    /** Configuration of DSL Entity Selection interfaces generation */
    fun selection(action: Action<KobbyKotlinEntitySelectionExtension>) {
        action.execute(selectionExtension.value)
    }
}

/**
 * Configuration of DSL Entity Projection interfaces generation
 */
@Kobby
open class KobbyKotlinEntityProjectionExtension {
    /** Prefix for projection interfaces */
    var projectionPrefix: String? = null

    /**
     * Postfix for projection interfaces
     *
     * Default: "Projection"
     */
    var projectionPostfix: String? = null

    /**
     * Name of projection argument in field functions
     *
     * Default: "__projection"
     */
    var projectionArgument: String? = null

    /** Prefix for projection fields that are not marked with the directive "@default" */
    var withPrefix: String? = null

    /** Postfix for projection fields that are not marked with the directive "@default" */
    var withPostfix: String? = null

    /**
     * Prefix for default projection fields (marked with the directive "@default")
     *
     * Default: "__without"
     */
    var withoutPrefix: String? = null

    /** Postfix for default projection fields (marked with the directive "@default") */
    var withoutPostfix: String? = null

    /**
     * Name of "minimize" function in projection interface
     *
     * Default: "__minimize"
     */
    var minimizeFun: String? = null

    /** Prefix for qualification interfaces */
    var qualificationPrefix: String? = null

    /**
     * Postfix for qualification interfaces
     *
     * Default: "Qualification"
     */
    var qualificationPostfix: String? = null

    /** Prefix for qualified projection interface */
    var qualifiedProjectionPrefix: String? = null

    /**
     * Postfix for qualified projection interface
     *
     * Default: "QualifiedProjection"
     */
    var qualifiedProjectionPostfix: String? = null

    /**
     * Prefix for qualification functions
     *
     * Default: "__on"
     */
    var onPrefix: String? = null

    /** Postfix for qualification functions */
    var onPostfix: String? = null

    /**
     * Enable notation without parentheses for projection field getters
     *
     * Default: false
     */
    var enableNotationWithoutParentheses: Boolean? = false
}

/**
 * Configuration of DSL Entity Selection interfaces generation
 */
@Kobby
open class KobbyKotlinEntitySelectionExtension {
    /** Prefix for selection interfaces */
    var selectionPrefix: String? = null

    /**
     * Postfix for selection interfaces
     *
     * Default: "Selection"
     */
    var selectionPostfix: String? = null

    /**
     * Name of selection argument in field functions
     *
     * Default: "__selection"
     */
    var selectionArgument: String? = null

    /** Prefix for query interfaces */
    var queryPrefix: String? = null

    /**
     * Postfix for query interfaces
     *
     * Default: "Query"
     */
    var queryPostfix: String? = null

    /**
     * Name of query argument in field functions
     *
     * Default: "__query"
     */
    var queryArgument: String? = null
}

/**
 * Configuration of DSL Entities implementation classes generation
 */
@Kobby
open class KobbyKotlinImplExtension {
    /**
     * Package name for entities implementation classes relative to root package name
     *
     * Default: "entity.impl"
     */
    var packageName: String? = null

    /** Prefix for entities implementation classes */
    var prefix: String? = null

    /**
     * Postfix for entities implementation classes
     *
     * Default: "Impl"
     */
    var postfix: String? = null

    /**
     * Is implementation classes should be internal
     *
     * Default: true
     */
    var internal: Boolean? = null

    /**
     * Prefix for inner fields in implementation classes
     *
     * Default: "__inner"
     */
    var innerPrefix: String? = null

    /** Postfix for inner fields in implementation classes */
    var innerPostfix: String? = null
}

/**
 * Configuration of adapter classes generation
 */
@Kobby
open class KobbyKotlinAdapterExtension {
    /**
     * Is extended adapter API (with GraphQL errors and extensions) enabled
     * [More details](https://github.com/ermadmi78/kobby/issues/48)
     * [More details](https://github.com/ermadmi78/kobby/issues/51)
     *
     * Default: false
     */
    var extendedApi: Boolean? = null

    /**
     * Throw exception when receiving non-empty GraphQL errors
     * [More details](https://github.com/ermadmi78/kobby/issues/48)
     * [More details](https://github.com/ermadmi78/kobby/issues/51)
     *
     * Default: true
     */
    var throwException: Boolean? = null

    internal val ktorExtension = lazy { KobbyKotlinAdapterKtorExtension() }

    /** Configuration of Ktor adapter classes generation */
    fun ktor(action: Action<KobbyKotlinAdapterKtorExtension>) {
        action.execute(ktorExtension.value)
    }
}

/**
 * Configuration of Ktor adapter classes generation
 */
@Kobby
open class KobbyKotlinAdapterKtorExtension {
    /**
     * Is simple Ktor adapter generation enabled
     *
     * By default, "true" if "io.ktor:ktor-client-cio" artifact is in the project dependencies
     */
    var simpleEnabled: Boolean? = null

    /**
     * Is composite Ktor adapter generation enabled
     *
     * By default, "true" if "io.ktor:ktor-client-cio" artifact is in the project dependencies
     */
    var compositeEnabled: Boolean? = null

    /**
     * Package name for Ktor adapter classes relative to root package name
     *
     * Default: "adapter.ktor"
     */
    var packageName: String? = null

    /** Prefix for Ktor adapter classes */
    var prefix: String? = null

    /**
     * Postfix for Ktor adapter classes
     *
     * Default: "KtorAdapter"
     */
    var postfix: String? = null

    /**
     * Default receive message timeout in milliseconds for subscriptions in Ktor composite adapter
     *
     * Default: 10000 milliseconds. Set it to zero or a negative value to disable it.
     */
    var receiveTimeoutMillis: Long? = null
}
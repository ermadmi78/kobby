package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.model.query.KobbyTypeAlias
import io.github.ermadmi78.kobby.task.KobbyTypeOperationQuery
import io.github.ermadmi78.kobby.task.KobbyTypeQuery
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@DslMarker
annotation class Kobby

/**
 * Kobby Plugin Configuration
 */
fun Project.kobby(configure: KobbyExtension.() -> Unit) =
    extensions.configure(KobbyExtension::class.java, configure)

/**
 * Kobby Plugin Configuration
 */
@Kobby
open class KobbyExtension {
    internal val schemaExtension = lazy { KobbySchemaExtension() }
    internal val kotlinExtension = lazy { KobbyKotlinExtension() }

    /** Schema location and parsing rules configuration */
    fun schema(action: Action<KobbySchemaExtension>) {
        action.execute(schemaExtension.value)
    }

    /** Configuration of Kotlin DSL generation */
    fun kotlin(action: Action<KobbyKotlinExtension>) {
        action.execute(kotlinExtension.value)
    }
}

/**
 * Schema location and parsing rules configuration
 */
@Kobby
open class KobbySchemaExtension {
    /**
     * GraphQL schema files to generate Kobby DSL.
     *
     * By default, all "`**`/`*`.graphqls" files in "src/main/resources"
     */
    var files: FileCollection? = null

    internal val scanExtension = lazy { KobbySchemaScanExtension() }
    internal val directiveExtension = lazy { KobbySchemaDirectiveExtension() }
    internal val truncateExtension = lazy { KobbySchemaTruncateExtension() }
    internal val analyzeExtension = lazy { KobbySchemaAnalyzeExtension() }

    /** Configuration of schema files location scanning */
    fun scan(action: Action<KobbySchemaScanExtension>) {
        action.execute(scanExtension.value)
    }

    /** Configuration of Kobby GraphQL directives parsing */
    fun directive(action: Action<KobbySchemaDirectiveExtension>) {
        action.execute(directiveExtension.value)
    }

    /**
     * GraphQL schema truncation configuration.
     * This mechanism provides the ability to truncate the GraphQL schema before code generation.
     *
     * Schema truncation occurs in two stages:
     * * The first stage removes all fields from GraphQL types that match the query.
     * * The second stage removes all GraphQL types from the schema that are not accessible from the schema root (Query, Mutation or Subscription types).
     */
    fun truncate(action: Action<KobbySchemaTruncateExtension>) {
        action.execute(truncateExtension.value)
    }

    /**
     * `kobbySchemaAnalyze` task configuration.
     *
     * This task prints a report to the console with all GraphQL types and fields that match the query.
     * To run task use:
     * ```
     * gradle kobbySchemaAnalyze
     * ```
     */
    fun analyze(action: Action<KobbySchemaAnalyzeExtension>) {
        action.execute(analyzeExtension.value)
    }
}

/**
 * Configuration of schema files location scanning
 */
@Kobby
open class KobbySchemaScanExtension {
    /**
     * Root directory to scan schema files
     *
     * Default: "src/main/resources"
     */
    var dir: String? = null

    /**
     * ANT style include patterns to scan schema files
     *
     * Default: listOf("`**`/`*`.graphqls")
     */
    var includes: Iterable<String>? = null

    /** ANT style exclude patterns to scan schema files */
    var excludes: Iterable<String>? = null
}

/**
 * Configuration of Kobby GraphQL directives parsing
 */
@Kobby
open class KobbySchemaDirectiveExtension {
    /**
     * Name of "primaryKey" directive
     *
     * Default: "primaryKey"
     */
    var primaryKey: String? = null

    /**
     * Name of "required" directive
     *
     * Default: "required"
     */
    var required: String? = null

    /**
     * Name of "default" directive
     *
     * Default: "default"
     */
    var default: String? = null

    /**
     * Name of "selection" directive
     *
     * Default: "selection"
     */
    var selection: String? = null
}

//**********************************************************************************************************************

/**
 * GraphQL schema truncation configuration.
 * This mechanism provides the ability to truncate the GraphQL schema before code generation.
 *
 * Schema truncation occurs in two stages:
 * * The first stage preserves all GraphQL fields that match the truncation query in the schema and removes all other fields.
 * * The second stage removes all GraphQL types from the schema that are not accessible from the schema root (Query, Mutation or Subscription types).
 */
@Kobby
open class KobbySchemaTruncateExtension {
    /**
     * Print detailed schema truncation report to console.
     *
     * Default: false
     */
    var reportEnabled: Boolean? = null

    /**
     * Is Regex enabled in GraphQL schema truncation query.
     * By default, a simplified Kobby Pattern is used:
     * * `?` - matches one character.
     * * `*` - matches zero or more characters.
     * * `|` - OR operator.
     * * `__query` - alias for `Query` type.
     * * `__mutation` - alias for `Mutation` type.
     * * `__subscription` - alias for `Subscription` type.
     * * `__root` - alias for `Query`, `Mutation` and `Subscription` types.
     * * `__any` - alias for any type in the GraphQL schema.
     * * `__anyScalar` - alias for any scalar in the GraphQL schema.
     * * `__anyObject` - alias for any object in the GraphQL schema.
     * * `__anyInterface` - alias for any interface in the GraphQL schema.
     * * `__anyUnion` - alias for any union in the GraphQL schema.
     * * `__anyEnum` - alias for any enum in the GraphQL schema.
     * * `__anyInput` - alias for any input object in the GraphQL schema.
     *
     * Default: false
     */
    var regexEnabled: Boolean? = null

    /**
     * Are patterns used in a GraphQL schema truncation query case sensitive.
     *
     * Default: true
     */
    var caseSensitive: Boolean? = null

    internal val queryExtension = lazy { KobbySchemaQueryExtension() }

    /**
     * A query used to truncate the GraphQL schema before generating code.
     *
     * All GraphQL type fields that match this query should be stored in the truncated schema.
     * All other fields will be removed from the truncated schema.
     *
     * Then, all GraphQL types that are not accessible from the schema root (Query, Mutation, or Subscription types)
     * will also be removed from the GraphQL schema.
     *
     * When performing a truncate operation, all fields of all types that do not match any of the `forXXX` conditions
     * will be stored in the truncated schema. For example the truncation by the query:
     * ``` Kotlin
     * byQuery {
     *     forMutation {
     *         exclude {
     *             field("*")
     *         }
     *     }
     * }
     * ```
     * will remove all fields from the `Mutation` type, but keep all fields in other types.
     *
     * Default:
     * ``` Kotlin
     * byQuery {
     *     forAny {
     *         include {
     *             field("*")
     *         }
     *     }
     * }
     * ```
     */
    fun byQuery(action: Action<KobbySchemaQueryExtension>) {
        action.execute(queryExtension.value)
    }
}

/**
 * `kobbySchemaAnalyze` task configuration.
 *
 * This task prints a report to the console with all GraphQL types and fields that match the query.
 */
@Kobby
open class KobbySchemaAnalyzeExtension {
    /**
     * * `true` - use a truncated GraphQL schema for analysis.
     * * `false` - use the original GraphQL schema for analysis.
     *
     * Default: true
     */
    var truncatedSchema: Boolean? = null

    /**
     * GraphQL schema analysis depth. Use -1 to analyse a schema with unlimited depth.
     *
     * Default: 1
     */
    var depth: Int? = null

    /**
     * GraphQL schema analysis report length limit. Use -1 to print a report with unlimited length.
     *
     * Default: 10000
     */
    var reportLengthLimit: Int? = null

    /**
     * The minimum weight of a GraphQL type that should be printed in the report.
     *
     * The GraphQL type weight is the number of GraphQL types (excluding scalars)
     * that are available for querying on a field that returns the given type.
     *
     * Default: 2
     */
    var printMinWeight: Int? = null

    /**
     * Print "override sign" (`^`) in report for overridden GraphQL type fields.
     *
     * Default: false
     */
    var printOverride: Boolean? = null

    /**
     * Print GraphQL field argument types in a report.
     *
     * Default: false
     */
    var printArgumentTypes: Boolean? = null

    /**
     * Print GraphQL field supertypes in report (`<-` followed by a list of supertypes).
     *
     * Default: false
     */
    var printSuperTypes: Boolean? = null

    /**
     * Print GraphQL field subtypes in report (`->` followed by a list of subtypes).
     *
     * Default: false
     */
    var printSubTypes: Boolean? = null

    /**
     * Is Regex enabled in GraphQL schema analyze query.
     * By default, a simplified Kobby Pattern is used:
     * * `?` - matches one character.
     * * `*` - matches zero or more characters.
     * * `|` - OR operator.
     * * `__query` - alias for `Query` type.
     * * `__mutation` - alias for `Mutation` type.
     * * `__subscription` - alias for `Subscription` type.
     * * `__root` - alias for `Query`, `Mutation` and `Subscription` types.
     * * `__any` - alias for any type in the GraphQL schema.
     * * `__anyScalar` - alias for any scalar in the GraphQL schema.
     * * `__anyObject` - alias for any object in the GraphQL schema.
     * * `__anyInterface` - alias for any interface in the GraphQL schema.
     * * `__anyUnion` - alias for any union in the GraphQL schema.
     * * `__anyEnum` - alias for any enum in the GraphQL schema.
     * * `__anyInput` - alias for any input object in the GraphQL schema.
     *
     * Default: false
     */
    var regexEnabled: Boolean? = null

    /**
     * Are patterns used in a GraphQL schema analyze query case sensitive.
     *
     * Default: true
     */
    var caseSensitive: Boolean? = null

    internal val queryExtension = lazy { KobbySchemaQueryExtension() }

    /**
     * A query used to analyze the GraphQL schema.
     *
     * The `kobbySchemaAnalyze` task prints a report to the console with the GraphQL types and fields
     * matches to the given query.
     *
     * Default:
     * ``` Kotlin
     * byQuery {
     *     forRoot {
     *         include {
     *             field("*")
     *         }
     *     }
     * }
     * ```
     */
    fun byQuery(action: Action<KobbySchemaQueryExtension>) {
        action.execute(queryExtension.value)
    }
}

/**
 * Schema query. The part of the query that is responsible for selecting the GraphQL types in which to look for fields.
 */
@Kobby
open class KobbySchemaQueryExtension {
    private val typeExtensions = mutableMapOf<String, KobbyTypeOperationQueryExtension>()

    internal fun build(): Map<String, KobbyTypeOperationQuery> = typeExtensions
        .apply { remove("") }
        .mapValues { entry -> entry.value.build() }

    /**
     * Apply the operation subquery to all types in the GraphQL schema that match the `pattern`.
     *
     * @param pattern Pattern to choose types in the schema
     *                (`?` - matches one character, `*` - matches zero or more characters, `|` - OR operator).
     * @param action operation subquery
     */
    fun forType(pattern: String, action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(pattern.trim()) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to a `Query` type in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forQuery(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.QUERY) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to a `Mutation` type in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forMutation(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.MUTATION) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to a `Subscription` type in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forSubscription(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.SUBSCRIPTION) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to root types in the GraphQL schema - `Query`, `Mutation` and `Subscription`.
     *
     * @param action operation subquery
     */
    fun forRoot(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ROOT) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any type in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAny(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any object in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAnyObject(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY_OBJECT) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any interface in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAnyInterface(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY_INTERFACE) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any union in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAnyUnion(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY_UNION) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any enum in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAnyEnum(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY_ENUM) {
            KobbyTypeOperationQueryExtension()
        }
    )

    /**
     * Apply the operation subquery to any input object in the GraphQL schema.
     *
     * @param action operation subquery
     */
    fun forAnyInput(action: Action<KobbyTypeOperationQueryExtension>) = action.execute(
        typeExtensions.getOrPut(KobbyTypeAlias.ANY_INPUT) {
            KobbyTypeOperationQueryExtension()
        }
    )
}

/**
 * Operation query. The part of the query responsible for applying the type subquery to the target GraphQL type.
 */
@Kobby
open class KobbyTypeOperationQueryExtension {
    private val include = KobbyTypeQueryExtension()
    private val exclude = KobbyTypeQueryExtension()

    internal fun build() = KobbyTypeOperationQuery(
        include = include.build(),
        exclude = exclude.build()
    )

    /**
     * Include operation.
     * Apply the type subquery to a target GraphQL type.
     * Put fields of the target type that match the subquery into the query result.
     *
     * @param action type subquery
     */
    fun include(action: Action<KobbyTypeQueryExtension>) {
        action.execute(include)
    }

    /**
     * Exclude operation.
     * Apply the type subquery to a target GraphQL type.
     * Put fields of the target type that do not match the subquery into the query result.
     *
     * @param action type subquery
     */
    fun exclude(action: Action<KobbyTypeQueryExtension>) {
        action.execute(exclude)
    }
}

/**
 * Type query. The part of the query that is responsible for selecting fields in the target GraphQL type.
 */
@Kobby
open class KobbyTypeQueryExtension {
    private val enumValue = mutableSetOf<String>()
    private val field = mutableSetOf<String>()
    private var anyOverriddenField = false
    private val dependency = mutableSetOf<String>()
    private val subTypeDependency = mutableSetOf<String>()
    private val superTypeDependency = mutableSetOf<String>()
    private val argumentDependency = mutableSetOf<String>()
    private val transitiveDependency = mutableSetOf<String>()
    private var minWeight: Int? = null
    private var maxWeight: Int? = null

    internal fun build() = KobbyTypeQuery(
        enumValue = enumValue.filter { it.isNotBlank() },
        field = field.filter { it.isNotBlank() },
        anyOverriddenField = anyOverriddenField,
        dependency = dependency.filter { it.isNotBlank() },
        subTypeDependency = subTypeDependency.filter { it.isNotBlank() },
        superTypeDependency = superTypeDependency.filter { it.isNotBlank() },
        argumentDependency = argumentDependency.filter { it.isNotBlank() },
        transitiveDependency = transitiveDependency.filter { it.isNotBlank() },
        minWeight = minWeight,
        maxWeight = maxWeight
    )

    /**
     * Select enum values whose name matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun enumValue(pattern: String) {
        enumValue += pattern.trim()
    }

    /**
     * Select fields whose name matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun field(pattern: String) {
        field += pattern.trim()
    }

    /** Select fields that override any fields from the supertype. */
    fun anyOverriddenField() {
        anyOverriddenField = true
    }

    /**
     * Select fields whose type matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun dependency(pattern: String) {
        dependency += pattern.trim()
    }

    /** Select fields whose type is scalar. */
    fun anyScalarDependency() = dependency(KobbyTypeAlias.ANY_SCALAR)

    /** Select fields whose type is object. */
    fun anyObjectDependency() = dependency(KobbyTypeAlias.ANY_OBJECT)

    /** Select fields whose type is interface. */
    fun anyInterfaceDependency() = dependency(KobbyTypeAlias.ANY_INTERFACE)

    /** Select fields whose type is union. */
    fun anyUnionDependency() = dependency(KobbyTypeAlias.ANY_UNION)

    /** Select fields whose type is enum. */
    fun anyEnumDependency() = dependency(KobbyTypeAlias.ANY_ENUM)

    /**
     * Select fields where one of the subtypes matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun subTypeDependency(pattern: String) {
        subTypeDependency += pattern.trim()
    }

    /**
     * Select fields where one of the supertypes matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun superTypeDependency(pattern: String) {
        superTypeDependency += pattern.trim()
    }

    /**
     * Select fields where one of the argument types matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun argumentDependency(pattern: String) {
        argumentDependency += pattern.trim()
    }

    /** Select fields where one of the arguments is of type scalar. */
    fun anyScalarArgumentDependency() = argumentDependency(KobbyTypeAlias.ANY_SCALAR)

    /** Select fields where one of the arguments is of type enum. */
    fun anyEnumArgumentDependency() = argumentDependency(KobbyTypeAlias.ANY_ENUM)

    /** Select fields where one of the arguments is of type input object. */
    fun anyInputArgumentDependency() = argumentDependency(KobbyTypeAlias.ANY_INPUT)

    /**
     * Select fields where one of the transitive dependencies matches the `pattern`.
     *
     * The transitive dependencies of a GraphQL field are all types (except scalars)
     * returned by a query on that field, as well as the types of the field's arguments.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun transitiveDependency(pattern: String) {
        transitiveDependency += pattern.trim()
    }

    /**
     * Select fields whose weight is greater than or equal to the `minWeight`.
     *
     * The weight of a GraphQL type is the number of types (excluding scalars)
     * that are available for querying on a field that returns the given type.
     *
     * The weight of a GraphQL field is the weight of its type plus the weights of the types of its arguments.
     *
     * @param minWeight Minimum weight of the target field.
     */
    fun minWeight(minWeight: Int) {
        this.minWeight = minWeight
    }

    /**
     * Select fields whose weight is less than or equal to the `maxWeight`.
     *
     * The weight of a GraphQL type is the number of types (excluding scalars)
     * that are available for querying on a field that returns the given type.
     *
     * The weight of a GraphQL field is the weight of its type plus the weights of the types of its arguments.
     *
     * @param maxWeight Maximum weight of the target field.
     */
    fun maxWeight(maxWeight: Int) {
        this.maxWeight = maxWeight
    }
}
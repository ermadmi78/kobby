package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.*
import java.util.regex.Pattern

/**
 * Type query. The part of the query that is responsible for selecting fields in the target GraphQL type.
 *
 * Created on 10.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@KobbyScope
class KobbyTypeQueryScope(
    schema: KobbySchema,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    patternCache: MutableMap<String, Pattern>,
    private val defaultResult: Boolean
) : AbstractKobbyQueryScope(schema, regexEnabled, caseSensitive, patternCache) {
    private var fieldAffected: Boolean = false
    private var enumValueAffected: Boolean = false

    private var minWeight: Int? = null
    private var maxWeight: Int? = null
    private val fieldPredicates = mutableListOf<(KobbyField) -> Boolean>()
    private val enumValuePredicates = mutableListOf<(KobbyEnumValue) -> Boolean>()

    private fun fieldMatches(field: KobbyField): Boolean {
        if (!fieldAffected) {
            return defaultResult
        }

        val minW = minWeight
        val maxW = maxWeight
        if (minW != null && maxW != null) {
            val weight = field.weight
            if (minW <= weight && weight <= maxW) {
                return true
            }
        } else if (minW != null) {
            if (minW <= field.weight) {
                return true
            }
        } else if (maxW != null) {
            if (field.weight <= maxW) {
                return true
            }
        }

        for (predicate in fieldPredicates) {
            if (predicate(field)) {
                return true
            }
        }

        return false
    }

    private fun enumValueMatches(enumValue: KobbyEnumValue): Boolean {
        if (!enumValueAffected) {
            return defaultResult
        }

        for (predicate in enumValuePredicates) {
            if (predicate(enumValue)) {
                return true
            }
        }

        return false
    }

    @Suppress("FunctionName")
    fun _buildPredicate(): KobbyTypePredicate =
        ::fieldMatches to ::enumValueMatches

    /**
     * Select enum values whose name matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun enumValue(pattern: String) {
        enumValueAffected = true
        if (pattern.isNotBlank()) {
            val regexPattern = buildRegex(pattern.trim())
            enumValuePredicates += { enumValue ->
                regexPattern.matcher(enumValue.name).matches()
            }
        }
    }

    /**
     * Select fields whose name matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun field(pattern: String) {
        fieldAffected = true
        if (pattern.isNotBlank()) {
            val regexPattern = buildRegex(pattern.trim())
            fieldPredicates += { field ->
                regexPattern.matcher(field.name).matches()
            }
        }
    }

    /** Select fields that override any fields from the supertype. */
    fun anyOverriddenField() {
        fieldAffected = true
        fieldPredicates += { field ->
            field.isOverride
        }
    }

    override fun findNodesByPattern(pattern: String, action: (KobbyNode) -> Unit) {
        fieldAffected = true
        super.findNodesByPattern(pattern, action)
    }

    /**
     * Select fields whose type matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun dependency(pattern: String) {
        fieldAffected = true

        val kind = KobbyTypeAlias.kindOf(pattern)
        if (kind != null) {
            if (kind != INPUT) {
                fieldPredicates += { field ->
                    field.type.node.kind == kind
                }
            }
        } else {
            findNodesByPattern(pattern) { node ->
                if (node.kind != INPUT) {
                    fieldPredicates += { field ->
                        field.type.node == node
                    }
                }
            }
        }
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
    fun subTypeDependency(pattern: String) = findNodesByPattern(pattern) { node ->
        when (node.kind) {
            OBJECT, INTERFACE -> {
                fieldPredicates += { field ->
                    field.type.node.subTree.contains(node)
                }
            }

            SCALAR, UNION, ENUM, INPUT -> {
                // Do nothing
            }
        }
    }

    /**
     * Select fields where one of the supertypes matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun superTypeDependency(pattern: String) = findNodesByPattern(pattern) { node ->
        when (node.kind) {
            INTERFACE, UNION -> {
                fieldPredicates += { field ->
                    field.type.node.implements.contains(node)
                }
            }

            SCALAR, OBJECT, ENUM, INPUT -> {
                // Do nothing
            }
        }
    }

    /**
     * Select fields where one of the argument types matches the `pattern`.
     *
     * @param pattern `?` matches one character, `*` matches zero or more characters, `|` OR operator.
     */
    fun argumentDependency(pattern: String) = findNodesByPattern(pattern) { node ->
        when (node.kind) {
            SCALAR, ENUM, INPUT -> {
                fieldPredicates += { field ->
                    field.argumentDependencies.contains(node)
                }
            }

            OBJECT, INTERFACE, UNION -> {
                // Do nothing
            }
        }
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
    fun transitiveDependency(pattern: String) = findNodesByPattern(pattern) { node ->
        when (node.kind) {
            SCALAR, ENUM, INPUT -> {
                fieldPredicates += { field ->
                    field.type.node.transitiveDependencies.contains(node) || field.argumentDependencies.contains(node)
                }
            }

            OBJECT, INTERFACE, UNION -> {
                fieldPredicates += { field ->
                    field.type.node.transitiveDependencies.contains(node)
                }
            }
        }
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
        fieldAffected = true
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
        fieldAffected = true
        this.maxWeight = maxWeight
    }
}
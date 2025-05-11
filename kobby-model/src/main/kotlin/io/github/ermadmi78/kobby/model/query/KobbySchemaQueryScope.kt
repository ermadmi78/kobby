package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.SCALAR
import java.util.regex.Pattern

/**
 * Schema query. The part of the query that is responsible for selecting the GraphQL types in which to look for fields.
 *
 * Created on 14.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@KobbyScope
class KobbySchemaQueryScope(
    schema: KobbySchema,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    patternCache: MutableMap<String, Pattern> = mutableMapOf()
) : AbstractKobbyQueryScope(schema, regexEnabled, caseSensitive, patternCache) {
    private val typeScopes = mutableMapOf<KobbyNode, KobbyTypeOperationQueryScope>()

    @Suppress("FunctionName")
    fun _affectBoth(): KobbySchemaQueryScope {
        typeScopes.values.forEach { scope ->
            scope._affectBoth()
        }
        return this
    }

    @Suppress("FunctionName")
    fun _buildMap(): Map<KobbyNode, KobbyTypePredicate> =
        typeScopes.mapValues { entry -> entry.value._buildPredicate() }

    @Suppress("FunctionName")
    fun _buildPredicate(): KobbyTypePredicate {
        val predicatesByNode: Map<KobbyNode, KobbyTypePredicate> = _buildMap()

        val fieldPredicate: (KobbyField) -> Boolean = { field ->
            predicatesByNode[field.node]?.let { (nodeFieldPredicate, _) ->
                nodeFieldPredicate(field)
            } ?: true
        }

        val enumValuePredicate: (KobbyEnumValue) -> Boolean = { enumValue ->
            predicatesByNode[enumValue.node]?.let { (_, nodeEnumValuePredicate) ->
                nodeEnumValuePredicate(enumValue)
            } ?: true
        }

        return fieldPredicate to enumValuePredicate
    }

    /**
     * Apply the operation subquery to all types in the GraphQL schema that match the `pattern`.
     *
     * @param pattern Pattern to choose types in the schema
     *                (`?` - matches one character, `*` - matches zero or more characters, `|` - OR operator).
     * @param block operation subquery
     */
    fun forType(
        pattern: String,
        block: KobbyTypeOperationQueryScope.() -> Unit
    ) = findNodesByPattern(pattern) { node ->
        if (node.kind != SCALAR) {
            block(typeScopes.getOrPut(node) {
                KobbyTypeOperationQueryScope(schema, regexEnabled, caseSensitive, patternCache)
            })
        }
    }

    /**
     * Apply the operation subquery to a `Query` type in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forQuery(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.QUERY, block)

    /**
     * Apply the operation subquery to a `Mutation` type in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forMutation(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.MUTATION, block)

    /**
     * Apply the operation subquery to a `Subscription` type in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forSubscription(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.SUBSCRIPTION, block)

    /**
     * Apply the operation subquery to root types in the GraphQL schema - `Query`, `Mutation` and `Subscription`.
     *
     * @param block operation subquery
     */
    fun forRoot(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ROOT, block)

    /**
     * Apply the operation subquery to any type in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAny(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY, block)

    /**
     * Apply the operation subquery to any object in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAnyObject(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY_OBJECT, block)

    /**
     * Apply the operation subquery to any interface in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAnyInterface(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY_INTERFACE, block)

    /**
     * Apply the operation subquery to any union in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAnyUnion(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY_UNION, block)

    /**
     * Apply the operation subquery to any enum in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAnyEnum(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY_ENUM, block)

    /**
     * Apply the operation subquery to any input object in the GraphQL schema.
     *
     * @param block operation subquery
     */
    fun forAnyInput(block: KobbyTypeOperationQueryScope.() -> Unit) =
        forType(KobbyTypeAlias.ANY_INPUT, block)
}
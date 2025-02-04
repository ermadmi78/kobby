package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyEnumValue
import io.github.ermadmi78.kobby.model.KobbyField
import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.KobbyScope
import java.util.regex.Pattern

/**
 * Operation query. The part of the query responsible for applying the type subquery to the target GraphQL type.
 *
 * Created on 14.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@KobbyScope
class KobbyTypeOperationQueryScope(
    schema: KobbySchema,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    patternCache: MutableMap<String, Pattern>
) {
    private val includeScope = KobbyTypeQueryScope(
        schema,
        regexEnabled,
        caseSensitive,
        patternCache,
        defaultResult = true
    )

    private val excludeScope = KobbyTypeQueryScope(
        schema,
        regexEnabled,
        caseSensitive,
        patternCache,
        defaultResult = false
    )

    @Suppress("FunctionName")
    fun _buildPredicate(): KobbyTypePredicate {
        val (includeFieldPredicate, includeEnumValuePredicate) = includeScope._buildPredicate()
        val (excludeFieldPredicate, excludeEnumValuePredicate) = excludeScope._buildPredicate()

        val fieldPredicate: (KobbyField) -> Boolean = { field ->
            when {
                excludeFieldPredicate(field) -> {
                    false
                }

                includeFieldPredicate(field) -> {
                    true
                }

                else -> {
                    false
                }
            }
        }

        val enumValuePredicate: (KobbyEnumValue) -> Boolean = { enumValue ->
            when {
                excludeEnumValuePredicate(enumValue) -> {
                    false
                }

                includeEnumValuePredicate(enumValue) -> {
                    true
                }

                else -> {
                    false
                }
            }
        }

        return fieldPredicate to enumValuePredicate
    }

    /**
     * Include operation.
     * Apply the type subquery to a target GraphQL type.
     * Put fields of the target type that match the subquery into the query result.
     *
     * @param block type subquery
     */
    fun include(block: KobbyTypeQueryScope.() -> Unit) {
        block(includeScope)
    }

    /**
     * Exclude operation.
     * Apply the type subquery to a target GraphQL type.
     * Put fields of the target type that do not match the subquery into the query result.
     *
     * @param block type subquery
     */
    fun exclude(block: KobbyTypeQueryScope.() -> Unit) {
        block(excludeScope)
    }
}
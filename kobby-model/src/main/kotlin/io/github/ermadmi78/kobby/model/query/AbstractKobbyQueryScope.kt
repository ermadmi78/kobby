package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbySchema
import java.util.regex.Pattern

/**
 * Created on 11.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
abstract class AbstractKobbyQueryScope(
    protected val schema: KobbySchema,
    protected val regexEnabled: Boolean,
    protected val caseSensitive: Boolean,
    protected val patternCache: MutableMap<String, Pattern>
) {
    protected fun buildRegex(pattern: String): Pattern = patternCache.getOrPut(pattern) {
        Pattern.compile(
            if (regexEnabled) pattern.trim() else kobbyPatternToRegex(pattern),
            if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
        )
    }

    protected open fun findNodesByPattern(pattern: String, action: (KobbyNode) -> Unit) {
        if (!regexEnabled && '|' in pattern) {
            pattern.splitToSet().forEach { subPattern ->
                findNodesByPatternImpl(subPattern, action)
            }
        } else {
            findNodesByPatternImpl(pattern, action)
        }
    }

    private fun findNodesByPatternImpl(pattern: String, action: (KobbyNode) -> Unit) {
        when (pattern) {
            KobbyTypeAlias.QUERY -> {
                action(schema.query)
                return
            }

            KobbyTypeAlias.MUTATION -> {
                action(schema.mutation)
                return
            }

            KobbyTypeAlias.SUBSCRIPTION -> {
                action(schema.subscription)
                return
            }

            KobbyTypeAlias.ROOT -> {
                action(schema.query)
                action(schema.mutation)
                action(schema.subscription)
                return
            }

            KobbyTypeAlias.ANY -> {
                schema.all(action)
                return
            }

            KobbyTypeAlias.ANY_SCALAR -> {
                schema.scalars(action)
                return
            }

            KobbyTypeAlias.ANY_OBJECT -> {
                schema.objects(action)
                return
            }

            KobbyTypeAlias.ANY_INTERFACE -> {
                schema.interfaces(action)
                return
            }

            KobbyTypeAlias.ANY_UNION -> {
                schema.unions(action)
                return
            }

            KobbyTypeAlias.ANY_ENUM -> {
                schema.enums(action)
                return
            }

            KobbyTypeAlias.ANY_INPUT -> {
                schema.inputs(action)
                return
            }
        }

        val kind: PatternKind = pattern.kind
        if (kind == PatternKind.NOTHING) {
            return
        }

        if (kind == PatternKind.PATTERN || regexEnabled || !caseSensitive) {
            val regex = buildRegex(pattern)
            schema.all { node ->
                if (regex.matcher(node.name).matches()) {
                    action(node)
                }
            }

            return
        }

        if (kind == PatternKind.LIST_OF_TYPES) {
            pattern.splitToSet().forEach { nodeName ->
                val node = schema.all[nodeName]
                if (node != null) {
                    action(node)
                }
            }

            return
        }

        require(kind == PatternKind.TYPE) { "Invalid algorithm" }

        val node = schema.all[pattern.pack()]
        if (node != null) {
            action(node)
        }
    }
}
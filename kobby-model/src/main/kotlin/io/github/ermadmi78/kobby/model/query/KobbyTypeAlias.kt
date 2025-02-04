package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyNodeKind

/**
 * Created on 14.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
object KobbyTypeAlias {
    const val QUERY = "__query"
    const val MUTATION = "__mutation"
    const val SUBSCRIPTION = "__subscription"

    const val ROOT = "__root"
    const val ANY = "__any"

    const val ANY_SCALAR = "__anyScalar"
    const val ANY_OBJECT = "__anyObject"
    const val ANY_INTERFACE = "__anyInterface"
    const val ANY_UNION = "__anyUnion"
    const val ANY_ENUM = "__anyEnum"
    const val ANY_INPUT = "__anyInput"

    fun kindOf(alias: String): KobbyNodeKind? {
        return when (alias) {
            ANY_SCALAR -> KobbyNodeKind.SCALAR
            ANY_OBJECT -> KobbyNodeKind.OBJECT
            ANY_INTERFACE -> KobbyNodeKind.INTERFACE
            ANY_UNION -> KobbyNodeKind.UNION
            ANY_ENUM -> KobbyNodeKind.ENUM
            ANY_INPUT -> KobbyNodeKind.INPUT
            else -> null
        }
    }
}
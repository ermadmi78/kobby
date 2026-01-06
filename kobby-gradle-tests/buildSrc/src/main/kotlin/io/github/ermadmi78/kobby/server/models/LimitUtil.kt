package io.github.ermadmi78.kobby.server.models

fun Int?.asLimit(): Int = when (val limit = this) {
    null -> 10
    else -> if (limit < 0) Int.MAX_VALUE else limit
}

package io.github.ermadmi78.kobby.server.models

interface Entity {
    val id: Long
    suspend fun fields(keys: List<String>? = null): Map<String, Any>
}


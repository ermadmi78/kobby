package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.server.operations.Query
import io.github.ermadmi78.kobby.server.models.Actor
import io.github.ermadmi78.kobby.server.models.Film
import io.github.ermadmi78.kobby.server.models.Taggable

class TaggableQueryService : Query {

    suspend fun taggable(tag: String): List<Taggable> = buildList {
        addAll(Film.all().filter { it.containsTag(tag) })
        addAll(Actor.all().filter { it.containsTag(tag) })
    }
}

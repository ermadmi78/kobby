package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Query
import io.github.ermadmi78.kobby.server.models.Film
import io.github.ermadmi78.kobby.server.models.Film.Companion.accepted
import io.github.ermadmi78.kobby.server.models.Genre

class FilmQueryService : Query {

    suspend fun film(id: ID): Film? = Film.get(id)

    suspend fun films(
        title: String? = null,
        genre: Genre? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Film> = Film.all().accepted(title, genre, limit, offset)
}

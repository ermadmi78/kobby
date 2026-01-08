package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.server.operations.Query
import io.github.ermadmi78.kobby.server.models.Actor
import io.github.ermadmi78.kobby.server.models.Actor.Companion.accepted
import io.github.ermadmi78.kobby.server.models.Film.Companion.accepted
import io.github.ermadmi78.kobby.server.models.Gender
import java.time.LocalDate

class ActorQueryService : Query {

    suspend fun actor(id: Long): Actor? = Actor.get(id)

    suspend fun actors(
        firstName: String? = null,
        lastName: String? = null,
        birthdayFrom: LocalDate? = null,
        birthdayTo: LocalDate? = null,
        gender: Gender? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Actor> = Actor.all().accepted(firstName, lastName, birthdayFrom, birthdayTo, gender, limit, offset)
}

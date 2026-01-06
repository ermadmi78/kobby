package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Mutation
import io.github.ermadmi78.kobby.server.models.*
import java.time.LocalDate

class MutationsService : Mutation {
    suspend fun createCountry(name: String) = Country.create(name)

    suspend fun createActor(countryId: ID, actor: ActorInput, tags: TagInput? = null) =
        Actor.create(countryId.value.toLong(), actor, tags)

    suspend fun createFilm(countryId: ID, film: FilmInput, tags: TagInput? = null) =
        Film.create(countryId.value.toLong(), film, tags)

    suspend fun associate(filmId: ID, actorId: ID): Boolean {
        val film = Film.get(filmId) ?: return false
        val actor = Actor.get(actorId) ?: return false

        return if (film.actors.any { it.id == actor.id }) false else {
            film.actors.add(actor)
            true
        }
    }

    suspend fun tagFilm(filmId: ID, tagValue: String): Boolean {
        val film = Film.get(filmId) ?: return false
        return if (film.tags.any { it.value == tagValue }) false else {
            film.tags.add(Tag(tagValue))
            true
        }
    }

    suspend fun tagActor(actorId: ID, tagValue: String): Boolean {
        val actor = Actor.get(actorId) ?: return false
        return if (actor.tags.any { it.value == tagValue }) false else {
            actor.tags.add(Tag(tagValue))
            true
        }
    }

    suspend fun updateBirthday(actorId: ID, birthday: LocalDate): Actor? {
        val actor = Actor.get(actorId) ?: return null
        return actor.apply {
            this.birthday = birthday
        }
    }

    suspend fun truncateMutations(): Boolean {
        Actor.truncate()
        Film.truncate()
        Country.truncate()
        return true
    }
}

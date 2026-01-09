package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.server.operations.Mutation
import io.github.ermadmi78.kobby.server.models.*
import java.time.LocalDate

class MutationsService : Mutation {
    suspend fun createCountry(name: String) = Country.create(name)

    suspend fun createActor(countryId: Long, actor: ActorInput, tags: TagInput? = null) =
        Actor.create(countryId, actor, tags)

    suspend fun createFilm(countryId: Long, film: FilmInput, tags: TagInput? = null) =
        Film.create(countryId, film, tags)

    suspend fun associate(filmId: Long, actorId: Long): Boolean {
        val film = Film.get(filmId) ?: return false
        val actor = Actor.get(actorId) ?: return false

        return if (film.actors.any { it.id == actor.id }) false else {
            film.actors.add(actor)
            true
        }
    }

    suspend fun tagFilm(filmId: Long, tagValue: String): Boolean {
        val film = Film.get(filmId) ?: return false
        return if (film.tags.any { it.value == tagValue }) false else {
            film.tags.add(Tag(tagValue))
            true
        }
    }

    suspend fun tagActor(actorId: Long, tagValue: String): Boolean {
        val actor = Actor.get(actorId) ?: return false
        return if (actor.tags.any { it.value == tagValue }) false else {
            actor.tags.add(Tag(tagValue))
            true
        }
    }

    suspend fun updateBirthday(actorId: Long, birthday: LocalDate): Actor? {
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

package io.github.ermadmi78.kobby.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import io.github.ermadmi78.kobby.server.controller.SubscriptionService.Companion.emit
import io.github.ermadmi78.kobby.server.models.Actor.Companion.accepted
import io.github.ermadmi78.kobby.server.models.asLimit
import java.time.LocalDate

data class Film(
    override val id: Long,
    override val tags: MutableList<Tag>,
    val title: String,
    val genre: Genre,
    val countryId: Long,

    @GraphQLIgnore
    val actors: MutableList<Actor>,
): Entity, Taggable {
    companion object {

        private fun actors(vararg ids: Long): MutableList<Actor> = ids
            .map { Actor.get(it) }
            .filterNotNull()
            .toMutableList()

        private fun initial() = mutableListOf(
            Film(1, title = "Amelie", countryId = 8, genre = Genre.COMEDY, tags = mutableListOf(Tag("best"), Tag("audrey")), actors = actors(1, 2, 3, 4)),
            Film(2, title = "A Very Long Engagement", countryId = 8, genre = Genre.DRAMA, tags = mutableListOf(Tag("audrey")), actors = actors(1, 4, 5)),
            Film(3, title = "Hunting and Gathering", countryId = 8, genre = Genre.DRAMA, tags = mutableListOf(Tag("audrey")), actors = actors(1, 6)),
            Film(4, title = "Priceless", countryId = 8, genre = Genre.COMEDY, tags = mutableListOf(Tag("best"), Tag("house")), actors = actors(1, 7)),
            Film(5, title = "House", countryId = 18, genre = Genre.COMEDY, tags = mutableListOf(Tag("best"), Tag("house")), actors = actors(8)),
            Film(6, title = "Peter's Friends", countryId = 18, genre = Genre.COMEDY, tags = mutableListOf(Tag("house")), actors = actors(8, 9)),
            Film(7, title = "Street Kings", countryId = 18, genre = Genre.THRILLER, tags = mutableListOf(Tag("house")), actors = actors(8, 10)),
            Film(8, title = "Mr. Pip", countryId = 18, genre = Genre.DRAMA, tags = mutableListOf(Tag("house")), actors = actors(8)),
            Film(9, title = "Ocean's Eleven", countryId = 19, genre = Genre.THRILLER, tags = mutableListOf(Tag("best"), Tag("julia"), Tag("clooney")), actors = actors(11, 12, 13)),
            Film(10, title = "Stepmom", countryId = 19, genre = Genre.DRAMA, tags = mutableListOf(Tag("julia")), actors = actors(11, 14)),
            Film(11, title = "Pretty Woman", countryId = 19, genre = Genre.COMEDY, tags = mutableListOf(Tag("julia")), actors = actors(11, 15)),
            Film(12, title = "From Dusk Till Dawn", countryId = 19, genre = Genre.THRILLER, tags = mutableListOf(Tag("clooney")), actors = actors(12, 16)),
        )

        private var films = initial()
        fun truncate() { this.films = initial() }
        fun all(): List<Film> = films
        fun get(id: Long): Film? = films.firstOrNull { it.id == id }

        suspend fun create(countryId: Long, film: FilmInput, tags: TagInput? = null): Film {
            val maxId = films.maxOfOrNull { it.id } ?: 0
            return Film(
                maxId.inc(),
                tags?.let { mutableListOf(Tag(it.value)) } ?: mutableListOf(),
                film.title,
                film.genre ?: Genre.DRAMA,
                countryId,
                mutableListOf()
            ).also {
                films.add(it)
                emit(it)
            }
        }

        suspend fun List<Film>.accepted(
            title: String? = null,
            genre: Genre? = null,
            limit: Int?,
            offset: Int?
        ): List<Film> = filter { title == null || it.title.contains(title, true) }
            .filter { genre == null || it.genre == genre }
            .drop(offset ?: 0)
            .take((limit.asLimit()))
    }

    override suspend fun fields(keys: List<String>?): Map<String, Any> {
        return buildMap {
            (keys?.toSet() ?: setOf("id", "title", "genre")).forEach {
                when (it) {
                    "id" -> put(it, id)
                    "title" -> put(it, title)
                    "genre" -> put(it, genre.name)
                }
            }
        }
    }

    suspend fun country(): Country = Country.get(countryId)!!
    suspend fun actors(
        firstName: String? = null,
        lastName: String? = null,
        birthdayFrom: LocalDate? = null,
        birthdayTo: LocalDate? = null,
        gender: Gender? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Actor> = actors.accepted(firstName, lastName, birthdayFrom, birthdayTo, gender, limit, offset)
}

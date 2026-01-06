package io.github.ermadmi78.kobby.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLUnion
import com.expediagroup.graphql.generator.scalars.ID
import io.github.ermadmi78.kobby.server.controller.SubscriptionService.Companion.emit
import io.github.ermadmi78.kobby.server.models.Actor.Companion.accepted
import io.github.ermadmi78.kobby.server.models.Film.Companion.accepted
import io.github.ermadmi78.kobby.server.models.asLimit
import java.time.LocalDate

data class Country(
    override val id: Long,
    val name: String
) : Entity {
    companion object {
        private fun initial() = mutableListOf<Country>(
            Country(1, name = "Argentina"),
            Country(2, name = "Australia"),
            Country(3, name = "Austria"),
            Country(4, name = "Belgium"),
            Country(5, name = "Brazil"),
            Country(6, name = "Canada"),
            Country(7, name = "Finland"),
            Country(8, name = "France"),
            Country(9, name = "Germany"),
            Country(10, name = "Italy"),
            Country(11, name = "Japan"),
            Country(12, name = "New Zealand"),
            Country(13, name = "Norway"),
            Country(14, name = "Portugal"),
            Country(15, name = "Russia"),
            Country(16, name = "Spain"),
            Country(17, name = "Sweden"),
            Country(18, name = "United Kingdom"),
            Country(19, name = "USA"),
        )

        private var countries = initial()
        fun truncate() { this.countries = initial() }
        fun all(): List<Country> = countries
        fun get(id: ID): Country? = countries.firstOrNull { it.id == id.value.toLong() }
        suspend fun create(name: String): Country {
            val maxId = countries.maxOfOrNull { it.id } ?: 0
            return Country(maxId.inc(), name).also {
                countries.add(it)
                emit(it)
            }
        }

        fun List<Country>.accepted(
            name: String? = null,
            limit: Int? = null,
            offset: Int? = null,
        ): List<Country> = filter { name == null || it.name.contains(name, true) }
            .drop(offset ?: 0)
            .take(limit.asLimit())
    }

    suspend fun film(id: ID): Film? = films().firstOrNull { it.id == id.value.toLong() }
    suspend fun films(
        title: String? = null,
        genre: Genre? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): List<Film> = Film.all().filter { it.countryId == id }
        .accepted(title, genre, limit, offset)

    suspend fun actor(id: ID): Actor? = films().flatMap { film -> film.actors.filter { it.id == id.value.toLong() } }.firstOrNull()
    suspend fun actors(
        firstName: String? = null,
        lastName: String? = null,
        birthdayFrom: LocalDate? = null,
        birthdayTo: LocalDate? = null,
        gender: Gender? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Actor> = Actor.all().filter { it.countryId == id }
        .accepted(firstName, lastName, birthdayTo, birthdayFrom, gender, limit, offset)

    override suspend fun fields(keys: List<String>?): Map<String, Any> {
        return buildMap {
            (keys?.toSet() ?: setOf("id", "name")).forEach {
                when (it) {
                    "id" -> put(it, id)
                    "name" -> put(it, name)
                }
            }
        }
    }

    @GraphQLUnion(
        name = "Native",
        possibleTypes = [Film::class, Actor::class]
    )
    suspend fun native(): List<Any> = buildList {
        addAll(films())
        addAll(actors())
    }
}

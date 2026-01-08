package io.github.ermadmi78.kobby.server.models

import io.github.ermadmi78.kobby.server.controller.SubscriptionService.Companion.emit
import io.github.ermadmi78.kobby.server.models.Film.Companion.accepted
import io.github.ermadmi78.kobby.server.models.asLimit
import java.time.LocalDate
import java.time.LocalDate.of

data class Actor(
    override val id: Long,
    override val tags: MutableList<Tag>,
    val firstName: String,
    val lastName: String?,
    var birthday: LocalDate,
    val gender: Gender,
    val countryId: Long
) : Entity, Taggable {
    companion object {
        private fun initial() = mutableListOf(
            Actor(id = 1,  countryId = 8,  firstName = "Audrey",    lastName = "Tautou",    birthday = of(1976, 8, 9),  gender = Gender.FEMALE, tags = mutableListOf(Tag("best"), Tag("audrey"))),
            Actor(id = 2,  countryId = 8,  firstName = "Mathieu",   lastName = "Kassovitz", birthday = of(1967, 8, 3),  gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 3,  countryId = 8,  firstName = "Jamel",     lastName = "Debbouze",  birthday = of(1975, 6, 18), gender = Gender.MALE,   tags = mutableListOf(Tag("best"))),
            Actor(id = 4,  countryId = 8,  firstName = "Dominique", lastName = "Pinon",     birthday = of(1955, 3, 4),  gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 5,  countryId = 8,  firstName = "Gaspard",   lastName = "Ulliel",    birthday = of(1984, 11, 25),gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 6,  countryId = 8,  firstName = "Guillaume", lastName = "Canet",     birthday = of(1973, 4, 10), gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 7,  countryId = 8,  firstName = "Gad",       lastName = "Elmaleh",   birthday = of(1971, 4, 19), gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 8,  countryId = 18, firstName = "Hugh",      lastName = "Laurie",    birthday = of(1959, 6, 11), gender = Gender.MALE,   tags = mutableListOf(Tag("best"), Tag("house"))),
            Actor(id = 9,  countryId = 18, firstName = "Stephen",   lastName = "Fry",       birthday = of(1957, 8, 24), gender = Gender.MALE,   tags = mutableListOf(Tag("best"))),
            Actor(id = 10, countryId = 19, firstName = "Keanu",     lastName = "Reeves",    birthday = of(1964, 9, 2),  gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 11, countryId = 19, firstName = "Julia",     lastName = "Roberts",   birthday = of(1967, 10, 28),gender = Gender.FEMALE, tags = mutableListOf(Tag("best"), Tag("julia"))),
            Actor(id = 12, countryId = 19, firstName = "George",    lastName = "Clooney",   birthday = of(1967, 10, 28),gender = Gender.MALE,   tags = mutableListOf(Tag("best"), Tag("clooney"))),
            Actor(id = 13, countryId = 19, firstName = "Brad",      lastName = "Pitt",      birthday = of(1963, 12, 18),gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 14, countryId = 19, firstName = "Susan",     lastName = "Sarandon",  birthday = of(1946, 10, 4), gender = Gender.FEMALE, tags = mutableListOf()),
            Actor(id = 15, countryId = 19, firstName = "Richard",   lastName = "Gere",      birthday = of(1949, 8, 31), gender = Gender.MALE,   tags = mutableListOf()),
            Actor(id = 16, countryId = 19, firstName = "Salma",     lastName = "Hayek",     birthday = of(1966, 9, 2),  gender = Gender.FEMALE, tags = mutableListOf())
        )

        private var actors = initial()
        fun truncate() { this.actors = initial() }
        fun all(): List<Actor> = actors
        fun get(id: Long): Actor? = actors.firstOrNull { it.id == id }
        suspend fun create(countryId: Long, actor: ActorInput, tags: TagInput? = null): Actor {
            val maxId = actors.maxOfOrNull { it.id } ?: 0
            return Actor(
                maxId.inc(),
                tags?.let { mutableListOf(Tag(it.value)) } ?: mutableListOf(),
                actor.firstName,
                actor.lastName,
                actor.birthday,
                actor.gender,
                countryId
            ).also {
                actors.add(it)
                emit(it)
            }
        }

        fun List<Actor>.accepted(
            firstName: String? = null,
            lastName: String? = null,
            birthdayFrom: LocalDate? = null,
            birthdayTo: LocalDate? = null,
            gender: Gender? = null,
            limit: Int? = null,
            offset: Int? = null
        ): List<Actor> = filter { firstName == null || it.firstName.contains(firstName, true) }
            .filter { lastName == null || it.lastName?.contains(lastName, true) == true }
            .filter { gender == null || it.gender == gender }
            .filter { birthdayFrom == null || it.birthday > birthdayFrom }
            .filter { birthdayTo == null || it.birthday < birthdayTo }
            .drop(offset ?: 0)
            .take(limit.asLimit())
    }

    override suspend fun fields(keys: List<String>?): Map<String, Any> {
        return buildMap {
            (keys?.toSet() ?: setOf("id", "firstName", "lastName", "birthday", "gender")).forEach {
                when (it) {
                    "id" -> put(it, id)
                    "firstName" -> put(it, firstName)
                    "lastName" -> put(it, lastName ?: "null")
                    "birthday" -> put(it, birthday.toString())
                    "gender" -> put(it, gender.name)
                }
            }
        }
    }

    suspend fun country(): Country = Country.get(countryId)!!

    suspend fun films(
        title: String? = null,
        genre: Genre? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Film> = Film.all().filter { film -> film.actors.any { it.id == this.id } }
        .accepted(title, genre, limit, offset)
}
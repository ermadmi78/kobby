import ContextHolder.context
import entity.createActor
import entity.createFilm
import entity.onActorCreated
import entity.onFilmCreated
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import kobby.kotlin.dto.ActorInput
import kobby.kotlin.dto.FilmInput
import kobby.kotlin.dto.Gender.FEMALE
import kobby.kotlin.dto.Gender.MALE
import kobby.kotlin.dto.Genre.*
import java.time.LocalDate

class CinemaSubscriptionsTest : AnnotationSpec() {

    @BeforeAll
    suspend fun setUp() {
        context.mutation { truncateMutations }
    }

    @Test
    suspend fun subscriptionsByMeansOfGeneratedAPI() {
        // Created countries subscription
        context.subscription {
            countryCreated()
        }.subscribe {
            // Create countries
            context.mutation {
                createCountry("First")
            }
            context.mutation {
                createCountry("Second")
            }
            context.mutation {
                createCountry("Third")
            }

            // Listen created countries
            receive().countryCreated.name shouldBe "First"
            receive().countryCreated.name shouldBe "Second"
            receive().countryCreated.name shouldBe "Third"
        }

        // Created films subscription
        context.subscription {
            filmCreated(2) {
                genre
                country()
            }
        }.subscribe {
            // Create films
            context.mutation {
                createFilm(2, FilmInput("First", COMEDY))
            }
            context.mutation {
                createFilm(6, FilmInput("Second", THRILLER))
            }
            context.mutation {
                createFilm(2, FilmInput("Third", HORROR))
            }

            // Listen created films
            receive().filmCreated.also { first ->
                first.title shouldBe "First"
                first.genre shouldBe COMEDY
                first.country.name shouldBe "Australia"
            }
            receive().filmCreated.also { third ->
                third.title shouldBe "Third"
                third.genre shouldBe HORROR
                third.country.name shouldBe "Australia"
            }
        }

        // Created actors subscription
        context.subscription {
            actorCreated(2) {
                gender
                country()
            }
        }.subscribe {
            // Create actors
            val now = LocalDate.now()
            context.mutation {
                createActor(2, ActorInput {
                    firstName = "First"
                    lastName = "Actress"
                    birthday = now
                    gender = FEMALE
                })
            }
            context.mutation {
                createActor(6, ActorInput {
                    firstName = "Second"
                    lastName = "Actress"
                    birthday = now
                    gender = FEMALE
                })
            }
            context.mutation {
                createActor(2, ActorInput {
                    firstName = "Third"
                    lastName = "Actor"
                    birthday = now
                    gender = MALE
                })
            }

            // Listen created actors
            receive().actorCreated.also { first ->
                first.firstName shouldBe "First"
                first.lastName shouldBe "Actress"
                first.birthday shouldBe now
                first.gender shouldBe FEMALE
                first.country.name shouldBe "Australia"
            }
            receive().actorCreated.also { third ->
                third.firstName shouldBe "Third"
                third.lastName shouldBe "Actor"
                third.birthday shouldBe now
                third.gender shouldBe MALE
                third.country.name shouldBe "Australia"
            }
        }
    }

    @Test
    suspend fun subscriptionsByMeansOfCustomizedAPI() {
        // Created countries subscription
        context.onCountryCreated().subscribe {
            // Create countries
            context.createCountry("First")
            context.createCountry("Second")
            context.createCountry("Third")

            // Listen created countries
            receive().name shouldBe "First"
            receive().name shouldBe "Second"
            receive().name shouldBe "Third"
        }

        // Prepare countries
        val australia = context.fetchCountry(2)
        val canada = context.fetchCountry(6)

        // Created films subscription
        australia.onFilmCreated { genre; country() }.subscribe {
            // Create films
            australia.createFilm(FilmInput("First", COMEDY))
            canada.createFilm(FilmInput("Second", THRILLER))
            australia.createFilm(FilmInput("Third", HORROR))

            // Listen created films
            receive().also { first ->
                first.title shouldBe "First"
                first.genre shouldBe COMEDY
                first.country.name shouldBe "Australia"
            }
            receive().also { third ->
                third.title shouldBe "Third"
                third.genre shouldBe HORROR
                third.country.name shouldBe "Australia"
            }
        }

        // Created actors subscription
        australia.onActorCreated { gender; country() }.subscribe {
            // Create actors
            val now = LocalDate.now()
            australia.createActor(ActorInput {
                firstName = "First"
                lastName = "Actress"
                birthday = now
                gender = FEMALE
            })
            canada.createActor(ActorInput {
                firstName = "Second"
                lastName = "Actress"
                birthday = now
                gender = FEMALE
            })
            australia.createActor(ActorInput {
                firstName = "Third"
                lastName = "Actor"
                birthday = now
                gender = MALE
            })

            // Listen created actors
            receive().also { first ->
                first.firstName shouldBe "First"
                first.lastName shouldBe "Actress"
                first.birthday shouldBe now
                first.gender shouldBe FEMALE
                first.country.name shouldBe "Australia"
            }
            receive().also { third ->
                third.firstName shouldBe "Third"
                third.lastName shouldBe "Actor"
                third.birthday shouldBe now
                third.gender shouldBe MALE
                third.country.name shouldBe "Australia"
            }
        }
    }
}
import kobby.kotlin.CinemaContext
import kobby.kotlin.CinemaReceiver
import kobby.kotlin.CinemaSubscriber
import kobby.kotlin.dto.ActorInput
import kobby.kotlin.dto.FilmInput
import kobby.kotlin.entity.*
import java.time.LocalDate

/**
 * Created on 13.03.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

suspend fun CinemaContext.findCountry(id: Long, __projection: CountryProjection.() -> Unit = {}): Country? =
    query {
        country(id, __projection)
    }.country

suspend fun CinemaContext.fetchCountry(id: Long, __projection: CountryProjection.() -> Unit = {}): Country =
    findCountry(id, __projection)!!

//**********************************************************************************************************************

suspend fun CinemaContext.findFilm(id: Long, __projection: FilmProjection.() -> Unit = {}): Film? =
    query {
        film(id, __projection)
    }.film

suspend fun CinemaContext.fetchFilm(id: Long, __projection: FilmProjection.() -> Unit = {}): Film =
    findFilm(id, __projection)!!

//**********************************************************************************************************************

suspend fun CinemaContext.findActor(id: Long, __projection: ActorProjection.() -> Unit = {}): Actor? =
    query {
        actor(id, __projection)
    }.actor

suspend fun CinemaContext.fetchActor(id: Long, __projection: ActorProjection.() -> Unit = {}): Actor =
    findActor(id, __projection)!!

//**********************************************************************************************************************

suspend fun CinemaContext.createCountry(
    name: String,
    __projection: CountryProjection.() -> Unit = {}
): Country =
    mutation {
        createCountry(name, __projection)
    }.createCountry

suspend fun CinemaContext.createFilm(
    countryId: Long,
    film: FilmInput,
    __query: MutationCreateFilmQuery.() -> Unit = {}
): Film =
    mutation {
        createFilm(countryId, film, __query)
    }.createFilm

suspend fun CinemaContext.createActor(
    countryId: Long,
    actor: ActorInput,
    __query: MutationCreateActorQuery.() -> Unit = {}
): Actor =
    mutation {
        createActor(countryId, actor, __query)
    }.createActor

suspend fun CinemaContext.updateBirthday(
    actorId: Long,
    birthday: LocalDate,
    __projection: ActorProjection.() -> Unit = {}
): Actor? =
    mutation {
        updateBirthday(actorId, birthday, __projection)
    }.updateBirthday

//**********************************************************************************************************************

fun CinemaContext.onCountryCreated(
    __projection: CountryProjection.() -> Unit = {}
): CinemaSubscriber<Country> = CinemaSubscriber {
    subscription {
        countryCreated(__projection)
    }.subscribe {
        it(CinemaReceiver {
            receive().countryCreated
        })
    }
}

fun CinemaContext.onFilmCreated(
    countryId: Long?,
    __projection: FilmProjection.() -> Unit = {}
): CinemaSubscriber<Film> = CinemaSubscriber {
    subscription {
        filmCreated(countryId, __projection)
    }.subscribe {
        it(CinemaReceiver {
            receive().filmCreated
        })
    }
}

fun CinemaContext.onActorCreated(
    countryId: Long?,
    __projection: ActorProjection.() -> Unit = {}
): CinemaSubscriber<Actor> = CinemaSubscriber {
    subscription {
        actorCreated(countryId, __projection)
    }.subscribe {
        it(CinemaReceiver {
            receive().actorCreated
        })
    }
}
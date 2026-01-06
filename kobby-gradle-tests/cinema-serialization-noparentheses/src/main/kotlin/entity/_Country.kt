package entity

import createActor
import createFilm
import kobby.kotlin.CinemaSubscriber
import kobby.kotlin.dto.ActorInput
import kobby.kotlin.dto.FilmInput
import kobby.kotlin.entity.*
import onActorCreated
import onFilmCreated

/**
 * Created on 13.03.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

suspend fun Country.refresh(__projection: (CountryProjection.() -> Unit)? = null): Country = __context().query {
    country(id) {
        __projection?.invoke(this) ?: __withCurrentProjection()
    }
}.country!!

suspend fun Country.findFilm(id: Long, __projection: FilmProjection.() -> Unit = {}): Film? = refresh {
    __minimize() // switch off all default fields to minimize GraphQL response
    film(id, __projection)
}.film

suspend fun Country.fetchFilm(id: Long, __projection: FilmProjection.() -> Unit = {}): Film =
    findFilm(id, __projection)!!

suspend fun Country.findFilms(__query: CountryFilmsQuery.() -> Unit = {}): List<Film> = refresh {
    __minimize() // switch off all default fields to minimize GraphQL response
    films(__query)
}.films

//**********************************************************************************************************************

suspend fun Country.createFilm(film: FilmInput, __query: MutationCreateFilmQuery.() -> Unit = {}): Film =
    __context().createFilm(id, film, __query)

suspend fun Country.createActor(actor: ActorInput, __query: MutationCreateActorQuery.() -> Unit = {}): Actor =
    __context().createActor(id, actor, __query)

//**********************************************************************************************************************

fun Country.onFilmCreated(__projection: FilmProjection.() -> Unit = {}): CinemaSubscriber<Film> =
    __context().onFilmCreated(id, __projection)

fun Country.onActorCreated(__projection: ActorProjection.() -> Unit = {}): CinemaSubscriber<Actor> =
    __context().onActorCreated(id, __projection)
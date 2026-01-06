package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Subscription
import io.github.ermadmi78.kobby.server.models.Actor
import io.github.ermadmi78.kobby.server.models.Country
import io.github.ermadmi78.kobby.server.models.Film
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter


class SubscriptionService : Subscription {

    companion object {
        private fun <T> defaultFlow() = MutableSharedFlow<T>(
            extraBufferCapacity = 3,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        private val countriesFlow = defaultFlow<Country>()
        private val filmsFlow = defaultFlow<Film>()
        private val actorsFlow = defaultFlow<Actor>()

        suspend fun emit(country: Country) = countriesFlow.emit(country)
        suspend fun emit(film: Film) = filmsFlow.emit(film)
        suspend fun emit(actor: Actor) = actorsFlow.emit(actor)
    }

    fun countryCreated(): Flow<Country> = countriesFlow.asSharedFlow()

    fun filmCreated(countryId: ID? = null): Flow<Film> = filmsFlow.asSharedFlow()
        .filter { countryId == null || it.countryId == countryId.value.toLong() }

    fun actorCreated(countryId: ID? = null): Flow<Actor> = actorsFlow.asSharedFlow()
        .filter { countryId == null || it.countryId == countryId.value.toLong() }
}

package io.github.ermadmi78.kobby.server.controller

import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Query
import io.github.ermadmi78.kobby.server.models.Country
import io.github.ermadmi78.kobby.server.models.Country.Companion.accepted

class CountryQueryService : Query {

    suspend fun country(id: ID): Country? = Country.get(id)

    suspend fun countries(
        name: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Country> = Country.all().accepted(name, limit, offset)
}

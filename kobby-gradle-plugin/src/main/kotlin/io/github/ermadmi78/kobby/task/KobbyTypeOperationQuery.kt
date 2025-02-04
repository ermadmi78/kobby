package io.github.ermadmi78.kobby.task

import java.io.Serializable

/**
 * Created on 15.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class KobbyTypeOperationQuery(
    val include: KobbyTypeQuery,
    val exclude: KobbyTypeQuery
) : Serializable {
    constructor() : this(KobbyTypeQuery(), KobbyTypeQuery())
}
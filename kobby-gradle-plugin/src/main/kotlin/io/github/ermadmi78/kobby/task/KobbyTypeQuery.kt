package io.github.ermadmi78.kobby.task

import java.io.Serializable

/**
 * Created on 15.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class KobbyTypeQuery(
    val enumValue: List<String>,
    val field: List<String>,
    val anyOverriddenField: Boolean,
    val dependency: List<String>,
    val subTypeDependency: List<String>,
    val superTypeDependency: List<String>,
    val argumentDependency: List<String>,
    val transitiveDependency: List<String>,
    val minWeight: Int?,
    val maxWeight: Int?
) : Serializable {
    constructor() : this(
        emptyList<String>(),
        emptyList<String>(),
        false,
        emptyList<String>(),
        emptyList<String>(),
        emptyList<String>(),
        emptyList<String>(),
        emptyList<String>(),
        null,
        null
    )
}
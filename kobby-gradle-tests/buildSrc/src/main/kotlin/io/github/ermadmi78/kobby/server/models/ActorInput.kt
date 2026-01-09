package io.github.ermadmi78.kobby.server.models

import java.time.LocalDate

data class ActorInput(
    val firstName: String,
    val lastName: String?,
    val birthday: LocalDate,
    val gender: Gender,
)

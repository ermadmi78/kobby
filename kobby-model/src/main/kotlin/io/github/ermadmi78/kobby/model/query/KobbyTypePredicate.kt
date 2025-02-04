package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyEnumValue
import io.github.ermadmi78.kobby.model.KobbyField

typealias KobbyTypePredicate = Pair<(KobbyField) -> Boolean, (KobbyEnumValue) -> Boolean>
package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 09.03.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
object Cinema : ResourceSchema() {
    val schema: KobbySchema by lazy {
        load()
    }
}
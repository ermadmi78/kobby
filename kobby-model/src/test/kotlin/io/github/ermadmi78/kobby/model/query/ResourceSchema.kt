package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.parseSchema
import java.io.InputStreamReader

/**
 * Created on 22.03.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
abstract class ResourceSchema {
    protected fun load(): KobbySchema = parseSchema(
        emptyMap(),
        InputStreamReader(
            this::class.java.getResourceAsStream(
                this::class.java.simpleName.lowercase() + ".graphqls.txt"
            )!!
        )
    )
}
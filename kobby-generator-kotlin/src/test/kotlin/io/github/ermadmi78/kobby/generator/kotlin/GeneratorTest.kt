package io.github.ermadmi78.kobby.generator.kotlin

import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.ANY
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.MAP
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.STRING
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.parseSchema
import io.kotest.core.spec.style.AnnotationSpec
import java.io.InputStreamReader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class GeneratorTest : AnnotationSpec() {
    @Test
    fun temp() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls"))
        )

        val layout = KotlinLayout(
            KotlinTypes.PREDEFINED_SCALARS + mapOf(
                "DateTime" to KotlinType("java.time", "OffsetDateTime"),
                "JSON" to MAP.parameterize(STRING, ANY.nullable())
            ),
            KotlinContextLayout(
                "kobby",
                "kobby",
                Decoration("Kobby", null),
                "query",
                "mutation"
            ),
            KotlinDtoLayout(
                "kobby.dto",
                Decoration(null, "Dto"),
                KotlinDtoJacksonLayout(true),
                KotlinDtoBuilderLayout(
                    true,
                    Decoration(null, "Builder"),
                ),
                KotlinDtoGraphQLLayout(
                    true,
                    "kobby.dto.graphql",
                    Decoration("GraphQL", null)
                )
            ),
            KotlinEntityLayout(
                true,
                "kobby.entity",
                Decoration(null, null),
                "__withCurrentProjection",
                KotlinEntityProjectionLayout(
                    Decoration(null, "Projection"),
                    "__projection",
                    Decoration(null, null),
                    Decoration("__without", null),
                    "__minimize",
                    Decoration(null, "Qualification"),
                    Decoration(null, "QualifiedProjection"),
                    Decoration("__on", null)
                ),
                KotlinEntitySelectionLayout(
                    Decoration(null, "Selection"),
                    "__selection",
                    Decoration(null, "Query"),
                    "__query"
                )
            ),
            KotlinImplLayout(
                "kobby.entity.impl",
                Decoration(null, "Impl"),
                true,
                Decoration("__inner", null)
            )
        )
        val files = generateKotlin(schema, layout)

//        println("************************************************************************************************")
//        println("DTO:")
//        println("************************************************************************************************")
//        files.forEach {
//            println()
//            it.writeTo(System.out)
//            println("---------")
//        }
    }
}
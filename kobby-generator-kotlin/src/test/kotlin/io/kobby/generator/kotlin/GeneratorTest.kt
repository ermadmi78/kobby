package io.kobby.generator.kotlin

import io.kobby.generator.kotlin.KotlinTypes.ANY
import io.kobby.generator.kotlin.KotlinTypes.MAP
import io.kobby.generator.kotlin.KotlinTypes.STRING
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
        val layout = KotlinGeneratorLayout(
            KotlinDirectiveLayout(
                "default",
                "required"
            ),
            KotlinTypes.PREDEFINED_SCALARS + mapOf(
                "DateTime" to KotlinType("java.time", "OffsetDateTime"),
                "JSON" to MAP.parameterize(STRING, ANY.nullable())
            ),
            KotlinContextLayout(
                "kobby",
                "kobby",
                Decoration("Kobby", null)
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
                KotlinEntityProjectionLayout(
                    Decoration(null, "Projection"),
                    "__projection",
                    Decoration("with", null),
                    Decoration("without", null)
                )
            ),
            KotlinImplLayout(
                "kobby.impl",
                Decoration(null, "Impl")
            )
        )
        val files = generateKotlin(layout, InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")))

        println("************************************************************************************************")
        println("DTO:")
        println("************************************************************************************************")
        files.forEach {
            println()
            it.writeTo(System.out)
            println("---------")
        }
    }
}
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
            KotlinDtoLayout(KotlinPackage("api.dto")),
            KotlinApiLayout(true, KotlinPackage("api")),
            KotlinImplLayout(KotlinPackage("api.impl")),
            KotlinTypes.PREDEFINED_SCALARS + mapOf(
                "DateTime" to KotlinType("java.time", "OffsetDateTime"),
                "JSON" to MAP.parameterize(STRING, ANY.nullable())
            )
        )
        val files = generateKotlin(layout, InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")))

        println("************************************************************************************************")
        println("DTO:")
        println("************************************************************************************************")
        files.dtoFiles.forEach {
            println()
            it.writeTo(System.out)
            println("---------")
        }
    }
}
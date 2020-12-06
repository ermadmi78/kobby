package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
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
        val layout = GeneratorLayout(
            DtoLayout(PackageSpec("api.dto")),
            ApiLayout(PackageSpec("api")),
            ImplLayout(PackageSpec("api.impl")),
            Scalars.PREDEFINED + mapOf(
                "DateTime" to ClassName("java.time", "OffsetDateTime"),
                "JSON" to MAP.parameterizedBy(STRING, ANY.copy(true))
            )
        )
        val files = generate(layout, InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")))

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
package io.kobby.generator

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
            PackageSpec("api.dto"),
            PackageSpec("api"),
            PackageSpec("api.impl")
        )
        val files = generate(layout, InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")))
        println("!!!!")
    }
}
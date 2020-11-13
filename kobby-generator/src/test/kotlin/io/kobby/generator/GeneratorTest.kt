package io.kobby.generator

import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.AnnotationSpec

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class GeneratorTest: AnnotationSpec() {
    @Test
    fun temp() {
        val graphQLSchema = SchemaParser().parse(this.javaClass.getResourceAsStream("kobby.graphqls"))
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!! TESTS !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }
}
package io.kobby.generator.kotlin

import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.Reader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

fun generateKotlin(layout: KotlinGeneratorLayout, vararg schemas: Reader): List<KotlinFile> {
    val graphQLSchema = TypeDefinitionRegistry()
    for (schema in schemas) {
        graphQLSchema.merge(SchemaParser().parse(schema))
    }

    val dto = generateDto(layout, graphQLSchema)


    return dto.files.map { it.toKotlinFile() }
}
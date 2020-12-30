package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.Reader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

fun generateKotlin(layout: KotlinGeneratorLayout, vararg schemas: Reader): KotlinFilesLayout {
    val graphQLSchema = TypeDefinitionRegistry()
    for (schema in schemas) {
        graphQLSchema.merge(SchemaParser().parse(schema))
    }

    val dto = generateDto(layout, graphQLSchema)


    return KotlinFilesLayout(
        dtoFiles = dto.values.map { FileSpec.get(layout.dto.packageName, it).toKotlinFile() }
    )
}
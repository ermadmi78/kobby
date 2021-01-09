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
        dtoFiles = dto.dtoClass.map { (typeName, dtoType) ->
            FileSpec.builder(layout.dto.packageName, dtoType.name!!).apply {
                addType(dtoType)
                dto.builderFunction[typeName]?.also {
                    addFunction(it)
                }
                dto.builderClass[typeName]?.also {
                    addType(it)
                }
            }.build().toKotlinFile()
        }
    )
}
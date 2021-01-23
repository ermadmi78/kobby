package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kobby.model.decorate
import java.io.Reader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

fun generateKotlin(layout: KotlinLayout, vararg schemas: Reader): List<KotlinFile> {
    val graphQLSchema = TypeDefinitionRegistry()
    for (schema in schemas) {
        graphQLSchema.merge(SchemaParser().parse(schema))
    }

    val result = mutableListOf<FileSpec>()

    //******************************************************************************************************************
    //                                   DSL Annotation
    //******************************************************************************************************************

    val contextLayout = layout.context
    val contextFile = FileSpec.builder(contextLayout.packageName, contextLayout.name)
    val dslAnnotation = ClassName(
        contextLayout.packageName,
        "DSL".decorate(contextLayout.decoration)
    )
    contextFile.addType(
        TypeSpec.classBuilder(dslAnnotation)
            .addModifiers(KModifier.ANNOTATION)
            .addAnnotation(DslMarker::class)
            .build()
    )

    //******************************************************************************************************************
    //                                   DTO
    //******************************************************************************************************************

    val dto = generateDto(layout, graphQLSchema, dslAnnotation)
    result += dto.files

    //******************************************************************************************************************
    //                                   Entity
    //******************************************************************************************************************
    if (layout.entity.enabled) {
        val entity = generateEntity(layout, graphQLSchema, dslAnnotation, dto.types, dto.interfaces)
        result += entity.files
    }

    result += contextFile.build()
    return result.map { it.toKotlinFile() }
}
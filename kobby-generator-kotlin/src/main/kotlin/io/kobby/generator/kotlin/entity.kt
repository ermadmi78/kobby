package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.TypeDefinitionRegistry

internal data class GenerateEntityResult(
    val projectionTypes: Map<String, TypeName>,
    val files: List<FileSpec>
)

/**
 * Created on 14.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateEntity(
    layout: KotlinGeneratorLayout,
    graphQLSchema: TypeDefinitionRegistry,
    dslAnnotation: ClassName,
    dtoTypes: Map<String, TypeName>,
    interfaces: Map<String, Set<String>>
): GenerateEntityResult {
    val entityLayout = layout.entity

    val projectionTypes = mutableMapOf<String, TypeName>()
    val projectionSpecs = mutableMapOf<String, TypeSpec>()

    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> {
                if (type.name != "Query" && type.name != "Mutation") {
                    val name = type.name.decorate(entityLayout.projectionDecoration)
                    projectionTypes[type.name] = ClassName(entityLayout.packageName, name)
                    projectionSpecs[type.name] = TypeSpec.interfaceBuilder(name).apply {
                        addAnnotation(dslAnnotation)
                        for (field in type.fieldDefinitions) {
                            if (!field.isRequired()) {
                                addFunction(FunSpec.builder(field.projectionName()).apply {
                                    addModifiers(KModifier.ABSTRACT)
                                }.build())
                            }
                        }
                    }.build()
                }
            }
        }
    }

    return GenerateEntityResult(
        projectionTypes,
        projectionSpecs.asSequence().map { (key, value) ->
            FileSpec.builder(entityLayout.packageName, key)
                .addType(value)
                .build()
        }.toList()
    )
}
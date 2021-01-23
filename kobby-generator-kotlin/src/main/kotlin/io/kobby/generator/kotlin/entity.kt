package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import graphql.language.InterfaceTypeDefinition
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
    layout: KotlinLayout,
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
                    val projectionType = type.name.projection(entityLayout)
                    projectionTypes[type.name] = projectionType
                    projectionSpecs[type.name] = TypeSpec.interfaceBuilder(projectionType).apply {
                        addAnnotation(dslAnnotation)
                        for (field in type.fieldDefinitions) {
                            if (!field.isRequired()) {
                                addFunction(FunSpec.builder(field.projectionName(entityLayout)).apply {
                                    addModifiers(KModifier.ABSTRACT)
                                    for (arg in field.inputValueDefinitions) {
                                        addParameter(arg.name, arg.type.resolve(dtoTypes))
                                    }
                                    graphQLSchema.types()[field.type.extractName()]?.takeIf {
                                        it is ObjectTypeDefinition || it is InterfaceTypeDefinition //todo union
                                    }?.also {
                                        addProjectionParameter(entityLayout, it.name)
                                    }
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
package io.kobby.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import graphql.language.*
import graphql.schema.idl.TypeDefinitionRegistry

/**
 * Created on 19.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto(layout: GeneratorLayout, graphQLSchema: TypeDefinitionRegistry): List<FileSpec> {
    val types = mutableMapOf<String, TypeName>().apply {
        putAll(layout.scalars)
    }
    val interfaces = mutableMapOf<String, MutableSet<String>>()
    for (type in graphQLSchema.types()) {
        when (type) {
            is ObjectTypeDefinition -> types[type.name] = ClassName(
                layout.dtoPackage.name,
                type.name.decorate(layout.dtoPrefix, layout.dtoPostfix)
            )
            is InputObjectTypeDefinition -> types[type.name] = ClassName(
                layout.dtoPackage.name,
                type.name
            )
            is InterfaceTypeDefinition -> {
                types[type.name] = ClassName(
                    layout.dtoPackage.name,
                    type.name.decorate(layout.dtoPrefix, layout.dtoPostfix)
                )
                interfaces.computeIfAbsent(type.name) { mutableSetOf() }.also {
                    for (field in type.fieldDefinitions) {
                        it += field.name
                    }
                }
            }
            is EnumTypeDefinition -> types[type.name] = ClassName(
                layout.dtoPackage.name,
                type.name
            )
            is UnionTypeDefinition -> TODO()
        }
    }

    for (type in graphQLSchema.types()) {
        val builder: TypeSpec? = when (type) {
            is ObjectTypeDefinition -> {
                val classBuilder = TypeSpec.classBuilder(type.name.decorate(layout.dtoPrefix, layout.dtoPostfix))
                    .apply {
                        addModifiers(KModifier.DATA)
                        type.implements.asSequence().map { it.resolve(types) }.forEach {
                            addSuperinterface(it)
                        }
                    }
                val constructorBuilder = FunSpec.constructorBuilder()
                for (field in type.fieldDefinitions) {
                    val fieldType = field.type.resolve(types).copy(true)
                    constructorBuilder.addParameter(
                        ParameterSpec.builder(field.name, fieldType)
                            .defaultValue("null")
                            .build()
                    )
                    classBuilder.addProperty(PropertySpec.builder(field.name, fieldType).apply {
                        initializer(field.name)
                        for (ancestorType in type.implements) {
                            if (field.name in interfaces[ancestorType.extractName()]!!) {
                                addModifiers(KModifier.OVERRIDE)
                                break
                            }
                        }
                    }.build())
                }

                classBuilder.primaryConstructor(constructorBuilder.build()).build()
            }
            is InputObjectTypeDefinition -> {
                val classBuilder = TypeSpec.classBuilder(type.name).addModifiers(KModifier.DATA)
                val constructorBuilder = FunSpec.constructorBuilder()
                for (field in type.inputValueDefinitions) {
                    val fieldType = field.type.resolve(types).copy(true)
                    constructorBuilder.addParameter(
                        ParameterSpec.builder(field.name, fieldType)
                            .defaultValue("null")
                            .build()
                    )
                    classBuilder.addProperty(
                        PropertySpec.builder(field.name, fieldType)
                            .initializer(field.name)
                            .build()
                    )
                }

                classBuilder.primaryConstructor(constructorBuilder.build()).build()
            }
            is InterfaceTypeDefinition ->
                TypeSpec.interfaceBuilder(type.name.decorate(layout.dtoPrefix, layout.dtoPostfix)).apply {
                    type.implements.asSequence().map { it.resolve(types) }.forEach {
                        addSuperinterface(it)
                    }
                    for (field in type.fieldDefinitions) {
                        val fieldType = field.type.resolve(types).copy(true)
                        addProperty(PropertySpec.builder(field.name, fieldType).apply {
                            for (ancestorType in type.implements) {
                                if (field.name in interfaces[ancestorType.extractName()]!!) {
                                    addModifiers(KModifier.OVERRIDE)
                                    break
                                }
                            }
                        }.build())
                    }
                }.build()
            is EnumTypeDefinition -> null
            is UnionTypeDefinition -> TODO()
            else -> null
        }
    }

    TODO()
}

internal fun String.decorate(prefix: String?, postfix: String?): String {
    return if (prefix.isNullOrBlank() && postfix.isNullOrBlank()) {
        this
    } else if (prefix.isNullOrBlank()) {
        this + postfix
    } else if (postfix.isNullOrBlank()) {
        prefix + this
    } else {
        prefix + this + postfix
    }
}

internal fun Type<*>.resolve(types: Map<String, TypeName>, nonNull: Boolean = false): TypeName = when (this) {
    is NonNullType -> type.resolve(types, true)
    is ListType -> LIST.parameterizedBy(type.resolve(types)).run {
        if (nonNull) this else copy(true)
    }
    is graphql.language.TypeName -> types[name]?.run {
        if (nonNull) this else copy(true)
    } ?: error("Cannot resolve type by name: $name")
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun Type<*>.extractName(): String = when (this) {
    is NonNullType -> type.extractName()
    is ListType -> type.extractName()
    is graphql.language.TypeName -> name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}
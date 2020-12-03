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
internal fun generateDto(layout: GeneratorLayout, graphQLSchema: TypeDefinitionRegistry): Map<String, TypeSpec> {
    val dtoLayout = layout.dto
    val types = mutableMapOf<String, TypeName>().apply {
        putAll(layout.scalars)
    }
    val interfaces = mutableMapOf<String, MutableSet<String>>()
    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
            )
            is InputObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name
            )
            is InterfaceTypeDefinition -> {
                types[type.name] = ClassName(
                    dtoLayout.packageName,
                    type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
                )
                interfaces.computeIfAbsent(type.name) { mutableSetOf() }.also {
                    for (field in type.fieldDefinitions) {
                        it += field.name
                    }
                }
            }
            is EnumTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name
            )
            is UnionTypeDefinition -> TODO()
        }
    }

    return graphQLSchema.types().values.asSequence().map { type ->
        when (type) {
            is ObjectTypeDefinition -> {
                val className = type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
                val classBuilder = TypeSpec.classBuilder(className).apply {
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

                classBuilder
                    .primaryConstructor(constructorBuilder.jacksonize(dtoLayout).build())
                    .jacksonize(dtoLayout, type.name, className)
                    .build()
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

                classBuilder
                    .primaryConstructor(constructorBuilder.jacksonize(dtoLayout).build())
                    .jacksonize(dtoLayout, type.name, type.name)
                    .build()
            }
            is InterfaceTypeDefinition ->
                TypeSpec.interfaceBuilder(type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)).apply {
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
            is EnumTypeDefinition -> TypeSpec.enumBuilder(type.name).apply {
                for (enumValue in type.enumValueDefinitions) {
                    addEnumConstant(enumValue.name)
                }
            }.build()
            is UnionTypeDefinition -> TODO("Union support is not implemented yet")
            else -> null
        }?.let {
            type.name to it
        }
    }.filterNotNull().toMap()
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
    } ?: error("Scalar type is not configured: $name")
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun Type<*>.extractName(): String = when (this) {
    is NonNullType -> type.extractName()
    is ListType -> type.extractName()
    is graphql.language.TypeName -> name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun FunSpec.Builder.jacksonize(layout: DtoLayout): FunSpec.Builder {
    if (layout.jacksonized && parameters.size == 1) {
        addAnnotation(JacksonAnnotations.JSON_CREATOR)
    }

    return this
}

internal fun TypeSpec.Builder.jacksonize(layout: DtoLayout, typeName: String, className: String): TypeSpec.Builder {
    if (!layout.jacksonized) {
        return this
    }

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_NAME)
            .addMember("value = %S", typeName)
            .build()
    )

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("use = %T.Id.NAME", JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("include = %T.As.PROPERTY", JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("property = %S", "__typename")
            .addMember("defaultImpl = $className::javaClass")
            .build()
    )

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
            .addMember("value = %T.Include.NON_ABSENT", JacksonAnnotations.JSON_INCLUDE)
            .build()
    )

    return this
}

internal object JacksonAnnotations {
    val JSON_CREATOR = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonCreator"
    )

    val JSON_TYPE_NAME = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeName"
    )

    val JSON_TYPE_INFO = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeInfo"
    )

    val JSON_INCLUDE = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonInclude"
    )
}
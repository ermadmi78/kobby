package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import graphql.language.*
import graphql.schema.idl.TypeDefinitionRegistry

internal data class GenerateDtoResult(
    val types: Map<String, TypeName>,
    val interfaces: Map<String, Set<String>>,
    val files: List<FileSpec>
)

/**
 * Created on 19.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto(
    layout: KotlinGeneratorLayout,
    graphQLSchema: TypeDefinitionRegistry,
    dslAnnotation: ClassName
): GenerateDtoResult {
    val dtoLayout = layout.dto
    val builderLayout = layout.dto.builder
    val graphqlLayout = layout.dto.graphql

    val types = mutableMapOf<String, TypeName>().apply {
        layout.scalars.forEach { (scalar, type) ->
            put(scalar, type.toTypeName())
        }
    }
    val interfaces = mutableMapOf<String, MutableSet<String>>()
    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name.decorate(dtoLayout.decoration)
            )
            is InputObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name
            )
            is InterfaceTypeDefinition -> {
                types[type.name] = ClassName(
                    dtoLayout.packageName,
                    type.name.decorate(dtoLayout.decoration)
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
    val files = mutableMapOf<String, FileSpec.Builder>()

    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> {
                val className = type.name.decorate(dtoLayout.decoration)
                val classBuilder = TypeSpec.classBuilder(className).apply {
                    addModifiers(KModifier.DATA)
                    type.implements.asSequence().map { it.resolve(types, true) }.forEach {
                        addSuperinterface(it)
                    }
                }
                val constructorBuilder = FunSpec.constructorBuilder()
                for (field in type.fieldDefinitions) {
                    val fieldType = field.type.resolve(types).nullable()
                    classBuilder.addProperty(constructorBuilder, field.name, fieldType) {
                        for (ancestorType in type.implements) {
                            if (field.name in interfaces[ancestorType.extractName()]!!) {
                                addModifiers(KModifier.OVERRIDE)
                                break
                            }
                        }
                    }
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
                    classBuilder.addProperty(
                        constructorBuilder,
                        field.name,
                        field.type.resolve(types)
                    )
                }

                classBuilder
                    .primaryConstructor(constructorBuilder.jacksonize(dtoLayout).build())
                    .jacksonize(dtoLayout, type.name, type.name)
                    .build()
            }
            is InterfaceTypeDefinition ->
                TypeSpec.interfaceBuilder(type.name.decorate(dtoLayout.decoration)).apply {
                    type.implements.asSequence().map { it.resolve(types, true) }.forEach {
                        addSuperinterface(it)
                    }
                    for (field in type.fieldDefinitions) {
                        val fieldType = field.type.resolve(types).nullable()
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
        }?.also {
            require(files[type.name] == null) {
                "DTO type name conflict - there are several types with name: ${type.name}"
            }
            files[type.name] = FileSpec.builder(dtoLayout.packageName, it.name!!).addType(it)
        }
    }

    if (layout.dto.builder.enabled) {
        for (type in graphQLSchema.types().values) {
            // Create builder functions
            when (type) {
                is ObjectTypeDefinition -> type.fieldDefinitions.joinToString { it.name }
                is InputObjectTypeDefinition -> type.inputValueDefinitions.joinToString {
                    if (it.type is NonNullType) {
                        "${it.name}·?:·error(\"${type.name}:·'${it.name}'·must·not·be·null\")"
                    } else {
                        it.name
                    }
                }
                else -> null
            }?.let { arguments ->
                val dtoType: TypeName = types[type.name]!!
                val dtoName: String = (dtoType as ClassName).simpleName
                val builderType: TypeName = ClassName(
                    dtoLayout.packageName,
                    dtoName.decorate(builderLayout.decoration)
                )
                FunSpec.builder(dtoName)
                    .addParameter("block", LambdaTypeName.get(builderType, emptyList(), UNIT))
                    .returns(dtoType)
                    .addStatement("return %T().apply(block).run·{ %T($arguments) }", builderType, dtoType)
                    .build()
            }?.also {
                files[type.name]!!.addFunction(it)
            }

            // Create builders
            when (type) {
                is ObjectTypeDefinition -> type.fieldDefinitions.map {
                    PropertySpec.builder(it.name, it.type.resolve(types).nullable())
                        .mutable()
                        .initializer("null")
                        .build()
                }
                is InputObjectTypeDefinition -> type.inputValueDefinitions.map {
                    PropertySpec.builder(it.name, it.type.resolve(types).nullable())
                        .mutable()
                        .initializer("null")
                        .build()
                }
                else -> null
            }?.let { properties ->
                val dtoName: String = (types[type.name] as ClassName).simpleName
                TypeSpec.classBuilder(dtoName.decorate(builderLayout.decoration)).apply {
                    addAnnotation(dslAnnotation)
                    properties.forEach {
                        addProperty(it)
                    }
                }.build()
            }?.also {
                files[type.name]!!.addType(it)
            }
        }
    }

    if (graphqlLayout.enabled) {
        // GraphQL Request
        val requestName = "Request".decorate(graphqlLayout.decoration)
        val requestConstructorBuilder = FunSpec.constructorBuilder()
        val requestType = TypeSpec.classBuilder(requestName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                requestConstructorBuilder,
                "query",
                STRING
            )
            .addProperty(
                requestConstructorBuilder,
                "variables",
                MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                requestConstructorBuilder,
                "operationName",
                STRING.nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .primaryConstructor(requestConstructorBuilder.build())
            .build()
        files["graphql.$requestName"] =
            FileSpec.builder(graphqlLayout.packageName, requestName).addType(requestType)

        // GraphQL ErrorType
        val errorTypeName = "ErrorType".decorate(graphqlLayout.decoration)
        val errorTypeType = TypeSpec.enumBuilder(errorTypeName)
            .addEnumConstant("InvalidSyntax")
            .addEnumConstant("ValidationError")
            .addEnumConstant("DataFetchingException")
            .addEnumConstant("OperationNotSupported")
            .addEnumConstant("ExecutionAborted")
            .build()
        files["graphql.$errorTypeName"] =
            FileSpec.builder(graphqlLayout.packageName, errorTypeName).addType(errorTypeType)

        // GraphQLErrorSourceLocation
        val errorSourceLocationName = "ErrorSourceLocation".decorate(graphqlLayout.decoration)
        val errorSourceLocationConstructorBuilder = FunSpec.constructorBuilder()
        val errorSourceLocationType = TypeSpec.classBuilder(errorSourceLocationName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "line",
                INT
            )
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "column",
                INT
            )
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "sourceName",
                STRING.nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .primaryConstructor(errorSourceLocationConstructorBuilder.build())
            .build()
        files["graphql.$errorSourceLocationName"] =
            FileSpec.builder(graphqlLayout.packageName, errorSourceLocationName).addType(errorSourceLocationType)

        // GraphQLError
        val errorName = "Error".decorate(graphqlLayout.decoration)
        val errorConstructorBuilder = FunSpec.constructorBuilder()
        val errorType = TypeSpec.classBuilder(errorName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                errorConstructorBuilder,
                "message",
                STRING
            )
            .addProperty(
                errorConstructorBuilder,
                "locations",
                LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorSourceLocationName)).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                errorConstructorBuilder,
                "errorType",
                ClassName(graphqlLayout.packageName, errorTypeName).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .addProperty(
                errorConstructorBuilder,
                "path",
                LIST.parameterizedBy(ANY).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                errorConstructorBuilder,
                "extensions",
                MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .primaryConstructor(errorConstructorBuilder.build())
            .build()
        files["graphql.$errorName"] =
            FileSpec.builder(graphqlLayout.packageName, errorName).addType(errorType)

        // GraphQLException
        val exceptionName = "Exception".decorate(graphqlLayout.decoration)
        val exceptionConstructorBuilder = FunSpec.constructorBuilder()
            .addParameter("message", STRING)
        val exceptionType = TypeSpec.classBuilder(exceptionName)
            .addProperty(
                exceptionConstructorBuilder,
                "request",
                ClassName(graphqlLayout.packageName, requestName)
            )
            .addProperty(
                exceptionConstructorBuilder,
                "errors",
                LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
            )
            .primaryConstructor(exceptionConstructorBuilder.build())
            .superclass(ClassName("kotlin", "RuntimeException"))
            .addSuperclassConstructorParameter(
                "message + (errors?.joinToString(\",\\n  \", \"\\n  \", \"\\n\")·{ it.toString() } ?: \"\")"
            )
            .build()
        files["graphql.$exceptionName"] =
            FileSpec.builder(graphqlLayout.packageName, exceptionName).addType(exceptionType)

        // GraphQLQueryResult
        types["Query"]?.also {
            val queryName = "QueryResult".decorate(graphqlLayout.decoration)
            val queryConstructorBuilder = FunSpec.constructorBuilder()
            val queryType = TypeSpec.classBuilder(queryName)
                .addModifiers(KModifier.DATA)
                .addProperty(
                    queryConstructorBuilder,
                    "data",
                    it.nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
                .addProperty(
                    queryConstructorBuilder,
                    "errors",
                    LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
                .primaryConstructor(queryConstructorBuilder.build())
                .build()
            files["graphql.$queryName"] =
                FileSpec.builder(graphqlLayout.packageName, queryName).addType(queryType)
        }

        // GraphQLMutationResult
        types["Mutation"]?.also {
            val mutationName = "MutationResult".decorate(graphqlLayout.decoration)
            val mutationConstructorBuilder = FunSpec.constructorBuilder()
            val mutationType = TypeSpec.classBuilder(mutationName)
                .addModifiers(KModifier.DATA)
                .addProperty(
                    mutationConstructorBuilder,
                    "data",
                    it.nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
                .addProperty(
                    mutationConstructorBuilder,
                    "errors",
                    LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
                .primaryConstructor(mutationConstructorBuilder.build())
                .build()
            files["graphql.$mutationName"] =
                FileSpec.builder(graphqlLayout.packageName, mutationName).addType(mutationType)
        }
    }

    return GenerateDtoResult(types, interfaces, files.values.asSequence().map { it.build() }.toList())
}

private fun FunSpec.Builder.jacksonize(layout: KotlinDtoLayout): FunSpec.Builder {
    if (layout.jackson.enabled && parameters.size == 1) {
        addAnnotation(JacksonAnnotations.JSON_CREATOR)
    }

    return this
}

private fun TypeSpec.Builder.jacksonize(
    layout: KotlinDtoLayout,
    typeName: String,
    className: String
): TypeSpec.Builder {
    if (!layout.jackson.enabled) {
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
            .addMember("defaultImpl = $className::class")
            .build()
    )

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
            .addMember("value = %T.Include.NON_ABSENT", JacksonAnnotations.JSON_INCLUDE)
            .build()
    )

    return this
}

private fun TypeSpec.Builder.addProperty(
    constructorBuilder: FunSpec.Builder,
    name: String,
    type: TypeName,
    block: PropertySpec.Builder.() -> Unit = {}
): TypeSpec.Builder = addProperty(PropertySpec.builder(name, type).also {
    constructorBuilder.addParameter(
        ParameterSpec.builder(name, type).apply {
            if (type.isNullable) {
                defaultValue("null")
            }
        }.build()
    )
    it.initializer(name)
}.apply(block).build())

private fun PropertySpec.Builder.jacksonInclude(
    layout: KotlinDtoLayout,
    include: JacksonInclude
): PropertySpec.Builder {
    if (layout.jackson.enabled) {
        addAnnotation(
            AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
                .addMember("value = %T.Include.${include.name}", JacksonAnnotations.JSON_INCLUDE)
                .build()
        )
    }
    return this
}

private object JacksonAnnotations {
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

private enum class JacksonInclude {
    ALWAYS,
    NON_NULL,
    NON_ABSENT,
    NON_EMPTY,
    NON_DEFAULT,
    CUSTOM,
    USE_DEFAULTS
}
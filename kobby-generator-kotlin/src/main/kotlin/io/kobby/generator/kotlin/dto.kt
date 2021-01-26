package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kobby.model.KobbySchema

/**
 * Created on 23.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objectsWithQueryAndMutation { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            // Build object DTO class
            buildClass(node.dtoName) {
                jacksonize(node)
                addModifiers(KModifier.DATA)
                node.comments {
                    addKdoc(it)
                }
                node.implements {
                    addSuperinterface(it.dtoClass)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc(it)
                            }
                            if (field.isOverride()) {
                                addModifiers(KModifier.OVERRIDE)
                            }
                        }
                    }
                    customizeConstructor {
                        jacksonize()
                    }
                }
            }

            // Build object DTO builder
            if (dto.builder.enabled) {
                // Builder function
                buildFunction(node.dtoName) {
                    val arguments = node.fields.values.joinToString { it.name }
                    addParameter("block", LambdaTypeName.get(node.builderClass, emptyList(), UNIT))
                    returns(node.dtoClass)
                    addStatement("return %T().apply(block).run·{ %T($arguments) }", node.builderClass, node.dtoClass)
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc(it)
                    }
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc(it)
                            }
                            mutable()
                            initializer("null")
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            buildInterface(node.dtoName) {
                node.comments {
                    addKdoc(it)
                }
                node.implements {
                    addSuperinterface(it.dtoClass)
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.dtoType.nullable()) {
                        field.comments {
                            addKdoc(it)
                        }
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            buildInterface(node.dtoName) {
                node.comments {
                    addKdoc(it)
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                Enums
    //******************************************************************************************************************
    schema.enums { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            buildEnum(node.dtoName) {
                node.comments {
                    addKdoc(it)
                }
                node.enumValues { enumValue ->
                    buildEnumConstant(enumValue.name) {
                        enumValue.comments {
                            addKdoc(it)
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                Inputs
    //******************************************************************************************************************
    schema.inputs { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            // Build input DTO class
            buildClass(node.dtoName) {
                jacksonize(node)
                addModifiers(KModifier.DATA)
                node.comments {
                    addKdoc(it)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType) {
                            field.comments {
                                addKdoc(it)
                            }
                        }
                    }
                    customizeConstructor {
                        jacksonize()
                    }
                }
            }

            // Build input DTO builder
            if (dto.builder.enabled) {
                // Builder function
                buildFunction(node.dtoName) {
                    val arguments = node.fields.values.joinToString {
                        if (it.type.nullable) {
                            it.name
                        } else {
                            "${it.name}·?:·error(\"${node.dtoName}:·'${it.name}'·must·not·be·null\")"
                        }
                    }
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)
                    addStatement("return %T().apply(block).run·{ %T($arguments) }", node.builderClass, node.dtoClass)
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc(it)
                    }
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc(it)
                            }
                            mutable()
                            initializer("null")
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                GraphQL DTO
    //******************************************************************************************************************
    if (dto.graphql.enabled) {
        // GraphQL Request
        files += buildFile(dto.graphql.packageName, dto.graphql.requestName) {
            buildClass(dto.graphql.requestName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("query", STRING)
                    buildProperty("variables", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("operationName", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                }
            }
        }

        // GraphQL ErrorType
        files += buildFile(dto.graphql.packageName, dto.graphql.errorTypeName) {
            buildEnum(dto.graphql.errorTypeName) {
                addEnumConstant("InvalidSyntax")
                addEnumConstant("ValidationError")
                addEnumConstant("DataFetchingException")
                addEnumConstant("OperationNotSupported")
                addEnumConstant("ExecutionAborted")
            }
        }

        // GraphQL ErrorSourceLocation
        files += buildFile(dto.graphql.packageName, dto.graphql.errorSourceLocationName) {
            buildClass(dto.graphql.errorSourceLocationName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("line", INT)
                    buildProperty("column", INT)
                    buildProperty("sourceName", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                }
            }
        }

        // GraphQL Error
        files += buildFile(dto.graphql.packageName, dto.graphql.errorName) {
            buildClass(dto.graphql.errorName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("message", STRING)
                    buildProperty(
                        "locations", LIST.parameterizedBy(dto.graphql.errorSourceLocationClass).nullable()
                    ) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("errorType", dto.graphql.errorTypeClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty("path", LIST.parameterizedBy(ANY).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("extensions", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        val argErrors = "errors" to LIST.parameterizedBy(dto.graphql.errorClass).nullable()

        // GraphQL Exception
        files += buildFile(dto.graphql.packageName, dto.graphql.exceptionName) {
            buildClass(dto.graphql.exceptionName) {
                buildPrimaryConstructorProperties {
                    buildParameter("message", STRING)
                    buildProperty("request", dto.graphql.requestClass)
                    buildProperty(argErrors)
                }
                superclass(ClassName("kotlin", "RuntimeException"))
                addSuperclassConstructorParameter(
                    "message + (errors?.joinToString(\",\\n  \", \"\\n  \", \"\\n\")·{ it.toString() } ?: \"\")"
                )
            }
        }

        // GraphQL QueryResult
        files += buildFile(dto.graphql.packageName, dto.graphql.queryResultName) {
            buildClass(dto.graphql.queryResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("data", schema.query.dtoClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        // GraphQL MutationResult
        files += buildFile(dto.graphql.packageName, dto.graphql.mutationResultName) {
            buildClass(dto.graphql.mutationResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("data", schema.mutation.dtoClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }
    }
    //******************************************************************************************************************

    files
}
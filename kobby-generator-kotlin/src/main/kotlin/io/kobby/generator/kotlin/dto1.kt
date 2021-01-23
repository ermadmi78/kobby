package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.UNIT
import io.kobby.model.KobbySchema

/**
 * Created on 23.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto1(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
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
                buildPrimaryConstructor {
                    node.fields { field ->
                        buildParameter(field.name, field.type.dtoType.nullable()) {
                            defaultValue("null")
                        }
                    }
                    jacksonize()
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.dtoType.nullable()) {
                        field.comments {
                            addKdoc(it)
                        }
                        initializer(field.name)
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
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
                    addAnnotation(dslAnnotation)
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
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
                buildPrimaryConstructor {
                    node.fields { field ->
                        buildParameter(field.name, field.type.dtoType) {
                            if (field.type.nullable) {
                                defaultValue("null")
                            }
                        }
                    }
                    jacksonize()
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.dtoType) {
                        field.comments {
                            addKdoc(it)
                        }
                        initializer(field.name)
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
                    addParameter("block", LambdaTypeName.get(node.builderClass, emptyList(), UNIT))
                    returns(node.dtoClass)
                    addStatement("return %T().apply(block).run·{ %T($arguments) }", node.builderClass, node.dtoClass)
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(dslAnnotation)
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            mutable()
                            initializer("null")
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************

    files
}
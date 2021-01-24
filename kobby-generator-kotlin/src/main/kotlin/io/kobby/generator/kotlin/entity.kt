package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.kobby.model.KobbyNodeKind.*
import io.kobby.model.KobbySchema

/**
 * Created on 24.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateEntity(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objects { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.implements {
                    addSuperinterface(it.projectionClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                    buildFunction(field.projectionName) {
                        addModifiers(KModifier.ABSTRACT)
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                        field.comments {
                            addKdoc(it)
                        }
                        field.arguments { arg ->
                            buildParameter(arg.name, arg.type.dtoType) {
                                if (arg.type.nullable) {
                                    defaultValue("null")
                                }
                                arg.comments {
                                    addKdoc(it)
                                }
                            }
                        }
                        if (field.type.node.kind in setOf(OBJECT, INTERFACE, UNION)) {
                            buildParameter(entity.projection.argument, field.type.node.projectionLambda) {
                                defaultValue("{}")
                            }
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
        files += buildFile(entity.packageName, node.entityName) {
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.implements {
                    addSuperinterface(it.projectionClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                    buildFunction(field.projectionName) {
                        addModifiers(KModifier.ABSTRACT)
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                        field.comments {
                            addKdoc(it)
                        }
                        field.arguments { arg ->
                            buildParameter(arg.name, arg.type.dtoType) {
                                if (arg.type.nullable) {
                                    defaultValue("null")
                                }
                                arg.comments {
                                    addKdoc(it)
                                }
                            }
                        }
                        if (field.type.node.kind in setOf(OBJECT, INTERFACE, UNION)) {
                            buildParameter(entity.projection.argument, field.type.node.projectionLambda) {
                                defaultValue("{}")
                            }
                        }
                    }
                }

                //todo add interface ON cases
            }
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
                //todo add interface ON cases
            }
        }
    }

    files
}
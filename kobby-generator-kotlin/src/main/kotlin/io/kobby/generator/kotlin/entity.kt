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
            // Build object entity
            buildInterface(node.entityName) {
                node.implements {
                    addSuperinterface(it.entityClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.entityType) {
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                        field.comments {
                            addKdoc(it)
                        }
                    }
                }
            }

            // Build object projection
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.implements {
                    addSuperinterface(it.projectionClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                    buildFunction(field.projectionFieldName) {
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
                            buildParameter(entity.projection.projectionArgument, field.type.node.projectionLambda) {
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
            // Build interface entity
            buildInterface(node.entityName) {
                node.implements {
                    addSuperinterface(it.entityClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.entityType) {
                        if (field.isOverride()) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                        field.comments {
                            addKdoc(it)
                        }
                    }
                }
            }

            // Build interface projection
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.implements {
                    addSuperinterface(it.projectionClass)
                }
                node.comments {
                    addKdoc(it)
                }
                node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                    buildFunction(field.projectionFieldName) {
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
                            buildParameter(entity.projection.projectionArgument, field.type.node.projectionLambda) {
                                defaultValue("{}")
                            }
                        }
                    }
                }
            }

            // Build interface qualification
            buildInterface(node.qualificationName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
                node.subObjects { subObject ->
                    buildFunction(subObject.projectionOnName) {
                        addModifiers(KModifier.ABSTRACT)
                        subObject.comments {
                            addKdoc(it)
                        }
                        buildParameter(entity.projection.projectionArgument, subObject.projectionLambda) {
                            defaultValue("{}")
                        }
                    }
                }
            }

            // Build interface qualified projection
            buildInterface(node.qualifiedProjectionName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
                addSuperinterface(node.projectionClass)
                addSuperinterface(node.qualificationClass)
            }
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(entity.packageName, node.entityName) {
            // Build union entity
            buildInterface(node.entityName) {
                node.comments {
                    addKdoc(it)
                }
            }

            // Build union projection
            buildInterface(node.projectionName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
            }

            // Build interface qualification
            buildInterface(node.qualificationName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
                node.subObjects { subObject ->
                    buildFunction(subObject.projectionOnName) {
                        addModifiers(KModifier.ABSTRACT)
                        subObject.comments {
                            addKdoc(it)
                        }
                        buildParameter(entity.projection.projectionArgument, subObject.projectionLambda) {
                            defaultValue("{}")
                        }
                    }
                }
            }

            // Build interface qualified projection
            buildInterface(node.qualifiedProjectionName) {
                addAnnotation(context.dslClass)
                node.comments {
                    addKdoc(it)
                }
                addSuperinterface(node.projectionClass)
                addSuperinterface(node.qualificationClass)
            }
        }
    }

    files
}
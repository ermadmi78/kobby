package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbyNodeKind.OBJECT
import io.github.ermadmi78.kobby.model.KobbySchema

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
            buildEntity(node, layout)
            buildProjection(node, layout)
            buildSelection(node, layout)
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildEntity(node, layout)
            buildProjection(node, layout)
            buildQualification(node, layout)
            buildQualifiedProjection(node, layout)
            buildSelection(node, layout)
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildEntity(node, layout)
            buildProjection(node, layout)
            buildQualification(node, layout)
            buildQualifiedProjection(node, layout)
            buildSelection(node, layout)
        }
    }

    files
}

private fun FileSpecBuilder.buildEntity(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildInterface(node.entityName) {
        if (entity.contextInheritanceEnabled) {
            addSuperinterface(context.contextClass)
        }
        node.implements {
            addSuperinterface(it.entityClass)
        }
        node.comments {
            addKdoc(it)
        }

        if (entity.contextInheritanceEnabled) {
            // context query
            buildFunction(context.query) {
                addModifiers(OVERRIDE, SUSPEND, ABSTRACT)
                buildParameter(entity.projection.projectionArgument, node.schema.query.projectionLambda)
                returns(node.schema.query.entityClass)
            }

            // context mutation
            buildFunction(context.mutation) {
                addModifiers(OVERRIDE, SUSPEND, ABSTRACT)
                buildParameter(entity.projection.projectionArgument, node.schema.mutation.projectionLambda)
                returns(node.schema.mutation.entityClass)
            }

            // context subscription
            buildFunction(context.subscription) {
                addModifiers(OVERRIDE, ABSTRACT)
                buildParameter(entity.projection.projectionArgument, node.schema.subscription.projectionLambda)
                returns(context.subscriberClass.parameterizedBy(node.schema.subscription.entityClass))
            }
        }

        if (entity.contextFunEnabled) {
            // context access function
            buildFunction(entity.contextFunName) {
                addModifiers(ABSTRACT)
                if (node.implements.isNotEmpty()) {
                    addModifiers(OVERRIDE)
                }
                returns(context.contextClass)
            }
        }

        // withCurrentProjection
        if (node.kind == OBJECT) {
            buildFunction(entity.withCurrentProjectionFun) {
                addModifiers(ABSTRACT)
                receiver(node.projectionClass)
            }
        }

        node.fields { field ->
            buildProperty(field.name, field.entityType) {
                if (field.isOverride) {
                    addModifiers(OVERRIDE)
                }
                field.comments {
                    addKdoc(it)
                }
            }
        }
    }
}

private fun FileSpecBuilder.buildProjection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildInterface(node.projectionName) {
        addAnnotation(context.dslClass)
        node.implements {
            addSuperinterface(it.projectionClass)
        }
        node.comments {
            addKdoc(it)
        }
        node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
            buildFunction(field.projectionFieldName) {
                addModifiers(ABSTRACT)
                if (field.isOverride) {
                    addModifiers(OVERRIDE)
                }
                field.comments {
                    addKdoc(it)
                }

                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.isInitialized }
                    .forEach { arg ->
                        buildParameter(arg.name, arg.entityType) {
                            if (!field.isOverride && arg.isInitialized) {
                                defaultValue("null")
                            }
                            arg.comments {
                                addKdoc(it)
                            }
                            arg.defaultValue?.also { literal ->
                                if (arg.comments.isNotEmpty()) {
                                    addKdoc(" ");
                                }
                                addKdoc("Default: $literal")
                            }
                        }
                    }
                field.lambda?.also {
                    buildParameter(it) {
                        if (!field.isOverride && field.type.node.hasDefaults) {
                            defaultValue("{}")
                        }
                    }
                }
            }
        }

        // minimize function
        buildFunction(entity.projection.minimizeFun) {
            if (node.implements.isNotEmpty()) {
                addModifiers(OVERRIDE)
            }
            node.fields.values.asSequence().filter { !it.isRequired && it.isDefault }.forEach { field ->
                addStatement("${field.projectionFieldName.escape()}()")
            }
        }
    }
}

private fun FileSpecBuilder.buildSelection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    node.fields.values.asSequence().filter { !it.isOverride && it.isSelection }.forEach { field ->
        buildInterface(field.selectionName) {
            addAnnotation(context.dslClass)
            field.comments {
                addKdoc(it)
            }
            field.arguments.values.asSequence().filter { it.isInitialized }.forEach { arg ->
                buildProperty(arg.name, arg.entityType) {
                    mutable()
                    arg.comments {
                        addKdoc(it)
                    }
                    arg.defaultValue?.also { literal ->
                        if (arg.comments.isNotEmpty()) {
                            addKdoc("  \n> ")
                        }
                        addKdoc("Default: $literal")
                    }
                }
            }
        }

        if (field.type.hasProjection) {
            buildInterface(field.queryName) {
                addAnnotation(context.dslClass)
                field.comments {
                    addKdoc(it)
                }
                addSuperinterface(field.selectionClass)
                addSuperinterface(field.type.node.qualifiedProjectionClass)
            }
        }
    }
}

private fun FileSpecBuilder.buildQualification(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildInterface(node.qualificationName) {
        addAnnotation(context.dslClass)
        node.comments {
            addKdoc(it)
        }
        node.subObjects { subObject ->
            buildFunction(subObject.projectionOnName) {
                addModifiers(ABSTRACT)
                subObject.comments {
                    addKdoc(it)
                }
                buildParameter(entity.projection.projectionArgument, subObject.projectionLambda) {
                    if (subObject.hasDefaults) {
                        defaultValue("{}")
                    }
                }
            }
        }
    }
}

private fun FileSpecBuilder.buildQualifiedProjection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildInterface(node.qualifiedProjectionName) {
        addAnnotation(context.dslClass)
        node.comments {
            addKdoc(it)
        }
        addSuperinterface(node.projectionClass)
        addSuperinterface(node.qualificationClass)
    }
}
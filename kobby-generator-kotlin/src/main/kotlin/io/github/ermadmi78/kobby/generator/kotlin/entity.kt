package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbyNodeKind.INPUT
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
        addSuperinterface(context.contextClass)
        node.implements {
            addSuperinterface(it.entityClass)
        }
        node.comments {
            addKdoc(it)
        }

        // context query
        buildFunction(context.query) {
            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND, KModifier.ABSTRACT)
            buildParameter(entity.projection.projectionArgument, node.schema.query.projectionLambda)
            returns(node.schema.query.entityClass)
        }

        // context mutation
        buildFunction(context.mutation) {
            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND, KModifier.ABSTRACT)
            buildParameter(entity.projection.projectionArgument, node.schema.mutation.projectionLambda)
            returns(node.schema.mutation.entityClass)
        }

        // withCurrentProjection
        if (node.kind == OBJECT) {
            buildFunction(entity.withCurrentProjectionFun) {
                addModifiers(KModifier.ABSTRACT)
                receiver(node.projectionClass)
            }
        }

        node.fields { field ->
            buildProperty(field.name, field.type.entityType) {
                if (field.isOverride) {
                    addModifiers(KModifier.OVERRIDE)
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
                addModifiers(KModifier.ABSTRACT)
                if (field.isOverride) {
                    addModifiers(KModifier.OVERRIDE)
                }
                field.comments {
                    addKdoc(it)
                }

                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.type.nullable }
                    .forEach { arg ->
                        buildParameter(arg.name, arg.type.entityType) {
                            if (!field.isOverride && arg.type.nullable) {
                                defaultValue("null")
                            }
                            arg.comments {
                                addKdoc(it)
                            }
                        }
                    }
                field.lambda?.also {
                    buildParameter(it) {
                        if (!field.isOverride) {
                            defaultValue("{}")
                        }
                    }
                }
            }
        }

        // minimize function
        buildFunction(entity.projection.minimizeFun) {
            if (node.implements.isNotEmpty()) {
                addModifiers(KModifier.OVERRIDE)
            }
            node.fields.values.asSequence().filter { !it.isRequired && it.isDefault }.forEach { field ->
                addStatement("${field.projectionFieldName}()")
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
            field.arguments.values.asSequence().filter { it.type.nullable }.forEach { arg ->
                buildProperty(arg.name, arg.type.entityType) {
                    mutable()
                    arg.comments {
                        addKdoc(it)
                    }
                }
                if (dto.builder.enabled && arg.type.node.kind == INPUT) {
                    buildFunction(arg.name) {
                        arg.comments {
                            addKdoc(it)
                        }
                        addParameter("block", arg.type.node.builderLambda)
                        addStatement("${arg.name} = %T(block)", arg.type.node.dtoClass)
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
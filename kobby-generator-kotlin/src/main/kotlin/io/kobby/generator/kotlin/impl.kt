package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.kobby.model.KobbyNode
import io.kobby.model.KobbySchema

/**
 * Created on 27.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateImpl(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    val buildProjection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        buildClass(node.implProjectionName) {
            addModifiers(KModifier.OPEN)
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }
            addSuperinterface(node.qualifiedProjectionClass)

            node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                buildProperty(field.innerName, field.innerType) {
                    mutable()
                    initializer(field.innerInitializer)
                }
                val isSelection = field.isSelection()
                field.arguments.values.asSequence()
                    .filter { !isSelection || !it.type.nullable }
                    .forEach { arg ->
                        buildProperty(arg.innerName, arg.type.entityType.nullable()) {
                            mutable()
                            initializer("null")
                        }
                    }

                buildFunction(field.projectionFieldName) {
                    addModifiers(KModifier.OVERRIDE)
                    field.arguments.values.asSequence()
                        .filter { !isSelection || !it.type.nullable }
                        .forEach { arg ->
                            buildParameter(arg.name, arg.type.entityType)
                        }

                    field.lambda?.also {
                        buildParameter(it)
                        addStatement("${field.innerName} = %T().apply(${it.first})", field.innerClass)
                    } ?: addStatement("${field.innerName} = ${!field.isDefault()}")

                    field.arguments.values.asSequence()
                        .filter { !isSelection || !it.type.nullable }
                        .forEach { arg ->
                            addStatement("${arg.innerName} = ${arg.name}")
                        }
                }
            }

            node.subObjects { subObject ->
                buildProperty(subObject.innerProjectionOnName, subObject.implProjectionClass) {
                    mutable()
                    initializer("%T()", subObject.implProjectionClass)
                }
                buildFunction(subObject.projectionOnName) {
                    addModifiers(KModifier.OVERRIDE)
                    buildParameter(entity.projection.projectionArgument, subObject.projectionLambda)
                    addStatement(
                        "${subObject.innerProjectionOnName} = %T().apply(${entity.projection.projectionArgument})",
                        subObject.implProjectionClass
                    )
                }
            }

            buildFunction(impl.repeatProjectionFunName) {
                suppressUnusedParameter()
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                val repeat = entity.projection.projectionArgument
                buildParameter(repeat, node.qualifiedProjectionClass)
                node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                    if (field.innerIsBoolean) {
                        val negation = if (field.isDefault()) "!" else ""
                        ifFlowStatement("$negation${field.innerName}") {
                            "$repeat.${field.projectionFieldName}()"
                        }
                    } else ifFlow("${field.innerName} != null") {
                        val isSelection = field.isSelection()
                        val args = field.arguments.values.asSequence()
                            .filter { !isSelection || !it.type.nullable }
                            .joinToString { it.innerName + if (it.type.nullable) "" else "!!" }
                            .let { if (it.isEmpty()) it else "($it)" }
                        controlFlow("$repeat.${field.projectionFieldName}$args") {
                            if (field.type.hasProjection) statement {
                                "this@${node.implProjectionName}" +
                                        ".${field.innerName}!!.${impl.repeatProjectionFunName}(this)"
                            }
                            if (isSelection) statement {
                                "this@${node.implProjectionName}" +
                                        ".${field.innerName}!!.${impl.repeatSelectionFunName}(this)"
                            }
                        }
                    }

                }

                node.subObjects { subObject ->
                    controlFlow("$repeat.${subObject.projectionOnName}") {
                        addStatement("this@${node.implProjectionName}.${subObject.innerProjectionOnName}.${impl.repeatProjectionFunName}(this)")
                    }
                }
            }

            buildFunction(impl.buildFunName) {
                suppressUnusedParameter()
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                buildParameter(buildFunArgSb)
                buildParameter(buildFunArgArguments)

                controlFlow("${buildFunArgSb.first}.apply") {
                    spaceAppend('{')
                    node.fields { field ->
                        when {
                            field.isRequired() -> spaceAppend(field.name)
                            field.innerIsBoolean -> ifFlow(field.innerName) {
                                spaceAppend(field.name)
                            }
                            else -> ifFlow("${field.innerName} != null") {
                                spaceAppend("${field.name}(")

                                val isSelection = field.isSelection()
                                field.arguments { arg ->
                                    if (arg.type.nullable) {
                                        val argName = if (isSelection)
                                            "${field.innerName}!!.${arg.name}" else arg.innerName
                                        ifFlow("$argName != null") {
                                            //
                                        }
                                    }
                                }
                                append(')')
                            }
                        }
                    }
                    spaceAppend('}')
                }
            }
        }
    }

    val buildSelection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        node.fields.values.asSequence().filter { it.isSelection() }.forEach { field ->
            val isQuery = field.type.hasProjection
            buildClass(if (isQuery) field.implQueryName else field.implSelectionName) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                if (isQuery) {
                    superclass(field.type.node.implProjectionClass)
                }
                addSuperinterface(if (isQuery) field.queryClass else field.selectionClass)
                field.arguments.values.asSequence().filter { it.type.nullable }.forEach { arg ->
                    buildProperty(arg.name, arg.type.entityType) {
                        mutable()
                        addModifiers(KModifier.OVERRIDE)
                        initializer("null")
                    }
                }

                buildFunction(impl.repeatSelectionFunName) {
                    suppressUnusedParameter()
                    if (impl.internal) {
                        addModifiers(KModifier.INTERNAL)
                    }
                    val repeat = entity.selection.selectionArgument
                    buildParameter(repeat, field.selectionClass)
                    field.arguments.values.asSequence().filter { it.type.nullable }.forEach { arg ->
                        addStatement("$repeat.${arg.name} = ${arg.name}")
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objects { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node)
            buildSelection(node)
        }
    }

    files
}

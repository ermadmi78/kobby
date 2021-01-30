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

    val buildSelection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        node.fields.values.asSequence().filter { it.isSelection }.forEach { field ->
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
                    suppressUnused()
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
            buildProjection(node, layout)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node, layout)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node, layout)
            buildSelection(node)
        }
    }

    files
}

private fun FileSpecBuilder.buildProjection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implProjectionName) {
        addModifiers(KModifier.OPEN)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }
        addSuperinterface(node.qualifiedProjectionClass)

        node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
            buildProperty(field.innerName, field.innerType) {
                mutable()
                initializer(field.innerInitializer)
            }
            field.arguments.values.asSequence()
                .filter { !field.isSelection || !it.type.nullable }
                .forEach { arg ->
                    buildProperty(arg.innerName, arg.type.entityType.nullable()) {
                        mutable()
                        initializer("null")
                    }
                }

            buildFunction(field.projectionFieldName) {
                addModifiers(KModifier.OVERRIDE)
                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.type.nullable }
                    .forEach { arg ->
                        buildParameter(arg.name, arg.type.entityType)
                    }

                field.lambda?.also {
                    buildParameter(it)
                    addStatement("${field.innerName} = %T().apply(${it.first})", field.innerClass)
                } ?: addStatement("${field.innerName} = ${!field.isDefault}")

                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.type.nullable }
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
            suppressUnused()
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }
            val repeat = entity.projection.projectionArgument
            buildParameter(repeat, node.qualifiedProjectionClass)
            node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
                if (field.innerIsBoolean) {
                    val negation = if (field.isDefault) "!" else ""
                    ifFlowStatement("$negation${field.innerName}") {
                        "$repeat.${field.projectionFieldName}()"
                    }
                } else ifFlow("${field.innerName} != null") {
                    val args = field.arguments.values.asSequence()
                        .filter { !field.isSelection || !it.type.nullable }
                        .joinToString { it.innerName + if (it.type.nullable) "" else "!!" }
                        .let { if (it.isEmpty()) it else "($it)" }
                    controlFlow("$repeat.${field.projectionFieldName}$args") {
                        if (field.type.hasProjection) statement {
                            "this@${node.implProjectionName}" +
                                    ".${field.innerName}!!.${impl.repeatProjectionFunName}(this)"
                        }
                        if (field.isSelection) statement {
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
            suppressUnused()
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }
            buildParameter(buildFunArgHeader)
            buildParameter(buildFunArgBody)
            buildParameter(buildFunArgArguments)

            controlFlow("${buildFunArgBody.first}.apply") {
                spaceAppend('{')
                addStatement("")
                node.fields { field ->
                    val fieldCondition = when {
                        field.isRequired -> "true"
                        field.innerIsBoolean -> field.innerName
                        else -> "${field.innerName} != null"
                    }
                    addComment("Field: ${field.name}")
                    ifFlow(fieldCondition) {
                        spaceAppend(field.name)
                        if (field.arguments.isNotEmpty()) {
                            addStatement("var counter = 0")
                            append('(')
                            addStatement("")
                            field.arguments { arg ->
                                val argName = if (arg.isSelection) "${field.innerName}!!.${arg.name}" else arg.innerName
                                addComment("Argument: ${field.name}.${arg.name}")
                                ifFlow(if (arg.type.nullable) "$argName != null" else "true") {
                                    ifFlow("counter++ > 0") {
                                        append(", ")
                                    }
                                    val mapName = buildFunArgArguments.first
                                    addStatement("val arg = %S + $mapName.size", argPrefix)
                                    addStatement("$mapName[arg] = $argName!!")
                                    addStatement("append(%S).append(arg)", "${arg.name}: \$")

                                    addStatement("")
                                    ifFlow("${buildFunArgHeader.first}.isNotEmpty()") {
                                        addStatement("${buildFunArgHeader.first}.append(%S)", ", ")
                                    }
                                    addStatement(
                                        "${buildFunArgHeader.first}.append(%P)",
                                        "\$arg: ${arg.type.sourceName}"
                                    )
                                }
                                addStatement("")
                            }
                            append(')')
                        }
                    }
                    addStatement("")
                }
                spaceAppend('}')
            }
        }
    }
}

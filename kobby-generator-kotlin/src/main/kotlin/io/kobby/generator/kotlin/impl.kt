package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.kobby.model.KobbyNode
import io.kobby.model.KobbyNodeKind.INTERFACE
import io.kobby.model.KobbyNodeKind.UNION
import io.kobby.model.KobbySchema
import io.kobby.model.KobbyType

/**
 * Created on 27.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateImpl(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objects { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildEntity(node, layout)
            buildProjection(node, layout)
            buildSelection(node, layout)
            buildObjectEntityBuilder(node, layout)
            buildResolvers(node, layout)
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node, layout)
            buildSelection(node, layout)
            buildInterfaceOrUnionEntityBuilder(node, layout)
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(impl.packageName, node.implName) {
            buildProjection(node, layout)
            buildSelection(node, layout)
            buildInterfaceOrUnionEntityBuilder(node, layout)
        }
    }

    //******************************************************************************************************************
    //                                                Query
    //******************************************************************************************************************
    files += buildFile(impl.packageName, schema.query.implName) {
        buildQueryOrMutation(schema.query, layout)
        buildSelection(schema.query, layout)
        buildResolvers(schema.query, layout)
    }

    //******************************************************************************************************************
    //                                                Mutation
    //******************************************************************************************************************
    files += buildFile(impl.packageName, schema.mutation.implName) {
        buildQueryOrMutation(schema.mutation, layout)
        buildSelection(schema.mutation, layout)
        buildResolvers(schema.mutation, layout)
    }

    files
}

private fun FileSpecBuilder.buildObjectEntityBuilder(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildFunction(node.entityBuilderName) {
        receiver(node.dtoClass)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }

        buildParameter(node.entityBuilderArgQuery)
        buildParameter(node.entityBuilderArgMutation)
        buildParameter(node.entityBuilderArgProjection)
        returns(node.entityClass)

        statement(node.implClass) {
            "return %T(${node.entityBuilderArgQuery.first}, " +
                    "${node.entityBuilderArgMutation.first}, ${node.entityBuilderArgProjection.first}, this)"
        }
    }
}

private fun FileSpecBuilder.buildInterfaceOrUnionEntityBuilder(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildFunction(node.entityBuilderName) {
        receiver(node.dtoClass)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }

        buildParameter(node.entityBuilderArgQuery)
        buildParameter(node.entityBuilderArgMutation)
        buildParameter(node.entityBuilderArgProjection)
        returns(node.entityClass)

        controlFlow("return when(this)") {
            node.subObjects { subObject ->
                statement(subObject.dtoClass, subObject.implClass) {
                    "is %T -> %T(" +
                            "${node.entityBuilderArgQuery.first}, " +
                            "${node.entityBuilderArgMutation.first}, " +
                            "${node.entityBuilderArgProjection.first}.${subObject.innerProjectionOnName}, " +
                            "this)"
                }
            }
            addStatement("else -> error(%P)", "Invalid algorithm - unexpected dto type: \${this::class.simpleName}")
        }
    }
}

private fun FileSpecBuilder.buildResolvers(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    node.fields.values.asSequence().filter { it.type.hasProjection }.forEach { field ->
        buildFunction(field.resolverName) {
            receiver(node.dtoClass)
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }

            buildParameter(field.resolverArgQuery)
            buildParameter(field.resolverArgMutation)
            buildParameter(field.resolverArgProjection)
            returns(field.type.entityType)

            val builderCall = "${field.type.node.entityBuilderName}(" +
                    "${field.resolverArgQuery.first}, " +
                    "${field.resolverArgMutation.first}, " +
                    "${field.resolverArgProjection.first})"

            if (field.type.run { !nullable && list }) {
                statement(ClassName("kotlin.collections", "listOf")) {
                    "return " + field.type.expand(field.name, true) { builderCall } + " ?: %T()"
                }
            } else {
                statement {
                    "return " + field.type.expand(field.name, true) { builderCall } + field.notNullAssertion
                }
            }
        }
    }
}

private fun KobbyType.expand(receiver: String, resolveNull: Boolean, block: () -> String): String = buildString {
    append(receiver)
    if (resolveNull) {
        append('?')
    }
    append('.')

    nestedOrNull?.also {
        append("map·{ ").append(it.expand("it", it.nullable, block)).append(" }")
    } ?: append(block())
}

private fun FileSpecBuilder.buildEntity(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implName) {
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }
        addSuperinterface(node.entityClass)

        buildPrimaryConstructorProperties {
            // __query
            buildProperty(entity.queryProperty, node.schema.query.entityClass) {
                addModifiers(KModifier.OVERRIDE)
            }

            // __mutation
            buildProperty(entity.mutationProperty, node.schema.mutation.entityClass) {
                addModifiers(KModifier.OVERRIDE)
            }

            // projection
            buildProperty(impl.projectionPropertyName, node.implProjectionClass) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
            }

            // dto
            buildProperty(impl.dtoPropertyName, node.dtoClass) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
            }
        }

        buildFunction(entity.withCurrentProjectionFun) {
            addModifiers(KModifier.OVERRIDE)
            receiver(node.projectionClass)

            statement {
                "${impl.projectionPropertyName}.${impl.repeatProjectionFunName}(this)"
            }
        }

        node.fields { field ->
            buildProperty(field.name, field.type.entityType) {
                addModifiers(KModifier.OVERRIDE)

                if (field.type.hasProjection) {
                    val projectionRef = "${impl.projectionPropertyName}.${field.innerName}"
                    buildLazyDelegate {
                        ifFlowStatement(
                            "$projectionRef == null",
                            ClassName("kotlin", "error"),
                            field.noProjectionMessage
                        ) { "%T(%S)" }

                        statement {
                            "${impl.dtoPropertyName}.${field.resolverName}(" +
                                    "${entity.queryProperty}, ${entity.mutationProperty}, $projectionRef!!)"
                        }
                    }
                } else {
                    buildGetter {
                        if (!field.isRequired) {
                            val ref = "${impl.projectionPropertyName}.${field.innerName}"
                            ifFlowStatement(
                                if (field.innerIsBoolean) "!$ref" else "$ref == null",
                                ClassName("kotlin", "error"),
                                field.noProjectionMessage
                            ) { "%T(%S)" }
                        }

                        statement {
                            "return ${impl.dtoPropertyName}.${field.name}${field.notNullAssertion}"
                        }
                    }
                }
            }
        }
    }
}

private fun FileSpecBuilder.buildSelection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
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

private fun FileSpecBuilder.buildProjection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implProjectionName) {
        addModifiers(KModifier.OPEN)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }
        addSuperinterface(node.qualifiedProjectionClass)

        if (node.kind == INTERFACE) {
            buildCompanionObject {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                buildProperty(impl.interfaceIgnore) {
                    if (impl.internal) {
                        addModifiers(KModifier.INTERNAL)
                    }
                    val ignore = node.fields.values.joinToString { "\"${it.name}\"" }
                    initializer("%T($ignore)", ClassName("kotlin.collections", "setOf"))
                }
            }
        }

        node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
            buildProperty(field.innerName, field.innerType) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                mutable()
                initializer(field.innerInitializer)
            }
            field.arguments.values.asSequence()
                .filter { !field.isSelection || !it.type.nullable }
                .forEach { arg ->
                    buildProperty(arg.innerName, arg.type.entityType.nullable()) {
                        if (impl.internal) {
                            addModifiers(KModifier.INTERNAL)
                        }
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
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
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
            buildParameter(buildFunArgIgnore)
            buildParameter(buildFunArgHeader)
            buildParameter(buildFunArgBody)
            buildParameter(buildFunArgArguments)

            controlFlow("${buildFunArgBody.first}.apply") {
                spaceAppend('{')

                addStatement("")
                node.fields { field ->
                    val fieldCondition = when {
                        field.isRequired -> "%S !in ${buildFunArgIgnore.first}"
                        field.innerIsBoolean -> "%S !in ${buildFunArgIgnore.first} && ${field.innerName}"
                        else -> "%S !in ${buildFunArgIgnore.first} && ${field.innerName} != null"
                    }
                    addComment("Field: ${field.name}")
                    ifFlow(fieldCondition, field.name) {
                        spaceAppend(field.name)

                        // build arguments
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
                                    addStatement("append(%S).append(%S).append('$').append(arg)", arg.name, ": ")

                                    addStatement("")
                                    ifFlow("${buildFunArgHeader.first}.isNotEmpty()") {
                                        addStatement("${buildFunArgHeader.first}.append(%S)", ", ")
                                    }
                                    addStatement(
                                        "${buildFunArgHeader.first}.append('$').append(arg).append(%S).append(%S)",
                                        ": ", arg.type.sourceName
                                    )
                                }
                                addStatement("")
                            }
                            append(')')
                        }

                        // build field projection
                        if (field.type.hasProjection) {
                            addStatement("")
                            addComment("Build nested projection of ${field.type.node.name}")
                            addStatement(
                                "${field.innerName}!!.${impl.buildFunName}(" +
                                        "%T(), " +
                                        "${buildFunArgHeader.first}, " +
                                        "${buildFunArgBody.first}, " +
                                        "${buildFunArgArguments.first})",
                                ClassName("kotlin.collections", "setOf")
                            )
                        }
                    }
                    addStatement("")
                }

                if (node.kind == INTERFACE || node.kind == UNION) {
                    spaceAppend("__typename")
                    addStatement("")
                    addStatement("val ${buildFunValSubBody.first} = %T()", buildFunValSubBody.second)
                    addStatement("")
                    node.subObjects { subObject ->
                        addComment("Qualification of: ${subObject.name}")
                        addStatement("${buildFunValSubBody.first}.clear()")
                        if (node.kind == INTERFACE) {
                            addStatement(
                                "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                        "${impl.interfaceIgnore.first}, " +
                                        "${buildFunArgHeader.first}, " +
                                        "${buildFunValSubBody.first}, " +
                                        "${buildFunArgArguments.first})"
                            )
                        } else {
                            addStatement(
                                "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                        "%T(), " +
                                        "${buildFunArgHeader.first}, " +
                                        "${buildFunValSubBody.first}, " +
                                        "${buildFunArgArguments.first})",
                                ClassName("kotlin.collections", "setOf")
                            )
                        }
                        ifFlowStatement(
                            "${buildFunValSubBody.first}.length > 4",
                            " ... on ", subObject.name
                        ) {
                            "append(%S).append(%S).append(${buildFunValSubBody.first})"
                        }

                        addStatement("")
                    }
                }

                spaceAppend('}')
            }
        }
    }
}

private fun FileSpecBuilder.buildQueryOrMutation(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implName) {
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }
        addSuperinterface(node.entityClass)

        buildPrimaryConstructorProperties {
            buildProperty(node.qmArgAdapter) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
            }
            buildProperty(node.qmArgRef) {
                addModifiers(KModifier.PRIVATE)
            }
        }

        buildProperty(node.qmProperty) {
            addModifiers(KModifier.OVERRIDE)
            buildGetter {
                addStatement("return ${node.qmArgRef.first}[0]!!")
            }
        }

        node.fields { field ->
            buildFunction(field.name) {
                returns(field.type.entityType)
                addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)

                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.type.nullable }
                    .forEach { arg ->
                        buildParameter(arg.name, arg.type.entityType)
                    }

                val qmValProjection = field.qmValProjection
                field.lambda?.also {
                    buildParameter(it)
                    addStatement("val $qmValProjection = %T().apply(${it.first})", field.innerClass)
                }



                addStatement("TODO()")
            }
        }
    }
}
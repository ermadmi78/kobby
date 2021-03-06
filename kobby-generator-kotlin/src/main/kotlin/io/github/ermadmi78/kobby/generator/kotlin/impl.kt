package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbyNodeKind.INTERFACE
import io.github.ermadmi78.kobby.model.KobbyNodeKind.UNION
import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.KobbyType

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

    files
}

private fun FileSpecBuilder.buildObjectEntityBuilder(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildFunction(node.entityBuilderName) {
        receiver(node.dtoClass)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }

        buildParameter(impl.contextPropertyName, context.contextClass)
        buildParameter(node.implProjectionProperty)
        returns(node.entityClass)

        statement(node.implClass) {
            "return %T(${impl.contextPropertyName}, ${impl.projectionPropertyName}, this)"
        }
    }
}

private fun FileSpecBuilder.buildInterfaceOrUnionEntityBuilder(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildFunction(node.entityBuilderName) {
        receiver(node.dtoClass)
        if (impl.internal) {
            addModifiers(KModifier.INTERNAL)
        }

        buildParameter(impl.contextPropertyName, context.contextClass)
        buildParameter(node.implProjectionProperty)
        returns(node.entityClass)

        controlFlow("return when(this)") {
            node.subObjects { subObject ->
                statement(subObject.dtoClass, subObject.implClass) {
                    "is %T -> %T(" +
                            "${impl.contextPropertyName}, " +
                            "${impl.projectionPropertyName}.${subObject.innerProjectionOnName}, " +
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

            buildParameter(impl.contextPropertyName, context.contextClass)
            buildParameter(field.type.node.implProjectionProperty)
            returns(field.entityType)

            val builderCall = "${field.type.node.entityBuilderName}(" +
                    "${impl.contextPropertyName}, " +
                    "${impl.projectionPropertyName})"

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
            // context
            buildProperty(impl.contextPropertyName, context.contextClass) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
            }

            // projection
            buildProperty(node.implProjectionProperty) {
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

        // context query
        buildFunction(context.query) {
            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            buildParameter(entity.projection.projectionArgument, node.schema.query.projectionLambda)
            returns(node.schema.query.entityClass)

            addStatement(
                "return ${impl.contextPropertyName}.${context.query}(${entity.projection.projectionArgument})"
            )
        }

        // context mutation
        buildFunction(context.mutation) {
            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            buildParameter(entity.projection.projectionArgument, node.schema.mutation.projectionLambda)
            returns(node.schema.mutation.entityClass)

            addStatement(
                "return ${impl.contextPropertyName}.${context.mutation}(${entity.projection.projectionArgument})"
            )
        }

        // withCurrentProjection
        buildFunction(entity.withCurrentProjectionFun) {
            addModifiers(KModifier.OVERRIDE)
            receiver(node.projectionClass)

            statement(ClassName("kotlin.collections", "setOf")) {
                "${impl.projectionPropertyName}.${impl.repeatProjectionFunName}(%T(), this)"
            }
        }

        node.fields { field ->
            buildProperty(field.name, field.entityType) {
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
                                    "${impl.contextPropertyName}, $projectionRef!!)"
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
    node.fields.values.asSequence().filter { !it.isOverride && it.isSelection }.forEach { field ->
        val isQuery = field.type.hasProjection
        buildClass(if (isQuery) field.implQueryName else field.implSelectionName) {
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }
            if (isQuery) {
                superclass(field.type.node.implProjectionClass)
            }
            addSuperinterface(if (isQuery) field.queryClass else field.selectionClass)
            field.arguments.values.asSequence().filter { it.isInitialized }.forEach { arg ->
                buildProperty(arg.name, arg.entityType) {
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
                field.arguments.values.asSequence().filter { it.isInitialized }.forEach { arg ->
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
            buildProperty(impl.interfaceIgnore) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                val ignore = node.fields.values.asSequence()
                    .filter { it.isRequired || it.isDefault }
                    .map { it.name }
                    .toList()
                initializer(
                    "%T(${ignore.joinToString { "%S" }})",
                    ClassName("kotlin.collections", "mutableSetOf"),
                    *ignore.toTypedArray()
                )
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
                .filter { !field.isSelection || !it.isInitialized }
                .forEach { arg ->
                    buildProperty(arg.innerName, arg.entityType.nullable()) {
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
                    .filter { !field.isSelection || !it.isInitialized }
                    .forEach { arg ->
                        buildParameter(arg.name, arg.entityType)
                    }

                field.lambda?.also {
                    buildParameter(it)
                    addStatement("${field.innerName} = %T().apply(${it.first})", field.innerClass)
                } ?: addStatement("${field.innerName} = ${!field.isDefault}")

                field.arguments.values.asSequence()
                    .filter { !field.isSelection || !it.isInitialized }
                    .forEach { arg ->
                        addStatement("${arg.innerName} = ${arg.name}")
                    }

                if (node.kind == INTERFACE || node.kind == UNION) {
                    node.subObjects { subObject ->
                        addStatement("")

                        val subField = subObject.fields[field.name]!!
                        addStatement("${subObject.innerProjectionOnName}.${subField.innerName} = ${field.innerName}")
                        subField.arguments.values.asSequence()
                            .filter { !subField.isSelection || !it.isInitialized }
                            .forEach { subArg ->
                                addStatement("${subObject.innerProjectionOnName}.${subArg.innerName} = ${subArg.name}")
                            }
                    }

                    if (node.kind == INTERFACE && !field.isDefault) {
                        addStatement("")
                        addStatement("${impl.interfaceIgnore.first} += %S", field.name)
                    }
                }
            }
        }

        node.subObjects { subObject ->
            buildProperty(subObject.innerProjectionOnName, subObject.implProjectionClass) {
                if (impl.internal) {
                    addModifiers(KModifier.INTERNAL)
                }
                initializer("%T()", subObject.implProjectionClass)
            }
            buildFunction(subObject.projectionOnName) {
                addModifiers(KModifier.OVERRIDE)
                buildParameter(entity.projection.projectionArgument, subObject.projectionLambda)
                if (node.kind == INTERFACE) {
                    addStatement(
                        "%T().apply(${entity.projection.projectionArgument}).${impl.repeatProjectionFunName}" +
                                "(${impl.interfaceIgnore.first}, ${subObject.innerProjectionOnName})",
                        subObject.implProjectionClass
                    )
                } else {
                    addStatement(
                        "%T().apply(${entity.projection.projectionArgument}).${impl.repeatProjectionFunName}" +
                                "(%T(), ${subObject.innerProjectionOnName})",
                        subObject.implProjectionClass,
                        ClassName("kotlin.collections", "setOf")
                    )
                }
            }
        }

        buildFunction(impl.repeatProjectionFunName) {
            suppressUnused()
            if (impl.internal) {
                addModifiers(KModifier.INTERNAL)
            }

            val ignore = buildFunArgIgnore.first
            buildParameter(buildFunArgIgnore)

            val repeat = entity.projection.projectionArgument
            buildParameter(repeat, node.qualifiedProjectionClass)

            node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
                val condition = if (field.innerIsBoolean) "${if (field.isDefault) "!" else ""}${field.innerName}"
                else "${field.innerName} != null"

                ifFlow("%S !in $ignore && $condition", field.name) {
                    var args = field.arguments.values.asSequence()
                        .filter { !field.isSelection || !it.isInitialized }
                        .joinToString {
                            it.innerName + if (it.isInitialized) "" else "!!"
                        }
                    if (field.type.hasProjection || field.isSelection) {
                        if (args.isNotEmpty()) {
                            args = "($args)"
                        }
                        controlFlow("$repeat.${field.projectionFieldName}$args") {
                            if (field.type.hasProjection) {
                                statement(ClassName("kotlin.collections", "setOf")) {
                                    "this@${node.implProjectionName}" +
                                            ".${field.innerName}!!.${impl.repeatProjectionFunName}(%T(), this)"
                                }
                            }
                            if (field.isSelection) statement {
                                "this@${node.implProjectionName}" +
                                        ".${field.innerName}!!.${impl.repeatSelectionFunName}(this)"
                            }
                        }
                    } else {
                        addStatement("$repeat.${field.projectionFieldName}($args)")
                    }
                }

            }

            node.subObjects { subObject ->
                controlFlow("$repeat.${subObject.projectionOnName}") {
                    statement(ClassName("kotlin.collections", "setOf")) {
                        "this@${node.implProjectionName}.${subObject.innerProjectionOnName}" +
                                ".${impl.repeatProjectionFunName}(%T(), this)"
                    }
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

            val ignore = buildFunArgIgnore.first
            val header = buildFunArgHeader.first
            val body = buildFunArgBody.first
            val arguments = buildFunArgArguments.first

            buildAppendChain(body) { spaceAppendLiteral('{') }

            addStatement("")
            node.fields { field ->
                val fieldCondition = when {
                    field.isRequired -> "%S !in $ignore"
                    field.innerIsBoolean -> "%S !in $ignore && ${field.innerName}"
                    else -> "%S !in $ignore && ${field.innerName} != null"
                }
                addComment("Field: ${field.name}")
                ifFlow(fieldCondition, field.name) {
                    buildAppendChain(body) { spaceAppendLiteral(field.name) }

                    // build arguments
                    if (field.arguments.isNotEmpty()) {
                        addStatement("var counter = 0")
                        val addBracketsExpression: String = field.arguments.values.let { args ->
                            if (!args.any { it.isInitialized }) "true"
                            else args.asSequence()
                                .filter { it.isInitialized }
                                .map { arg ->
                                    if (arg.isSelection) "${field.innerName}!!.${arg.name}" else arg.innerName
                                }
                                .joinToString(" || ") { "$it != null" }
                        }
                        addStatement("val addBrackets = $addBracketsExpression")
                        ifFlow("addBrackets") {
                            buildAppendChain(body) { appendLiteral('(') }
                        }
                        addStatement("")
                        field.arguments { arg ->
                            val argName = if (arg.isSelection) "${field.innerName}!!.${arg.name}" else arg.innerName
                            addComment("Argument: ${field.name}.${arg.name}")
                            ifFlow(if (arg.isInitialized) "$argName != null" else "true") {
                                ifFlow("counter++ > 0") {
                                    buildAppendChain(body) { appendLiteral(", ") }
                                }
                                val mapName = arguments
                                addStatement("val arg = %S + $mapName.size", argPrefix)
                                addStatement("$mapName[arg] = $argName!!")
                                buildAppendChain(body) {
                                    appendLiteral(arg.name)
                                    appendLiteral(": ")
                                    appendLiteral('$')
                                    appendExactly("arg")
                                }

                                addStatement("")
                                ifFlow("$header.isNotEmpty()") {
                                    buildAppendChain(header) { appendLiteral(", ") }
                                }
                                buildAppendChain(header) {
                                    appendLiteral('$')
                                    appendExactly("arg")
                                    appendLiteral(": ")
                                    appendLiteral(arg.type.sourceName)
                                }
                            }
                            addStatement("")
                        }
                        ifFlow("addBrackets") {
                            buildAppendChain(body) { appendLiteral(')') }
                        }
                    }

                    // build field projection
                    if (field.type.hasProjection) {
                        addStatement("")
                        addComment("Build nested projection of ${field.type.node.name}")
                        addStatement(
                            "${field.innerName}!!.${impl.buildFunName}(" +
                                    "%T(), " +
                                    "$header, " +
                                    "$body, " +
                                    "$arguments)",
                            ClassName("kotlin.collections", "setOf")
                        )
                    }
                }
                addStatement("")
            }

            if (node.kind == INTERFACE || node.kind == UNION) {
                val subBody = buildFunValSubBody.first

                buildAppendChain(body) { spaceAppendLiteral("__typename") }
                addStatement("")
                addStatement("val $subBody = %T()", buildFunValSubBody.second)
                addStatement("")
                node.subObjects { subObject ->
                    addComment("Qualification of: ${subObject.name}")
                    addStatement("$subBody.clear()")
                    if (node.kind == INTERFACE) {
                        addStatement(
                            "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                    "${impl.interfaceIgnore.first}, " +
                                    "$header, " +
                                    "$subBody, " +
                                    "$arguments)"
                        )
                    } else {
                        addStatement(
                            "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                    "%T(), " +
                                    "$header, " +
                                    "$subBody, " +
                                    "$arguments)",
                            ClassName("kotlin.collections", "setOf")
                        )
                    }
                    ifFlow("$subBody.length > 4") {
                        buildAppendChain(body) {
                            appendLiteral(" ... on ")
                            appendLiteral(subObject.name)
                            appendExactly(subBody)
                        }
                    }

                    addStatement("")
                }
            }

            buildAppendChain(body) { spaceAppendLiteral('}') }
        }
    }
}

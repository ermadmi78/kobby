package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import io.github.ermadmi78.kobby.model.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.INTERFACE
import io.github.ermadmi78.kobby.model.KobbyNodeKind.UNION

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
    if (adapter.extendedApi && node.isOperation) {
        val graphqlResultClass: ClassName = when {
            node.isQuery -> dto.graphql.queryResultClass
            node.isMutation -> dto.graphql.mutationResultClass
            node.isSubscription -> dto.graphql.subscriptionResultClass
            else -> error("Invalid algorithm")
        }

        buildFunction(node.entityBuilderName) {
            receiver(graphqlResultClass)
            if (impl.internal) {
                addModifiers(INTERNAL)
            }

            buildParameter(impl.contextPropertyName, context.contextClass)
            buildParameter(node.implProjectionProperty)
            returns(node.entityClass)

            statement(
                node.implClass,
                node.dtoClass,
                MemberName("kotlin.collections", "emptyList"),
                if (dto.serialization.enabled) {
                    context.emptyJsonObjectMember
                } else {
                    MemberName("kotlin.collections", "emptyMap")
                }
            ) {
                "return·%T(" +
                        "\n⇥${impl.contextPropertyName}," +
                        "\n${impl.projectionPropertyName}," +
                        "\nthis.data·?:·%T()," +
                        "\nthis.errors·?:·%M()," +
                        "\nthis.extensions·?:·%M${if (dto.serialization.enabled) "" else "()"}" +
                        "⇤\n)"
            }
        }
    } else {
        buildFunction(node.entityBuilderName) {
            receiver(node.dtoClass)
            if (impl.internal) {
                addModifiers(INTERNAL)
            }

            buildParameter(impl.contextPropertyName, context.contextClass)
            buildParameter(node.implProjectionProperty)
            returns(node.entityClass)

            statement(node.implClass) {
                "return·%T(${impl.contextPropertyName},·${impl.projectionPropertyName},·this)"
            }
        }
    }
}

private fun FileSpecBuilder.buildInterfaceOrUnionEntityBuilder(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildFunction(node.entityBuilderName) {
        receiver(node.dtoClass)
        if (impl.internal) {
            addModifiers(INTERNAL)
        }

        buildParameter(impl.contextPropertyName, context.contextClass)
        buildParameter(node.implProjectionProperty)
        returns(node.entityClass)

        controlFlow("return·when(this)") {
            node.subObjects { subObject ->
                statement(subObject.dtoClass, subObject.implClass) {
                    "is·%T·->·%T(" +
                            "${impl.contextPropertyName},·" +
                            "${impl.projectionPropertyName}.${subObject.innerProjectionOnName},·" +
                            "this)"
                }
            }
            addStatement(
                "else·->·%T(%P)",
                ClassName("kotlin", "error"),
                "Invalid algorithm - unexpected dto type: \${this::class.simpleName}"
            )
        }
    }
}

private fun FileSpecBuilder.buildResolvers(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    node.fields.values.asSequence().filter { it.type.hasProjection }.forEach { field ->
        buildFunction(field.resolverName) {
            receiver(node.dtoClass)
            if (impl.internal) {
                addModifiers(INTERNAL)
            }

            buildParameter(impl.contextPropertyName, context.contextClass)
            buildParameter(field.type.node.implProjectionProperty)
            returns(field.entityType)

            val builderCall = "${field.type.node.entityBuilderName}(" +
                    "${impl.contextPropertyName},·" +
                    "${impl.projectionPropertyName})"

            if (field.type.run { !nullable && list }) {
                val args = mutableListOf<Any>()
                val expand = field.type.expand(field.name.escape(), true, args) { builderCall }
                args += ClassName("kotlin.collections", "listOf")
                addStatement("return·$expand·?:·%T()", *args.toTypedArray())
            } else {
                val args = mutableListOf<Any>()
                val expand = field.type.expand(field.name.escape(), true, args) { builderCall }
                addStatement("return·${expand}${field.notNullAssertion}", *args.toTypedArray())
            }
        }
    }
}

private fun KobbyType.expand(
    receiver: String,
    resolveNull: Boolean,
    args: MutableList<Any>,
    block: () -> String
): String = buildString {
    append(receiver)
    if (resolveNull) {
        append('?')
    }
    append('.')

    nestedOrNull?.also {
        args += MemberName("kotlin.collections", "map")
        append("%M·{·").append(it.expand("it", it.nullable, args, block)).append("·}")
    } ?: append(block())
}

private fun FileSpecBuilder.buildEntity(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implName) {
        if (impl.internal) {
            addModifiers(INTERNAL)
        }
        addSuperinterface(node.entityClass)

        val innerDto = impl.dtoPropertyName

        buildPrimaryConstructorProperties {
            // context
            buildProperty(impl.contextPropertyName, context.contextClass) {
                if (impl.internal) {
                    addModifiers(INTERNAL)
                }
            }

            // projection
            buildProperty(node.implProjectionProperty) {
                if (impl.internal) {
                    addModifiers(INTERNAL)
                }
            }

            // dto
            buildProperty(innerDto, node.dtoClass) {
                if (impl.internal) {
                    addModifiers(INTERNAL)
                }
            }

            if (adapter.extendedApi && node.isOperation) {
                // errors
                buildProperty(impl.errorsPropertyName, dto.errorsType) {
                    if (impl.internal) {
                        addModifiers(INTERNAL)
                    }
                }

                // extensions
                buildProperty(impl.extensionsPropertyName, dto.extensionsType) {
                    if (impl.internal) {
                        addModifiers(INTERNAL)
                    }
                }
            }
        }

        // Entity equals and hashCode generation by @primaryKey directive
        if (node.primaryKeysCount > 0) {
            buildFunction(EQUALS_FUN) {
                addModifiers(OVERRIDE)
                buildParameter(EQUALS_ARG, ANY.nullable())
                returns(BOOLEAN)

                ifFlowStatement("this·===·$EQUALS_ARG") {
                    "return·true"
                }
                ifFlowStatement("this.javaClass·!=·$EQUALS_ARG?.javaClass") {
                    "return·false"
                }

                addStatement("")
                addStatement("$EQUALS_ARG·as·%T", node.implClass)

                if (node.primaryKeysCount == 1) {
                    node.firstPrimaryKey().also {
                        addStatement("return·$innerDto.${it.name.escape()}·==·$EQUALS_ARG.$innerDto.${it.name.escape()}")
                    }
                } else {
                    addStatement("")
                    node.primaryKeys {
                        ifFlowStatement("$innerDto.${it.name.escape()}·!=·$EQUALS_ARG.$innerDto.${it.name.escape()}") {
                            "return·false"
                        }
                    }

                    addStatement("")
                    addStatement("return·true")
                }
            }

            buildFunction(HASH_CODE_FUN) {
                addModifiers(OVERRIDE)
                returns(INT)

                if (node.primaryKeysCount == 1) {
                    node.firstPrimaryKey().also {
                        addStatement("return·$innerDto.${it.name.escape()}?.hashCode()·?:·0")
                    }
                } else {
                    var first = true
                    node.primaryKeys {
                        if (first) {
                            first = false
                            addStatement("var·$HASH_CODE_RES·=·$innerDto.${it.name.escape()}?.hashCode()·?:·0")
                        } else {
                            addStatement(
                                "$HASH_CODE_RES·=·31·*·$HASH_CODE_RES·+·($innerDto.${it.name.escape()}?.hashCode()·?:·0)"
                            )
                        }
                    }
                    addStatement("return·$HASH_CODE_RES")
                }
            }
        }

        buildFunction(TO_STRING_FUN) {
            suppressUnused()
            addModifiers(OVERRIDE)
            returns(STRING)

            controlFlow("return·%M", MemberName("kotlin.text", "buildString")) {
                buildAppendChain {
                    appendLiteral(node.name)
                    appendLiteral('(')
                }

                if (node.fields.isNotEmpty()) {
                    addStatement("")
                    addStatement("var·counter·=·0")
                    addStatement("")
                }
                node.fields { field ->
                    val fieldCondition = when {
                        field.isRequired -> "true"
                        field.innerIsBoolean -> "${impl.projectionPropertyName}.${field.innerName}"
                        else -> "${impl.projectionPropertyName}.${field.innerName}·!=·null"
                    }
                    ifFlow(fieldCondition) {
                        ifFlow("counter++·>·0") {
                            buildAppendChain { appendLiteral(", ") }
                        }
                        buildAppendChain {
                            appendLiteral(field.name)
                            appendLiteral('=')
                            appendExactly(field.name.escape())
                        }
                    }
                    addStatement("")
                }

                buildAppendChain { appendLiteral(')') }
            }
        }

        if (adapter.extendedApi && node.isOperation) {
            buildFunction(entity.errorsFunName) {
                addModifiers(OVERRIDE)
                returns(dto.errorsType)

                addStatement("return·${impl.errorsPropertyName}")
            }

            buildFunction(entity.extensionsFunName) {
                addModifiers(OVERRIDE)
                returns(dto.extensionsType)

                addStatement("return·${impl.extensionsPropertyName}")
            }
        }

        if (entity.contextFunEnabled) {
            buildFunction(entity.contextFunName) {
                addModifiers(OVERRIDE)
                returns(context.contextClass)

                addStatement("return·${impl.contextPropertyName}")
            }
        }

        // withCurrentProjection
        buildFunction(entity.withCurrentProjectionFun) {
            addModifiers(OVERRIDE)
            receiver(node.projectionClass)

            statement(ClassName("kotlin.collections", "setOf")) {
                "${impl.projectionPropertyName}.${impl.repeatProjectionFunName}(%T(),·this)"
            }
        }

        node.fields { field ->
            buildProperty(field.name, field.entityType) {
                addModifiers(OVERRIDE)

                if (field.type.hasProjection) {
                    val projectionRef = "${impl.projectionPropertyName}.${field.innerName}"
                    buildLazyDelegate {
                        ifFlowStatement(
                            "$projectionRef·==·null",
                            ClassName("kotlin", "error"),
                            field.noProjectionMessage
                        ) { "%T(%S)" }

                        statement {
                            "$innerDto.${field.resolverName}(${impl.contextPropertyName},·$projectionRef!!)"
                        }
                    }
                } else {
                    buildGetter {
                        if (!field.isRequired) {
                            val ref = "${impl.projectionPropertyName}.${field.innerName}"
                            ifFlowStatement(
                                if (field.innerIsBoolean) "!$ref" else "$ref·==·null",
                                ClassName("kotlin", "error"),
                                field.noProjectionMessage
                            ) { "%T(%S)" }
                        }

                        statement {
                            "return·$innerDto.${field.name.escape()}${field.notNullAssertion}"
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
                addModifiers(INTERNAL)
            }
            if (isQuery) {
                superclass(field.type.node.implProjectionClass)
            }

            val parentFields = if (!isQuery) emptyMap() else field.type.node.fields.values.filter { it.isProjectionPropertyEnabled }.associateBy { it.name }
            fun KobbyArgument.isProjectionPropertyEnabled() = layout.entity.projection.enableNotationWithoutParentheses && name in parentFields

            addSuperinterface(if (isQuery) field.queryClass else field.selectionClass)
            field.arguments.values.asSequence().filter { it.isInitialized }.forEach { arg ->
                val argType = if (dto.serialization.enabled) {
                    arg.entityTypeWithSerializer
                } else {
                    arg.entityType
                }

                if (arg.isProjectionPropertyEnabled()) {
                    buildProperty(arg.innerName, argType) {
                        if (impl.internal) {
                            addModifiers(INTERNAL)
                        }
                        mutable()
                        initializer(field.innerInitializer)
                    }
                }

                buildProperty(arg.name, argType) {
                    mutable()
                    addModifiers(OVERRIDE)
                    if (arg.isProjectionPropertyEnabled()) {
                        val parentField = parentFields.getValue(arg.name)
                        buildGetter {
                            addStatement("super.${parentField.innerName}·=·${!parentField.isDefault}")
                            addStatement("return·${arg.innerName}")
                        }
                        buildSetter {
                            addParameter("value", argType)
                            addStatement("${arg.innerName}·=·value")
                        }
                    } else {
                        initializer("null")
                    }
                }
            }

            buildFunction(impl.repeatSelectionFunName) {
                suppressUnused()
                if (impl.internal) {
                    addModifiers(INTERNAL)
                }
                val repeat = entity.selection.selectionArgument
                buildParameter(repeat, field.selectionClass)
                field.arguments.values.asSequence().filter { it.isInitialized }.forEach { arg ->
                    val argName = if (arg.isProjectionPropertyEnabled()) arg.innerName else arg.name
                    addStatement("$repeat.${arg.name.escape()}·=·${argName.escape()}")
                }
            }
        }
    }
}

private fun FileSpecBuilder.buildProjection(node: KobbyNode, layout: KotlinLayout) = with(layout) {
    buildClass(node.implProjectionName) {
        addModifiers(OPEN)
        if (impl.internal) {
            addModifiers(INTERNAL)
        }
        addSuperinterface(node.qualifiedProjectionClass)

        if (node.kind == INTERFACE) {
            buildProperty(impl.interfaceIgnore) {
                if (impl.internal) {
                    addModifiers(INTERNAL)
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
                    addModifiers(INTERNAL)
                }
                mutable()
                initializer(field.innerInitializer)
            }
            field.arguments.values.asSequence()
                .filter { !field.isSelection || !it.isInitialized }
                .forEach { arg ->
                    val argType = if (dto.serialization.enabled) {
                        arg.entityTypeWithSerializer.nullable()
                    } else {
                        arg.entityType.nullable()
                    }
                    buildProperty(arg.innerName, argType) {
                        if (impl.internal) {
                            addModifiers(INTERNAL)
                        }
                        mutable()
                        initializer("null")
                    }
                }

            if (field.isProjectionPropertyEnabled) {
                buildProperty(field.projectionFieldName, ANY.nullable()) {
                    addModifiers(OVERRIDE)
                    buildGetter {
                        addStatement("${field.innerName}·=·${!field.isDefault}")
                        addStatement("return·null")
                    }
                }
            } else {
                buildFunction(field.projectionFieldName) {
                    addModifiers(OVERRIDE)
                    field.arguments.values.asSequence()
                        .filter { !field.isSelection || !it.isInitialized }
                        .forEach { arg ->
                            buildParameter(arg.name, arg.entityType)
                        }

                    field.lambda?.also {
                        buildParameter(it)
                        addStatement(
                            "${field.innerName}·=·%T().%M(${it.first})",
                            field.innerClass,
                            MemberName("kotlin", "apply")
                        )
                    } ?: addStatement("${field.innerName}·=·${!field.isDefault}")

                    field.arguments.values.asSequence()
                        .filter { !field.isSelection || !it.isInitialized }
                        .forEach { arg ->
                            addStatement("${arg.innerName}·=·${arg.name.escape()}")
                        }

                    if (node.kind == INTERFACE || node.kind == UNION) {
                        node.subObjects { subObject ->
                            addStatement("")

                            val subField = subObject.fields[field.name] ?: invalidSchema(
                                "The object type '${subObject.name}' does not have a field '${field.name}' " +
                                        "required via interface '${node.name}'"
                            )
                            addStatement("${subObject.innerProjectionOnName}.${subField.innerName}·=·${field.innerName}")
                            subField.arguments.values.asSequence()
                                .filter { !subField.isSelection || !it.isInitialized }
                                .forEach { subArg ->
                                    addStatement("${subObject.innerProjectionOnName}.${subArg.innerName}·=·${subArg.name.escape()}")
                                }
                        }

                        if (node.kind == INTERFACE && !field.isDefault) {
                            addStatement("")
                            addStatement("${impl.interfaceIgnore.first}·+=·%S", field.name)
                        }
                    }
                }
            }
        }

        node.subObjects { subObject ->
            buildProperty(subObject.innerProjectionOnName, subObject.implProjectionClass) {
                if (impl.internal) {
                    addModifiers(INTERNAL)
                }
                initializer("%T()", subObject.implProjectionClass)
            }
            buildFunction(subObject.projectionOnName) {
                addModifiers(OVERRIDE)
                buildParameter(entity.projection.projectionArgument, subObject.projectionLambda)
                if (node.kind == INTERFACE) {
                    addStatement(
                        "%T().%M(${entity.projection.projectionArgument}).${impl.repeatProjectionFunName}" +
                                "(${impl.interfaceIgnore.first},·${subObject.innerProjectionOnName})",
                        subObject.implProjectionClass,
                        MemberName("kotlin", "apply")
                    )
                } else {
                    addStatement(
                        "%T().%M(${entity.projection.projectionArgument}).${impl.repeatProjectionFunName}" +
                                "(%T(),·${subObject.innerProjectionOnName})",
                        subObject.implProjectionClass,
                        MemberName("kotlin", "apply"),
                        ClassName("kotlin.collections", "setOf")
                    )
                }
            }
        }

        buildFunction(impl.repeatProjectionFunName) {
            suppressUnused()
            if (impl.internal) {
                addModifiers(INTERNAL)
            }

            val ignore = buildFunArgIgnore.first
            buildParameter(buildFunArgIgnore)

            val repeat = entity.projection.projectionArgument
            buildParameter(repeat, node.qualifiedProjectionClass)

            node.fields.values.asSequence().filter { !it.isRequired }.forEach { field ->
                val condition = if (field.innerIsBoolean) "${if (field.isDefault) "!" else ""}${field.innerName}"
                else "${field.innerName}·!=·null"

                ifFlow("%S·!in·$ignore·&&·$condition", field.name) {
                    var args = field.arguments.values.asSequence()
                        .filter { !field.isSelection || !it.isInitialized }
                        .joinToString {
                            it.innerName + if (it.isInitialized) "" else "!!"
                        }
                    if (field.type.hasProjection || field.isSelection) {
                        if (args.isNotEmpty()) {
                            args = "($args)"
                        }
                        controlFlow("$repeat.${field.projectionFieldName.escape()}$args") {
                            if (field.type.hasProjection) {
                                statement(ClassName("kotlin.collections", "setOf")) {
                                    "this@${node.implProjectionName}" +
                                            ".${field.innerName}!!.${impl.repeatProjectionFunName}(%T(),·this)"
                                }
                            }
                            if (field.isSelection) statement {
                                "this@${node.implProjectionName}" +
                                        ".${field.innerName}!!.${impl.repeatSelectionFunName}(this)"
                            }
                        }
                    } else {
                        addStatement(
                            buildString {
                                append(repeat)
                                append('.')
                                append(field.projectionFieldName.escape())
                                if (!field.isProjectionPropertyEnabled) {
                                    append("($args)")
                                }
                            }
                        )
                    }
                }
            }

            node.subObjects { subObject ->
                controlFlow("$repeat.${subObject.projectionOnName}") {
                    statement(ClassName("kotlin.collections", "setOf")) {
                        "this@${node.implProjectionName}.${subObject.innerProjectionOnName}" +
                                ".${impl.repeatProjectionFunName}(%T(),·this)"
                    }
                }
            }
        }

        buildFunction(impl.buildFunName) {
            addKdoc("%L", "Projection builder function")
            suppressUnused()
            if (impl.internal) {
                addModifiers(INTERNAL)
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
                addComment("Field: ${field.name}")
                if (field.arguments.isEmpty()) {
                    writeFieldProjectionBuilderCode(field, layout)
                } else {
                    val projectionBuilderFun = field.projectionBuilderFunName
                    this@buildClass.buildFunction(projectionBuilderFun) {
                        addKdoc("%L", "Projection for field: ${field.name}")
                        suppressUnused()
                        addModifiers(PRIVATE)
                        buildParameter(buildFunArgIgnore)
                        buildParameter(buildFunArgHeader)
                        buildParameter(buildFunArgBody)
                        buildParameter(buildFunArgArguments)

                        writeFieldProjectionBuilderCode(field, layout)
                    }
                    addStatement("$projectionBuilderFun($ignore,·$header,·$body,·$arguments)")
                }
                addStatement("")
            }

            if (node.kind == INTERFACE || node.kind == UNION) {
                val subBody = buildFunValSubBody.first

                buildAppendChain(body) { spaceAppendLiteral("__typename") }
                addStatement("")
                addStatement("val·$subBody·=·%T()", buildFunValSubBody.second)
                addStatement("")
                node.subObjects { subObject ->
                    addComment("Qualification of: ${subObject.name}")
                    addStatement("$subBody.clear()")
                    if (node.kind == INTERFACE) {
                        addStatement(
                            "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                    "${impl.interfaceIgnore.first},·" +
                                    "$header,·" +
                                    "$subBody,·" +
                                    "$arguments)"
                        )
                    } else {
                        addStatement(
                            "${subObject.innerProjectionOnName}.${impl.buildFunName}(" +
                                    "%T(),·" +
                                    "$header,·" +
                                    "$subBody,·" +
                                    "$arguments)",
                            ClassName("kotlin.collections", "setOf")
                        )
                    }
                    ifFlow("$subBody.length·>·4") {
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

private fun FunSpecBuilder.writeFieldProjectionBuilderCode(field: KobbyField, layout: KotlinLayout) = with(layout) {
    val ignore = buildFunArgIgnore.first
    val header = buildFunArgHeader.first
    val body = buildFunArgBody.first
    val arguments = buildFunArgArguments.first

    val fieldCondition = when {
        field.isRequired -> "%S·!in·$ignore"
        field.innerIsBoolean -> "%S·!in·$ignore·&&·${field.innerName}"
        else -> "%S·!in·$ignore·&&·${field.innerName}·!=·null"
    }
    ifFlow(fieldCondition, field.name) {
        buildAppendChain(body) { spaceAppendLiteral(field.name) }

        // build arguments
        if (field.arguments.isNotEmpty()) {
            addStatement("var·counter·=·0")
            val addBracketsExpression: String = field.arguments.values.let { args ->
                if (args.any { !it.isInitialized }) "true"
                else args.asSequence()
                    .map { arg ->
                        require(arg.isInitialized) { "Invalid algorithm" }
                        if (arg.isSelection) "${field.innerName}!!.${arg.name.escape()}" else arg.innerName
                    }
                    .joinToString(" || ") { "$it·!=·null" }
            }
            addStatement("val·addBrackets·=·$addBracketsExpression")
            ifFlow("addBrackets") {
                buildAppendChain(body) { appendLiteral('(') }
            }
            addStatement("")
            field.arguments { arg ->
                val argName =
                    if (arg.isSelection) "${field.innerName}!!.${arg.name.escape()}" else arg.innerName
                addComment("Argument: ${field.name}.${arg.name}")
                ifFlow(if (arg.isInitialized) "$argName·!=·null" else "true") {
                    ifFlow("counter++·>·0") {
                        buildAppendChain(body) { appendLiteral(", ") }
                    }
                    addStatement("val·arg·=·%S·+·$arguments.size", argPrefix)
                    if (dto.serialization.enabled) {
                        addStatement(
                            "$arguments[arg]·=·%M.%M($argName!!)",
                            context.jsonMember,
                            MemberName("kotlinx.serialization.json", "encodeToJsonElement")
                        )
                    } else {
                        addStatement("$arguments[arg]·=·$argName!!")
                    }
                    buildAppendChain(body) {
                        appendLiteral(arg.name)
                        appendLiteral(": ")
                        appendLiteral('$')
                        appendExactly("arg")
                    }

                    addStatement("")
                    ifFlow(
                        "$header.%M()",
                        MemberName("kotlin.text", "isNotEmpty")
                    ) {
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
                        "%T(),·" +
                        "$header,·" +
                        "$body,·" +
                        "$arguments)",
                ClassName("kotlin.collections", "setOf")
            )
        }
    }
}
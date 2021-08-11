package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_INCLUDE
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_SUB_TYPES
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_SUB_TYPES_TYPE
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_TYPE_INFO
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_TYPE_NAME
import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 23.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objects { node ->
        files += buildFile(dto.packageName, node.dtoName) {
            // Build object DTO class
            buildClass(node.dtoName) {
                jacksonizeClass(node)
                if (node.fields.isNotEmpty()) {
                    addModifiers(KModifier.DATA)
                }
                node.comments {
                    addKdoc(it)
                }
                node.implements {
                    addSuperinterface(it.dtoClass)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc(it)
                            }
                            if (field.isOverride) {
                                addModifiers(KModifier.OVERRIDE)
                            }
                        }
                    }
                    customizeConstructor {
                        jacksonizeConstructor()
                    }
                }

                // DTO equals and hashCode generation by @primaryKey directive
                if (dto.applyPrimaryKeys && node.primaryKeysCount > 0) {
                    buildFunction(EQUALS_FUN) {
                        addModifiers(KModifier.OVERRIDE)
                        buildParameter(EQUALS_ARG, ANY.nullable())
                        returns(BOOLEAN)

                        ifFlowStatement("this·===·$EQUALS_ARG") {
                            "return·true"
                        }
                        ifFlowStatement("javaClass·!=·$EQUALS_ARG?.javaClass") {
                            "return·false"
                        }

                        addStatement("")
                        addStatement("$EQUALS_ARG·as·%T", node.dtoClass)

                        if (node.primaryKeysCount == 1) {
                            node.firstPrimaryKey().also {
                                addStatement("return·${it.name}·==·$EQUALS_ARG.${it.name}")
                            }
                        } else {
                            addStatement("")
                            node.primaryKeys {
                                ifFlowStatement("${it.name}·!=·$EQUALS_ARG.${it.name}") {
                                    "return·false"
                                }
                            }

                            addStatement("")
                            addStatement("return·true")
                        }
                    }

                    buildFunction(HASH_CODE_FUN) {
                        addModifiers(KModifier.OVERRIDE)
                        returns(INT)

                        if (node.primaryKeysCount == 1) {
                            node.firstPrimaryKey().also {
                                addStatement("return·${it.name}?.hashCode()·?:·0")
                            }
                        } else {
                            var first = true
                            node.primaryKeys {
                                if (first) {
                                    first = false
                                    addStatement("var·$HASH_CODE_RES·=·${it.name}?.hashCode()·?:·0")
                                } else {
                                    addStatement("$HASH_CODE_RES·=·31·*·$HASH_CODE_RES·+·(${it.name}?.hashCode()·?:·0)")
                                }
                            }
                            addStatement("return·$HASH_CODE_RES")
                        }
                    }
                }
            }

            // Build object DTO builder
            if (dto.builder.enabled && node.fields.isNotEmpty()) {
                // Builder function
                buildFunction(node.dtoName) {
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement("// ${node.dtoName} builder DSL")
                    controlFlow(
                        "return·%T().%T(block).%T",
                        node.builderClass,
                        ClassName("kotlin", "apply"),
                        ClassName("kotlin", "let")
                    ) {
                        val arguments = node.fields.values.joinToString(",\n") { "it.${it.name}" }
                        addStatement("%T(\n⇥$arguments⇤\n)", node.dtoClass)
                    }
                }

                // Copy function
                buildFunction(dto.builder.copyFun) {
                    receiver(node.dtoClass)
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement("//·${node.dtoName}·${dto.builder.copyFun}·DSL")
                    controlFlow(
                        "return·%T().%T",
                        node.builderClass,
                        ClassName("kotlin", "also")
                    ) {
                        node.fields { field ->
                            addStatement("it.${field.name} = ${field.name}")
                        }
                    }
                    controlFlow(
                        ".%T(block).%T",
                        ClassName("kotlin", "apply"),
                        ClassName("kotlin", "let")
                    ) {
                        val arguments = node.fields.values.joinToString(",\n") { "it.${it.name}" }
                        addStatement("%T(\n⇥$arguments⇤\n)", node.dtoClass)
                    }
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc(it)
                    }
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc(it)
                            }
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
                jacksonizeInterface(node)
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
                        if (field.isOverride) {
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
                jacksonizeInterface(node)
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
                jacksonizeClass(node)
                addModifiers(KModifier.DATA)
                node.comments {
                    addKdoc(it)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        val defaultValue: CodeBlock? = field.defaultValue?.let { literal ->
                            val args = mutableListOf<Any?>()
                            val format = literal.buildInitializer(field.type, args)
                            CodeBlock.of(format, *args.toTypedArray())
                        }
                        buildPropertyWithDefault(field.name, field.type.dtoType, defaultValue) {
                            field.comments {
                                addKdoc(it)
                            }
                            field.defaultValue?.also { literal ->
                                if (field.comments.isNotEmpty()) {
                                    addKdoc("  \n> ")
                                }
                                addKdoc("Default: $literal")
                            }
                        }
                    }
                    customizeConstructor {
                        jacksonizeConstructor()
                    }
                }
            }

            // Build input DTO builder
            if (dto.builder.enabled && node.fields.isNotEmpty()) {
                // Builder function
                buildFunction(node.dtoName) {
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement("// ${node.dtoName} builder DSL")
                    controlFlow(
                        "return·%T().%T(block).%T",
                        node.builderClass,
                        ClassName("kotlin", "apply"),
                        ClassName("kotlin", "let")
                    ) {
                        val types: MutableList<ClassName> = mutableListOf(node.dtoClass)
                        val arguments = node.fields.values.joinToString(",\n") {
                            if (it.type.nullable || it.hasDefaultValue) {
                                "it.${it.name}"
                            } else {
                                types += ClassName("kotlin", "error")
                                "it.${it.name}·?:·%T(\"${node.dtoName}:·'${it.name}'·must·not·be·null\")"
                            }
                        }
                        addStatement("%T(\n⇥$arguments⇤\n)", *types.toTypedArray())
                    }
                }

                // Copy function
                buildFunction(dto.builder.copyFun) {
                    receiver(node.dtoClass)
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement("//·${node.dtoName}·${dto.builder.copyFun}·DSL")
                    controlFlow(
                        "return·%T().%T",
                        node.builderClass,
                        ClassName("kotlin", "also")
                    ) {
                        node.fields { field ->
                            addStatement("it.${field.name} = ${field.name}")
                        }
                    }
                    controlFlow(
                        ".%T(block).%T",
                        ClassName("kotlin", "apply"),
                        ClassName("kotlin", "let")
                    ) {
                        val types: MutableList<ClassName> = mutableListOf(node.dtoClass)
                        val arguments = node.fields.values.joinToString(",\n") {
                            if (it.type.nullable || it.hasDefaultValue) {
                                "it.${it.name}"
                            } else {
                                types += ClassName("kotlin", "error")
                                "it.${it.name}·?:·%T(\"${node.dtoName}:·'${it.name}'·must·not·be·null\")"
                            }
                        }
                        addStatement("%T(\n⇥$arguments⇤\n)", *types.toTypedArray())
                    }
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc(it)
                    }
                    node.fields { field ->
                        buildProperty(
                            field.name,
                            if (field.hasDefaultValue) field.type.dtoType else field.type.dtoType.nullable()
                        ) {
                            field.comments {
                                addKdoc(it)
                            }
                            mutable()

                            val literal = field.defaultValue
                            if (literal == null) {
                                initializer("null")
                            } else {
                                if (field.comments.isNotEmpty()) {
                                    addKdoc("  \n> ")
                                }
                                addKdoc("Default: $literal")

                                val args = mutableListOf<Any?>()
                                val format = literal.buildInitializer(field.type, args)
                                initializer(CodeBlock.of(format, *args.toTypedArray()))
                            }
                        }
                    }
                }
            }
        }
    }

    //******************************************************************************************************************
    //                                                GraphQL DTO
    //******************************************************************************************************************
    if (dto.graphql.enabled) {
        // GraphQL Request
        files += buildFile(dto.graphql.packageName, dto.graphql.requestName) {
            buildClass(dto.graphql.requestName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("query", STRING)
                    buildProperty("variables", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("operationName", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                }
            }
        }

        // GraphQL ErrorType
        files += buildFile(dto.graphql.packageName, dto.graphql.errorTypeName) {
            buildEnum(dto.graphql.errorTypeName) {
                addEnumConstant("InvalidSyntax")
                addEnumConstant("ValidationError")
                addEnumConstant("DataFetchingException")
                addEnumConstant("OperationNotSupported")
                addEnumConstant("ExecutionAborted")
            }
        }

        // GraphQL ErrorSourceLocation
        files += buildFile(dto.graphql.packageName, dto.graphql.errorSourceLocationName) {
            buildClass(dto.graphql.errorSourceLocationName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("line", INT)
                    buildProperty("column", INT)
                    buildProperty("sourceName", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                }
            }
        }

        // GraphQL Error
        files += buildFile(dto.graphql.packageName, dto.graphql.errorName) {
            buildClass(dto.graphql.errorName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("message", STRING)
                    buildProperty(
                        "locations", LIST.parameterizedBy(dto.graphql.errorSourceLocationClass).nullable()
                    ) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("errorType", dto.graphql.errorTypeClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty("path", LIST.parameterizedBy(ANY).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("extensions", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        val argErrors = "errors" to LIST.parameterizedBy(dto.graphql.errorClass).nullable()

        // GraphQL Exception
        files += buildFile(dto.graphql.packageName, dto.graphql.exceptionName) {
            buildClass(dto.graphql.exceptionName) {
                buildPrimaryConstructorProperties {
                    buildParameter("message", STRING)
                    buildProperty("request", dto.graphql.requestClass)
                    buildProperty(argErrors)
                }
                superclass(ClassName("kotlin", "RuntimeException"))
                addSuperclassConstructorParameter(
                    "message + (errors?.%M(\",\\n  \", \"\\n  \", \"\\n\")·{ it.toString() } ?: \"\")",
                    MemberName("kotlin.collections", "joinToString")
                )
            }
        }

        // GraphQL QueryResult
        files += buildFile(dto.graphql.packageName, dto.graphql.queryResultName) {
            buildClass(dto.graphql.queryResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("data", schema.query.dtoClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        // GraphQL MutationResult
        files += buildFile(dto.graphql.packageName, dto.graphql.mutationResultName) {
            buildClass(dto.graphql.mutationResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("data", schema.mutation.dtoClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        // GraphQL SubscriptionResult
        files += buildFile(dto.graphql.packageName, dto.graphql.subscriptionResultName) {
            buildClass(dto.graphql.subscriptionResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("data", schema.subscription.dtoClass.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                }
            }
        }

        // GraphQL ErrorResult
        files += buildFile(dto.graphql.packageName, dto.graphql.errorResultName) {
            buildClass(dto.graphql.errorResultName) {
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty(argErrors) {
                        jacksonIncludeNonEmpty()
                    }
                    customizeConstructor {
                        jacksonizeConstructor()
                    }
                }
            }
        }

        // GraphQL Messaging
        files += buildFile(dto.graphql.packageName, "messaging") {
            buildInterface(dto.graphql.messageName) {
                addKdoc(
                    "Message protocol description see [here]" +
                            "(https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)"
                )
                if (dto.jackson.enabled) {
                    buildAnnotation(JSON_TYPE_INFO) {
                        addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                        addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                        addMember("property = %S", "type")
                    }

                    buildAnnotation(JSON_SUB_TYPES) {
                        for (message in GqlMessage.values()) {
                            buildAnnotation(JSON_SUB_TYPES_TYPE) {
                                addMember("value = %T::class", dto.graphql.messageImplClass(message))
                                addMember("name = %S", message.type)
                            }
                        }
                    }
                }
            }

            buildInterface(dto.graphql.clientMessageName) {
                addKdoc(
                    "Message protocol description see [here]" +
                            "(https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)"
                )
                if (dto.jackson.enabled) {
                    buildAnnotation(JSON_TYPE_INFO) {
                        addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                        addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                        addMember("property = %S", "type")
                    }

                    buildAnnotation(JSON_SUB_TYPES) {
                        for (message in GqlMessage.values()) {
                            if (message.client) {
                                buildAnnotation(JSON_SUB_TYPES_TYPE) {
                                    addMember("value = %T::class", dto.graphql.messageImplClass(message))
                                    addMember("name = %S", message.type)
                                }
                            }
                        }
                    }
                }
            }

            buildInterface(dto.graphql.serverMessageName) {
                addKdoc(
                    "Message protocol description see [here]" +
                            "(https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)"
                )
                if (dto.jackson.enabled) {
                    buildAnnotation(JSON_TYPE_INFO) {
                        addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                        addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                        addMember("property = %S", "type")
                    }

                    buildAnnotation(JSON_SUB_TYPES) {
                        for (message in GqlMessage.values()) {
                            if (message.server) {
                                buildAnnotation(JSON_SUB_TYPES_TYPE) {
                                    addMember("value = %T::class", dto.graphql.messageImplClass(message))
                                    addMember("name = %S", message.type)
                                }
                            }
                        }
                    }
                }
            }

            GqlMessage.values().forEach { message ->
                buildClass(dto.graphql.messageImplName(message)) {
                    if (dto.jackson.enabled) {
                        buildAnnotation(JSON_TYPE_NAME) {
                            addMember("value = %S", message.type)
                        }

                        buildAnnotation(JSON_TYPE_INFO) {
                            addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                            addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                            addMember("property = %S", "type")
                            addMember("defaultImpl = %T::class", dto.graphql.messageImplClass(message))
                        }

                        buildAnnotation(JSON_INCLUDE) {
                            addMember("value = %T.Include.NON_ABSENT", JSON_INCLUDE)
                        }
                    }

                    addKdoc("See ${message.name} [here](https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md)")

                    addSuperinterface(dto.graphql.messageClass)
                    if (message.client) {
                        addSuperinterface(dto.graphql.clientMessageClass)
                    }
                    if (message.server) {
                        addSuperinterface(dto.graphql.serverMessageClass)
                    }

                    when (message) {
                        GqlMessage.GQL_CONNECTION_INIT -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("payload", MAP.parameterizedBy(STRING, ANY.nullable()).nullable())
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                        GqlMessage.GQL_START -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty("payload", dto.graphql.requestClass)
                            }
                        }
                        GqlMessage.GQL_STOP -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                        GqlMessage.GQL_CONNECTION_TERMINATE -> {
                            // Do nothing
                        }
                        GqlMessage.GQL_CONNECTION_ERROR -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("payload", ANY.nullable())
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                        GqlMessage.GQL_CONNECTION_ACK -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("payload", ANY.nullable())
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                        GqlMessage.GQL_DATA -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty("payload", dto.graphql.subscriptionResultClass)
                            }
                        }
                        GqlMessage.GQL_ERROR -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty("payload", dto.graphql.errorResultClass)
                            }
                        }
                        GqlMessage.GQL_COMPLETE -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                        GqlMessage.GQL_CONNECTION_KEEP_ALIVE -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }
    //******************************************************************************************************************

    files
}
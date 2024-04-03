package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.SEALED
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_IGNORE_PROPERTIES
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_INCLUDE
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_PROPERTY
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
                annotateSerializable()
                annotateSerialName(node.name)

                jacksonizeClass(node)
                if (node.fields.isNotEmpty()) {
                    addModifiers(KModifier.DATA)
                }
                node.comments {
                    addKdoc("%L", it)
                }
                node.implements {
                    addSuperinterface(it.dtoClass)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        val fieldType = if (dto.serialization.enabled) {
                            field.type.dtoTypeWithSerializer.nullable()
                        } else {
                            field.type.dtoType.nullable()
                        }
                        buildProperty(field.name, fieldType) {
                            field.comments {
                                addKdoc("%L", it)
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
                        ifFlowStatement("this.javaClass·!=·$EQUALS_ARG?.javaClass") {
                            "return·false"
                        }

                        addStatement("")
                        addStatement("$EQUALS_ARG·as·%T", node.dtoClass)

                        if (node.primaryKeysCount == 1) {
                            node.firstPrimaryKey().also {
                                addStatement("return·this.${it.name.escape()}·==·$EQUALS_ARG.${it.name.escape()}")
                            }
                        } else {
                            addStatement("")
                            node.primaryKeys {
                                ifFlowStatement("this.${it.name.escape()}·!=·$EQUALS_ARG.${it.name.escape()}") {
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
                                addStatement("return·this.${it.name.escape()}?.hashCode()·?:·0")
                            }
                        } else {
                            var first = true
                            node.primaryKeys {
                                if (first) {
                                    first = false
                                    addStatement("var·$HASH_CODE_RES·=·this.${it.name.escape()}?.hashCode()·?:·0")
                                } else {
                                    addStatement("$HASH_CODE_RES·=·31·*·$HASH_CODE_RES·+·(this.${it.name.escape()}?.hashCode()·?:·0)")
                                }
                            }
                            addStatement("return·$HASH_CODE_RES")
                        }
                    }
                }
            }

            // Build object DTO builder
            if (dto.builder.enabled && node.fields.isNotEmpty()) {
                // toBuilder function
                buildFunction(dto.builder.toBuilderFun) {
                    receiver(node.dtoClass)
                    returns(node.builderClass)

                    controlFlow(
                        "return·%T().%M",
                        node.builderClass,
                        MemberName("kotlin", "also")
                    ) {
                        node.fields { field ->
                            addStatement("it.${field.name.escape()}·=·this.${field.name.escape()}")
                        }
                    }
                }

                // toDto function
                buildFunction(dto.builder.toDtoFun) {
                    receiver(node.builderClass)
                    returns(node.dtoClass)

                    val arguments = node.fields.values.joinToString(",\n") { it.name.escape() }
                    addStatement("return·%T(\n⇥$arguments⇤\n)", node.dtoClass)
                }

                // Builder function
                buildFunction(node.dtoName) {
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement(
                        "return·%T().%M(block).%M()",
                        node.builderClass,
                        MemberName("kotlin", "apply"),
                        MemberName(dto.packageName, dto.builder.toDtoFun)
                    )
                }

                // Copy function
                buildFunction(dto.builder.copyFun) {
                    receiver(node.dtoClass)
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement(
                        "return·%M().%M(block).%M()",
                        MemberName(dto.packageName, dto.builder.toBuilderFun),
                        MemberName("kotlin", "apply"),
                        MemberName(dto.packageName, dto.builder.toDtoFun)
                    )
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc("%L", it)
                    }
                    node.fields { field ->
                        buildProperty(field.name, field.type.dtoType.nullable()) {
                            field.comments {
                                addKdoc("%L", it)
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
                    addKdoc("%L", it)
                }
                node.implements {
                    addSuperinterface(it.dtoClass)
                }
                node.fields { field ->
                    buildProperty(field.name, field.type.dtoType.nullable()) {
                        field.comments {
                            addKdoc("%L", it)
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
                    addKdoc("%L", it)
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
                    addKdoc("%L", it)
                }
                node.enumValues { enumValue ->
                    if (enumValue.name in FORBIDDEN_ENUM_NAMES) {
                        buildEnumConstant(enumValue.name.rename(node.enumValues.keys)) {
                            annotateSerialName(enumValue.name)
                            if (dto.jackson.enabled) {
                                buildAnnotation(JSON_PROPERTY) {
                                    addMember("%S", enumValue.name)
                                }
                            }
                            enumValue.comments {
                                addKdoc("%L", it)
                            }

                            if (enumValue.comments.isNotEmpty()) {
                                addKdoc("%L", "  \n> ")
                            }
                            addKdoc("%L", "see https://github.com/ermadmi78/kobby/issues/21")
                        }
                    } else {
                        buildEnumConstant(enumValue.name) {
                            enumValue.comments {
                                addKdoc("%L", it)
                            }
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
                annotateSerializable()

                jacksonizeClass(node)
                addModifiers(KModifier.DATA)
                node.comments {
                    addKdoc("%L", it)
                }
                buildPrimaryConstructorProperties {
                    node.fields { field ->
                        val fieldType = if (dto.serialization.enabled) {
                            field.type.dtoTypeWithSerializer
                        } else {
                            field.type.dtoType
                        }
                        val defaultValue: CodeBlock? = field.defaultValue?.let { literal ->
                            val args = mutableListOf<Any?>()
                            val format = literal.buildInitializer(field.type, args)
                            CodeBlock.of(format, *args.toTypedArray())
                        }
                        buildPropertyWithDefault(field.name, fieldType, defaultValue) {
                            field.comments {
                                addKdoc("%L", it)
                            }
                            field.defaultValue?.also { literal ->
                                if (field.comments.isNotEmpty()) {
                                    addKdoc("%L", "  \n> ")
                                }
                                addKdoc("%L", "Default: $literal")
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
                // toBuilder function
                buildFunction(dto.builder.toBuilderFun) {
                    receiver(node.dtoClass)
                    returns(node.builderClass)

                    controlFlow(
                        "return·%T().%M",
                        node.builderClass,
                        MemberName("kotlin", "also")
                    ) {
                        node.fields { field ->
                            addStatement("it.${field.name.escape()}·=·this.${field.name.escape()}")
                        }
                    }
                }

                // toInput function
                buildFunction(dto.builder.toInputFun) {
                    receiver(node.builderClass)
                    returns(node.dtoClass)

                    val types: MutableList<Any> = mutableListOf(node.dtoClass)
                    val arguments = node.fields.values.joinToString(",\n") {
                        if (it.type.nullable || it.hasDefaultValue) {
                            it.name.escape()
                        } else {
                            types += MemberName("kotlin", "error")
                            "${it.name.escape()}·?:·%M(\"${node.dtoName}:·'${it.name}'·must·not·be·null\")"
                        }
                    }
                    addStatement("return·%T(\n⇥$arguments⇤\n)", *types.toTypedArray())
                }

                // Builder function
                buildFunction(node.dtoName) {
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement(
                        "return·%T().%M(block).%M()",
                        node.builderClass,
                        MemberName("kotlin", "apply"),
                        MemberName(dto.packageName, dto.builder.toInputFun)
                    )
                }

                // Copy function
                buildFunction(dto.builder.copyFun) {
                    receiver(node.dtoClass)
                    addParameter("block", node.builderLambda)
                    returns(node.dtoClass)

                    addStatement(
                        "return·%M().%M(block).%M()",
                        MemberName(dto.packageName, dto.builder.toBuilderFun),
                        MemberName("kotlin", "apply"),
                        MemberName(dto.packageName, dto.builder.toInputFun)
                    )
                }

                // Builder class
                buildClass(node.builderName) {
                    addAnnotation(context.dslClass)
                    node.comments {
                        addKdoc("%L", it)
                    }
                    node.fields { field ->
                        buildProperty(
                            field.name,
                            if (field.hasDefaultValue) field.type.dtoType else field.type.dtoType.nullable()
                        ) {
                            field.comments {
                                addKdoc("%L", it)
                            }
                            mutable()

                            val literal = field.defaultValue
                            if (literal == null) {
                                initializer("null")
                            } else {
                                if (field.comments.isNotEmpty()) {
                                    addKdoc("%L", "  \n> ")
                                }
                                addKdoc("%L", "Default: $literal")

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
                annotateSerializable()
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("query", STRING)

                    if (dto.serialization.enabled) {
                        buildProperty("variables", SerializationJson.JSON_OBJECT.nullable())
                    } else {
                        buildProperty("variables", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                            jacksonIncludeNonEmpty()
                        }
                    }

                    buildProperty("operationName", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }
                }
            }
        }

        // GraphQL ErrorSourceLocation
        files += buildFile(dto.graphql.packageName, dto.graphql.errorSourceLocationName) {
            buildClass(dto.graphql.errorSourceLocationName) {
                annotateSerializable()
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
                annotateSerializable()
                addModifiers(KModifier.DATA)
                buildPrimaryConstructorProperties {
                    buildProperty("message", STRING)
                    buildProperty(
                        "locations", LIST.parameterizedBy(dto.graphql.errorSourceLocationClass).nullable()
                    ) {
                        jacksonIncludeNonEmpty()
                    }
                    buildProperty("errorType", STRING.nullable()) {
                        jacksonIncludeNonAbsent()
                    }

                    if (dto.serialization.enabled) {
                        buildProperty("path", SerializationJson.JSON_ARRAY.nullable())
                        buildProperty("extensions", SerializationJson.JSON_OBJECT.nullable())
                    } else {
                        buildProperty("path", LIST.parameterizedBy(ANY).nullable()) {
                            jacksonIncludeNonEmpty()
                        }
                        buildProperty("extensions", MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                            jacksonIncludeNonEmpty()
                        }
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
                annotateSerializable()
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
                annotateSerializable()
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
                annotateSerializable()
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

        // GraphQL Messaging
        files += buildFile(dto.graphql.packageName, "messaging") {
            buildClass(dto.graphql.clientMessageName) {
                addModifiers(SEALED)
                addKdoc(
                    "%L",
                    "Message protocol description see [here]" +
                            "(https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)"
                )

                annotateSerializable()
                enableExperimentalSerializationApi()
                annotateJsonClassDiscriminator("type")

                if (dto.jackson.enabled) {
                    buildAnnotation(JSON_TYPE_INFO) {
                        addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                        addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                        addMember("property = %S", "type")
                    }

                    buildAnnotation(JSON_SUB_TYPES) {
                        for (message in WsMessage.values()) {
                            if (message.client) {
                                buildAnnotation(JSON_SUB_TYPES_TYPE) {
                                    addMember("value = %T::class", dto.graphql.messageImplClass(message))
                                    addMember("name = %S", message.type)
                                }
                            }
                        }
                    }

                    buildAnnotation(JSON_IGNORE_PROPERTIES) {
                        addMember("ignoreUnknown = true")
                    }
                }
            }

            buildClass(dto.graphql.serverMessageName) {
                addModifiers(SEALED)
                addKdoc(
                    "%L",
                    "Message protocol description see [here]" +
                            "(https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)"
                )

                annotateSerializable()
                enableExperimentalSerializationApi()
                annotateJsonClassDiscriminator("type")

                if (dto.jackson.enabled) {
                    buildAnnotation(JSON_TYPE_INFO) {
                        addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
                        addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
                        addMember("property = %S", "type")
                    }

                    buildAnnotation(JSON_SUB_TYPES) {
                        for (message in WsMessage.values()) {
                            if (!message.client) {
                                buildAnnotation(JSON_SUB_TYPES_TYPE) {
                                    addMember("value = %T::class", dto.graphql.messageImplClass(message))
                                    addMember("name = %S", message.type)
                                }
                            }
                        }
                    }

                    buildAnnotation(JSON_IGNORE_PROPERTIES) {
                        addMember("ignoreUnknown = true")
                    }
                }
            }

            WsMessage.values().forEach { message ->
                buildClass(dto.graphql.messageImplName(message)) {
                    annotateSerializable()
                    annotateSerialName(message.type)

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

                    addKdoc(
                        "%L",
                        "See ${message.docName} [here](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)"
                    )

                    if (message.client) {
                        superclass(dto.graphql.clientMessageClass)
                    } else {
                        superclass(dto.graphql.serverMessageClass)
                    }

                    when (message) {
                        WsMessage.WS_CLIENT_MESSAGE_CONNECTION_INIT -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_OBJECT.nullable())
                                } else {
                                    buildProperty("payload", MAP.parameterizedBy(STRING, ANY.nullable()).nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_CONNECTION_ACK -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_ELEMENT.nullable())
                                } else {
                                    buildProperty("payload", ANY.nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_CLIENT_MESSAGE_PING -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_ELEMENT.nullable())
                                } else {
                                    buildProperty("payload", ANY.nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_CLIENT_MESSAGE_PONG -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_ELEMENT.nullable())
                                } else {
                                    buildProperty("payload", ANY.nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_PING -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_ELEMENT.nullable())
                                } else {
                                    buildProperty("payload", ANY.nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_PONG -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                if (dto.serialization.enabled) {
                                    buildProperty("payload", SerializationJson.JSON_ELEMENT.nullable())
                                } else {
                                    buildProperty("payload", ANY.nullable())
                                    customizeConstructor {
                                        jacksonizeConstructor()
                                    }
                                }
                            }
                        }

                        WsMessage.WS_CLIENT_MESSAGE_SUBSCRIBE -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty("payload", dto.graphql.requestClass)
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_NEXT -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty("payload", dto.graphql.subscriptionResultClass)
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_ERROR -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                buildProperty(
                                    "payload",
                                    LIST.parameterizedBy(dto.graphql.errorClass).nullable()
                                ) {
                                    jacksonIncludeNonEmpty()
                                }
                            }
                        }

                        WsMessage.WS_CLIENT_MESSAGE_COMPLETE -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }

                        WsMessage.WS_SERVER_MESSAGE_COMPLETE -> {
                            addModifiers(KModifier.DATA)
                            buildPrimaryConstructorProperties {
                                buildProperty("id", STRING)
                                customizeConstructor {
                                    jacksonizeConstructor()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    //******************************************************************************************************************

    files
}
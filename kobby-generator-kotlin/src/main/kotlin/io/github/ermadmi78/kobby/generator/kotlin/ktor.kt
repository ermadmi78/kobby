package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.generator.kotlin.GqlMessage.*
import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 20.06.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateKtorAdapter(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()
    val ktor = adapter.ktor

    if (ktor.simpleEnabled) {
        files += buildFile(ktor.packageName, ktor.simpleName) {
            buildClass(ktor.simpleName) {
                addModifiers(OPEN)
                addSuperinterface(context.adapterClass)
                buildPrimaryConstructorProperties {
                    buildProperty(
                        ktor.simplePropertyClient,
                        ClassName("io.ktor.client", "HttpClient")
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildProperty(ktor.simplePropertyUrl, STRING.nullable()) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.simplePropertyHeaders,
                        MAP.parameterizedBy(STRING, STRING),
                        CodeBlock.of("%T()", Kotlin.mapOf)
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.simplePropertyListener,
                        dto.adapterListenerLambda,
                        CodeBlock.of("{}")
                    ) {
                        addModifiers(PROTECTED)
                    }
                }

                buildSimpleQueryOrMutationFun(
                    schema.query,
                    layout,
                    context.adapterFunExecuteQuery,
                    "query",
                    dto.graphql.queryResultClass
                )

                buildSimpleQueryOrMutationFun(
                    schema.mutation,
                    layout,
                    context.adapterFunExecuteMutation,
                    "mutation",
                    dto.graphql.mutationResultClass
                )
            }
        }
    }

    if (ktor.compositeEnabled) {
        files += buildFile(ktor.packageName, ktor.compositeName) {
            val readResponseText = MemberName("io.ktor.client.statement", "readText")
            addAliasedImport(readResponseText, "readResponseText")

            val readFrameText = MemberName("io.ktor.http.cio.websocket", "readText")
            addAliasedImport(readFrameText, "readFrameText")

            buildClass(ktor.compositeName) {
                addModifiers(OPEN)
                addSuperinterface(context.adapterClass)
                buildPrimaryConstructorProperties {
                    buildProperty(
                        ktor.compositePropertyClient,
                        ClassName("io.ktor.client", "HttpClient")
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildProperty(
                        ktor.compositePropertyHttpUrl,
                        STRING
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildProperty(
                        ktor.compositePropertyWebSocketUrl,
                        STRING
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildProperty(
                        ktor.compositePropertyMapper,
                        context.mapperClass
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertyRequestHeaders,
                        MAP.parameterizedBy(STRING, STRING),
                        CodeBlock.of("%T()", Kotlin.mapOf)
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildProperty(
                        ktor.compositePropertySubscriptionPayload,
                        MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertySubscriptionReceiveTimeoutMillis,
                        LONG.nullable(),
                        ktor.receiveTimeoutMillis?.let { CodeBlock.of("%L", it) }
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertyHttpTokenHeader,
                        STRING,
                        CodeBlock.of("%S", "Authorization")
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertyWebSocketTokenHeader,
                        STRING,
                        CodeBlock.of("%S", "authToken")
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertyIdGenerator,
                        LambdaTypeName.get(null, listOf(), STRING),
                        CodeBlock.of(
                            "{ %T.nextLong().toString() }",
                            ClassName("kotlin.random", "Random")
                        )
                    ) {
                        addModifiers(PROTECTED)
                    }
                    buildPropertyWithDefault(
                        ktor.compositePropertyListener,
                        dto.adapterListenerLambda,
                        CodeBlock.of("{}")
                    ) {
                        addModifiers(PROTECTED)
                    }
                }

                buildCompositeQueryOrMutationFun(
                    schema.query,
                    layout,
                    context.adapterFunExecuteQuery,
                    "query",
                    dto.graphql.queryResultClass,
                    readResponseText
                )

                buildCompositeQueryOrMutationFun(
                    schema.mutation,
                    layout,
                    context.adapterFunExecuteMutation,
                    "mutation",
                    dto.graphql.mutationResultClass,
                    readResponseText
                )

                buildExecuteSubscriptionFun(schema, layout)

                buildExecuteSubscriptionImplFun(schema, layout)

                buildFunction(ktor.compositeFunSendMessage) {
                    addModifiers(PROTECTED, OPEN, SUSPEND)
                    receiver(ClassName("io.ktor.http.cio.websocket", "WebSocketSession"))
                    buildParameter(ktor.compositePropertyMessage, dto.graphql.clientMessageClass)

                    addStatement(
                        "val·${ktor.compositeValContent}·=·${ktor.compositePropertyMapper}" +
                                ".${context.mapperFunSerialize}(${ktor.compositePropertyMessage})"
                    )
                    addStatement(
                        "%T(${ktor.compositeValContent})",
                        ClassName("io.ktor.http.cio.websocket", "send")
                    )
                }

                buildFunction(ktor.compositeFunReceiveMessage) {
                    addModifiers(PROTECTED, OPEN, SUSPEND)
                    receiver(ClassName("io.ktor.http.cio.websocket", "WebSocketSession"))
                    returns(dto.graphql.serverMessageClass)

                    controlFlow(
                        "val·${ktor.compositeValMessage}·=·" +
                                "if·(${ktor.compositePropertySubscriptionReceiveTimeoutMillis}·==·null)"
                    ) {
                        addStatement("incoming.receive()")
                    }
                    controlFlow("else") {
                        controlFlow(
                            "%T(${ktor.compositePropertySubscriptionReceiveTimeoutMillis})",
                            ClassName("kotlinx.coroutines", "withTimeout")
                        ) {
                            addStatement("incoming.receive()")
                        }
                    }

                    addStatement("")
                    addStatement(
                        "val·${ktor.compositeValContent}·=·(${ktor.compositeValMessage}·as·%T).%M()",
                        ClassName("io.ktor.http.cio.websocket", "Frame", "Text"),
                        readFrameText
                    )
                    statement(dto.graphql.serverMessageClass) {
                        "return·${ktor.compositePropertyMapper}" +
                                ".${context.mapperFunDeserialize}(${ktor.compositeValContent}, %T::class)"
                    }
                }
            }
        }
    }

    files
}

private fun TypeSpecBuilder.buildSimpleQueryOrMutationFun(
    node: KobbyNode,
    layout: KotlinLayout,
    name: String,
    operation: String,
    graphQlResultClass: TypeName
) = with(layout) {
    buildFunction(name) {
        addModifiers(OVERRIDE)
        addModifiers(SUSPEND)
        buildParameter(context.adapterArgQuery)
        buildParameter(context.adapterArgVariables)
        returns(node.dtoClass)

        val ktor = adapter.ktor
        statement(dto.graphql.requestClass) {
            "val·${ktor.simpleValRequest}·=·%T(${context.adapterArgQuery.first}, ${context.adapterArgVariables.first})"
        }
        addStatement("${ktor.simplePropertyListener}(${ktor.simpleValRequest})")
        addStatement("")

        controlFlow(
            "val·${ktor.simpleValResult}·=·${ktor.simplePropertyClient}.%T<%T>",
            ClassName("io.ktor.client.request", "post"),
            graphQlResultClass
        ) {
            addStatement("body = ${ktor.simpleValRequest}")
            addStatement(
                "%T(%T.Application.Json)",
                ClassName("io.ktor.http", "contentType"),
                ClassName("io.ktor.http", "ContentType")
            )
            controlFlow("this@%T.url?.%T", ktor.simpleClass, Kotlin.also) {
                addStatement("%T(it)", ClassName("io.ktor.client.request", "url"))
            }
            controlFlow("for (element·in·this@%T.${ktor.simplePropertyHeaders})", ktor.simpleClass) {
                addStatement("headers[element.key]·=·element.value")
            }
        }
        addStatement("")

        controlFlow(
            "${ktor.simpleValResult}.errors?.%T·{ it.%T() }?.%T",
            Kotlin.takeIf, Kotlin.isNotEmpty, Kotlin.let
        ) {
            addStatement(
                "throw·%T(%S, ${ktor.simpleValRequest}, it)",
                dto.graphql.exceptionClass,
                "GraphQL $operation failed"
            )
        }
        addStatement(
            "return·${ktor.simpleValResult}.data ?: throw·%T(\n⇥%S,\n${ktor.simpleValRequest}⇤\n)",
            dto.graphql.exceptionClass,
            "GraphQL $operation completes successfully but returns no data"
        )
    }
}

private fun TypeSpecBuilder.buildCompositeQueryOrMutationFun(
    node: KobbyNode,
    layout: KotlinLayout,
    name: String,
    operation: String,
    graphQlResultClass: TypeName,
    readResponseText: MemberName
) = with(layout) {
    buildFunction(name) {
        addModifiers(OVERRIDE)
        addModifiers(SUSPEND)
        buildParameter(context.adapterArgQuery)
        buildParameter(context.adapterArgVariables)
        returns(node.dtoClass)

        val ktor = adapter.ktor
        statement(dto.graphql.requestClass) {
            "val·${ktor.compositeValRequest}·=·" +
                    "%T(${context.adapterArgQuery.first}, ${context.adapterArgVariables.first})"
        }
        addStatement("${ktor.compositePropertyListener}(${ktor.compositeValRequest})")
        addStatement("")

        controlFlow(
            "val·${ktor.compositeValContent}·=·${ktor.compositePropertyClient}.%T<%T>",
            ClassName("io.ktor.client.request", "post"),
            ClassName("io.ktor.client.statement", "HttpResponse")
        ) {
            statement {
                "body = ${ktor.compositePropertyMapper}.${context.mapperFunSerialize}(${ktor.compositeValRequest})"
            }
            addStatement(
                "%T(%T.Application.Json)",
                ClassName("io.ktor.http", "contentType"),
                ClassName("io.ktor.http", "ContentType")
            )
            statement(ClassName("io.ktor.client.request", "url")) {
                "%T(${ktor.compositePropertyHttpUrl})"
            }
            controlFlow("for (element·in·${ktor.compositePropertyRequestHeaders})") {
                addStatement("headers[element.key]·=·element.value")
            }
        }
        addStatement(".%M()", readResponseText)
        addStatement("")

        statement(graphQlResultClass) {
            "val·${ktor.compositeValResult}·=·" +
                    "${ktor.compositePropertyMapper}.${context.mapperFunDeserialize}(" +
                    "${ktor.compositeValContent}, %T::class)"
        }
        addStatement("")

        controlFlow(
            "${ktor.compositeValResult}.errors?.%T·{ it.%T() }?.%T",
            Kotlin.takeIf, Kotlin.isNotEmpty, Kotlin.let
        ) {
            addStatement(
                "throw·%T(%S, ${ktor.compositeValRequest}, it)",
                dto.graphql.exceptionClass,
                "GraphQL $operation failed"
            )
        }
        addStatement(
            "return·${ktor.compositeValResult}.data ?: throw·%T(\n⇥%S,\n${ktor.compositeValRequest}⇤\n)",
            dto.graphql.exceptionClass,
            "GraphQL $operation completes successfully but returns no data"
        )
    }
}

private fun TypeSpecBuilder.buildExecuteSubscriptionFun(schema: KobbySchema, layout: KotlinLayout) = with(layout) {
    buildFunction(context.adapterFunExecuteSubscription) {
        addModifiers(OVERRIDE)
        addModifiers(SUSPEND)
        buildParameter(context.adapterArgQuery)
        buildParameter(context.adapterArgVariables)
        buildParameter(context.adapterArgBlock, schema.receiverSubscriptionDtoLambda)
        returns(UNIT)

        val ktor = adapter.ktor

        controlFlow(
            "${ktor.compositePropertyClient}.%T(\n⇥${ktor.compositePropertyWebSocketUrl},\n" +
                    "request = {\n⇥" +
                    ktor.compositePropertyRequestHeaders +
                    ".%T·{·(key,·value)·->\n⇥headers[key] = value⇤\n}" +
                    "⇤\n}⇤\n)",
            ClassName("io.ktor.client.features.websocket", "ws"),
            Kotlin.forEach
        ) {
            statement(MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                "var·${ktor.compositeValInitPayload}:·%T·=·${ktor.compositePropertySubscriptionPayload}"
            }
            controlFlow(
                "if (${ktor.compositePropertyHttpTokenHeader} " +
                        "%M ${ktor.compositePropertyRequestHeaders})",
                Kotlin.contains
            ) {
                controlFlow("if (${ktor.compositeValInitPayload}·==·null)") {
                    addStatement("${ktor.compositeValInitPayload} = %T()", Kotlin.mapOf)
                }
                addStatement("")

                controlFlow(
                    "if (${ktor.compositePropertyWebSocketTokenHeader} " +
                            "%M ${ktor.compositeValInitPayload})",
                    Kotlin.notContains
                ) {
                    addStatement(
                        "@%T(%S)",
                        ClassName("kotlin", "Suppress"),
                        "SuspiciousCollectionReassignment"
                    )
                    statement(Kotlin.plus, Kotlin.mapOf, Kotlin.to) {
                        "${ktor.compositeValInitPayload} %M= " +
                                "%T(${ktor.compositePropertyWebSocketTokenHeader} " +
                                "%T ${ktor.compositePropertyRequestHeaders}" +
                                "[${ktor.compositePropertyHttpTokenHeader}])"
                    }
                }
            }
            addStatement("")

            statement(dto.graphql.requestClass) {
                "val·${ktor.compositeValRequest}·=·" +
                        "%T(${context.adapterArgQuery.first}, ${context.adapterArgVariables.first})"
            }
            addStatement("${ktor.compositePropertyListener}(${ktor.compositeValRequest})")
            addStatement("")

            addStatement(
                "${ktor.compositeFunExecuteSubscriptionImpl}(${ktor.compositeValInitPayload}, " +
                        "${ktor.compositeValRequest}, ${context.adapterArgBlock})"
            )
        }
    }
}

private fun TypeSpecBuilder.buildExecuteSubscriptionImplFun(schema: KobbySchema, layout: KotlinLayout) = with(layout) {
    val ktor = adapter.ktor
    val graphql = dto.graphql

    val message: (GqlMessage) -> ClassName = {
        graphql.messageImplClass(it)
    }

    val initPayload = ktor.compositeValInitPayload
    val request = ktor.compositeValRequest
    val block = context.adapterArgBlock

    val reply = ktor.compositeValReply
    val receiveMessage = ktor.compositeFunReceiveMessage
    val receiver = ktor.compositeValReceiver
    val subscriptionId = ktor.compositeValSubscriptionId
    val result = ktor.compositeValResult

    buildFunction(ktor.compositeFunExecuteSubscriptionImpl) {
        addModifiers(PROTECTED, OPEN, SUSPEND)
        receiver(ClassName("io.ktor.http.cio.websocket", "WebSocketSession"))
        buildParameter(initPayload, MAP.parameterizedBy(STRING, ANY.nullable()).nullable())
        buildParameter(request, graphql.requestClass)
        buildParameter(block, schema.receiverSubscriptionDtoLambda)
        returns(UNIT)

        buildSendMessage(layout, GQL_CONNECTION_INIT) {
            initPayload
        }
        controlFlow("try") {
            controlFlow("while·(true)") {
                controlFlow("when·(val·$reply·=·$receiveMessage())") {
                    controlFlow("is·%T·->", message(GQL_CONNECTION_KEEP_ALIVE)) {
                        addStatement("continue")
                    }
                    controlFlow("is·%T·->", message(GQL_CONNECTION_ACK)) {
                        addStatement("break")
                    }
                    controlFlow("is·%T·->", message(GQL_CONNECTION_ERROR)) {
                        addStatement(
                            "throw·%T(%P, $request)",
                            graphql.exceptionClass,
                            "Connection error: \${$reply.payload}"
                        )
                    }
                    controlFlow("else·->") {
                        addStatement(
                            "throw·%T(%P, $request)",
                            graphql.exceptionClass,
                            "Invalid protocol - unexpected reply: \$$reply"
                        )
                    }
                }
            }

            addStatement("")
            addStatement("val·$subscriptionId·=·${ktor.compositePropertyIdGenerator}()")
            buildSendMessage(layout, GQL_START) {
                "$subscriptionId, $request"
            }
            addStatement("")

            controlFlow("val·$receiver·=·%T<%T>", context.receiverClass, schema.subscription.dtoClass) {
                controlFlow("while·(true)") {
                    controlFlow("when·(val·$reply·=·$receiveMessage())") {
                        controlFlow("is·%T·->", message(GQL_DATA)) {
                            addStatement("%T($reply.id·==·$subscriptionId)", Kotlin.require)
                            addStatement("")

                            addStatement("val·$result·=·$reply.payload")
                            controlFlow(
                                "$result.errors?.%T·{ it.%T() }?.%T",
                                Kotlin.takeIf, Kotlin.isNotEmpty, Kotlin.let
                            ) {
                                addStatement(
                                    "throw·%T(%S, $request, it)",
                                    graphql.exceptionClass,
                                    "GraphQL subscription failed"
                                )
                            }
                            addStatement(
                                "return@%T·$result.data ?: throw·%T(\n⇥%S,\n$request⇤\n)",
                                context.receiverClass,
                                graphql.exceptionClass,
                                "GraphQL subscription completes successfully but returns no data"
                            )
                        }

                        controlFlow("is·%T·->", message(GQL_ERROR)) {
                            addStatement("%T($reply.id·==·$subscriptionId)", Kotlin.require)
                            addStatement(
                                "throw·%T(%S, $request, $reply.payload.errors)",
                                graphql.exceptionClass,
                                "Subscription failed"
                            )
                        }

                        controlFlow("is·%T·->", message(GQL_COMPLETE)) {
                            addStatement("%T($reply.id·==·$subscriptionId)", Kotlin.require)
                            addStatement(
                                "throw·%T(%S)",
                                ClassName(
                                    "kotlin.coroutines.cancellation",
                                    "CancellationException"
                                ),
                                "Subscription finished"
                            )
                        }

                        controlFlow("is·%T·->", message(GQL_CONNECTION_KEEP_ALIVE)) {
                            addStatement("continue")
                        }

                        controlFlow("else·->") {
                            addStatement(
                                "throw·%T(%P, $request)",
                                graphql.exceptionClass,
                                "Invalid protocol - unexpected reply: \$$reply"
                            )
                        }
                    }
                }

                addStatement("")
                addStatement(
                    "@%T(%S)",
                    ClassName("kotlin", "Suppress"),
                    "UNREACHABLE_CODE"
                )
                addStatement("%T(%S)", Kotlin.error, "Invalid algorithm")
            }
            addStatement("")

            controlFlow("try") {
                addStatement("$block.invoke($receiver)")
            }
            controlFlow("finally") {
                buildSendMessage(layout, GQL_STOP) {
                    subscriptionId
                }
            }
        }
        controlFlow("finally") {
            buildSendMessage(layout, GQL_CONNECTION_TERMINATE)
        }
    }
}

private fun FunSpecBuilder.buildSendMessage(
    layout: KotlinLayout,
    message: GqlMessage,
    vararg args: Any,
    block: () -> String = { "" }
) = statement(layout.dto.graphql.messageImplClass(message), *args) {
    "${layout.adapter.ktor.compositeFunSendMessage}(%T(${block()}))"
}

private object Kotlin {
    val also = ClassName("kotlin", "also")
    val takeIf = ClassName("kotlin", "takeIf")
    val let = ClassName("kotlin", "let")
    val to = ClassName("kotlin", "to")
    val error = ClassName("kotlin", "error")
    val require = ClassName("kotlin", "require")

    val isNotEmpty = ClassName("kotlin.collections", "isNotEmpty")
    val forEach = ClassName("kotlin.collections", "forEach")
    val mapOf = ClassName("kotlin.collections", "mapOf")

    val contains = MemberName("kotlin.collections", KOperator.CONTAINS)
    val notContains = MemberName("kotlin.collections", KOperator.NOT_CONTAINS)
    val plus = MemberName("kotlin.collections", KOperator.PLUS)
}

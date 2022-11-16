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
                    // protected val client: HttpClient
                    buildProperty(
                        ktor.simplePropertyClient,
                        ClassName("io.ktor.client", "HttpClient")
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val url: String? = null
                    buildProperty(ktor.simplePropertyUrl, STRING.nullable()) {
                        addModifiers(PROTECTED)
                    }

                    // protected val headers ...
                    if (ktor.dynamicHttpHeaders) {
                        // protected val headers: suspend () -> Map<String, String> = { mapOf<String, String>() }
                        buildPropertyWithDefault(
                            ktor.simplePropertyHeaders,
                            LambdaTypeName
                                .get(returnType = MAP.parameterizedBy(STRING, STRING))
                                .copy(suspending = true),
                            CodeBlock.of("{·%M<%T,·%T>()·}", Kotlin.mapOf, STRING, STRING)
                        ) {
                            addModifiers(PROTECTED)
                        }
                    } else {
                        // protected val headers: Map<String, String> = mapOf()
                        buildPropertyWithDefault(
                            ktor.simplePropertyHeaders,
                            MAP.parameterizedBy(STRING, STRING),
                            CodeBlock.of("%M()", Kotlin.mapOf)
                        ) {
                            addModifiers(PROTECTED)
                        }
                    }

                    // protected val listener: (CinemaRequest) -> Unit = {}
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
            buildClass(ktor.compositeName) {
                addModifiers(OPEN)
                addSuperinterface(context.adapterClass)
                buildPrimaryConstructorProperties {
                    // protected val client: HttpClient
                    buildProperty(
                        ktor.compositePropertyClient,
                        ClassName("io.ktor.client", "HttpClient")
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val httpUrl: String
                    buildProperty(
                        ktor.compositePropertyHttpUrl,
                        STRING
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val webSocketUrl: String
                    buildProperty(
                        ktor.compositePropertyWebSocketUrl,
                        STRING
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val mapper: XXXMapper
                    buildProperty(
                        ktor.compositePropertyMapper,
                        context.mapperClass
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val requestHeaders ...
                    if (ktor.dynamicHttpHeaders) {
                        // protected val requestHeaders: suspend () -> Map<String, String> = { mapOf<String, String>() }
                        buildPropertyWithDefault(
                            ktor.compositePropertyRequestHeaders,
                            LambdaTypeName
                                .get(returnType = MAP.parameterizedBy(STRING, STRING))
                                .copy(suspending = true),
                            CodeBlock.of("{·%M<%T,·%T>()·}", Kotlin.mapOf, STRING, STRING)
                        ) {
                            addModifiers(PROTECTED)
                        }
                    } else {
                        // protected val requestHeaders: Map<String, String> = mapOf()
                        buildPropertyWithDefault(
                            ktor.compositePropertyRequestHeaders,
                            MAP.parameterizedBy(STRING, STRING),
                            CodeBlock.of("%M()", Kotlin.mapOf)
                        ) {
                            addModifiers(PROTECTED)
                        }
                    }

                    // protected val subscriptionPayload ...
                    if (ktor.dynamicHttpHeaders) {
                        // protected val subscriptionPayload: suspend () -> Map<String, Any?>? = { null }
                        buildPropertyWithDefault(
                            ktor.compositePropertySubscriptionPayload,
                            LambdaTypeName
                                .get(returnType = MAP.parameterizedBy(STRING, ANY.nullable()).nullable())
                                .copy(suspending = true),
                            CodeBlock.of("{·null·}")
                        ) {
                            addModifiers(PROTECTED)
                        }
                    } else {
                        // protected val subscriptionPayload: Map<String, Any?>? = null
                        buildProperty(
                            ktor.compositePropertySubscriptionPayload,
                            MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
                        ) {
                            addModifiers(PROTECTED)
                        }
                    }

                    // protected val subscriptionReceiveTimeoutMillis: Long? = null
                    buildPropertyWithDefault(
                        ktor.compositePropertySubscriptionReceiveTimeoutMillis,
                        LONG.nullable(),
                        ktor.receiveTimeoutMillis?.let { CodeBlock.of("%L", it) }
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val httpAuthorizationTokenHeader: String = "Authorization"
                    buildPropertyWithDefault(
                        ktor.compositePropertyHttpTokenHeader,
                        STRING,
                        CodeBlock.of("%S", "Authorization")
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val webSocketAuthorizationTokenHeader: String = "authToken"
                    buildPropertyWithDefault(
                        ktor.compositePropertyWebSocketTokenHeader,
                        STRING,
                        CodeBlock.of("%S", "authToken")
                    ) {
                        addModifiers(PROTECTED)
                    }

                    // protected val idGenerator: () -> String = { Random.nextLong().toString() }
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

                    // protected val listener: (CinemaRequest) -> Unit = {}
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
                    dto.graphql.queryResultClass
                )

                buildCompositeQueryOrMutationFun(
                    schema.mutation,
                    layout,
                    context.adapterFunExecuteMutation,
                    "mutation",
                    dto.graphql.mutationResultClass
                )

                buildExecuteSubscriptionFun(schema, layout)

                buildExecuteSubscriptionImplFun(schema, layout)

                buildFunction(ktor.compositeFunSendMessage) {
                    addModifiers(PROTECTED, OPEN, SUSPEND)
                    receiver(ClassName("io.ktor.websocket", "WebSocketSession"))
                    buildParameter(ktor.compositePropertyMessage, dto.graphql.clientMessageClass)

                    addStatement(
                        "val·${ktor.compositeValContent}·=·${ktor.compositePropertyMapper}" +
                                ".${context.mapperFunSerialize}(${ktor.compositePropertyMessage})"
                    )
                    addStatement(
                        "%M(${ktor.compositeValContent})",
                        MemberName("io.ktor.websocket", "send")
                    )
                }

                buildFunction(ktor.compositeFunReceiveMessage) {
                    addModifiers(PROTECTED, OPEN, SUSPEND)
                    receiver(ClassName("io.ktor.websocket", "WebSocketSession"))
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
                        ClassName("io.ktor.websocket", "Frame", "Text"),
                        MemberName("io.ktor.websocket", "readText")
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

        addStatement(
            "val·${ktor.simpleValHeaders}:·%T·=·${ktor.simplePropertyHeaders}"
                    + (if (ktor.dynamicHttpHeaders) "()" else ""),
            MAP.parameterizedBy(STRING, STRING)
        )
        controlFlow(
            "val·${ktor.simpleValResult}·=·${ktor.simplePropertyClient}.%M",
            MemberName("io.ktor.client.request", "post")
        ) {
            addStatement(
                "%M<%T>(${ktor.simpleValRequest})",
                MemberName("io.ktor.client.request", "setBody"),
                dto.graphql.requestClass
            )
            addStatement(
                "%M(%T.Application.Json)",
                MemberName("io.ktor.http", "contentType"),
                ClassName("io.ktor.http", "ContentType")
            )
            controlFlow("this@%T.url?.%M", ktor.simpleClass, Kotlin.also) {
                addStatement("%M(it)", MemberName("io.ktor.client.request", "url"))
            }
            controlFlow("for·(element·in·${ktor.simpleValHeaders})") {
                addStatement("headers[element.key]·=·element.value")
            }
        }
        addStatement(
            ".%M<%T>()",
            MemberName("io.ktor.client.call", "body"),
            graphQlResultClass
        )
        addStatement("")

        controlFlow(
            "${ktor.simpleValResult}.errors?.%M·{ it.%M() }?.%M",
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
            "val·${ktor.compositeValRequest}·=·" +
                    "%T(${context.adapterArgQuery.first}, ${context.adapterArgVariables.first})"
        }
        addStatement("${ktor.compositePropertyListener}(${ktor.compositeValRequest})")
        addStatement("")

        addStatement(
            "val·${ktor.compositeValHeaders}:·%T·=·${ktor.compositePropertyRequestHeaders}"
                    + (if (ktor.dynamicHttpHeaders) "()" else ""),
            MAP.parameterizedBy(STRING, STRING)
        )
        controlFlow(
            "val·${ktor.compositeValContent}·=·${ktor.compositePropertyClient}.%M",
            MemberName("io.ktor.client.request", "post")
        ) {
            addStatement(
                "%M<%T>(${ktor.compositePropertyMapper}.${context.mapperFunSerialize}(${ktor.compositeValRequest}))",
                MemberName("io.ktor.client.request", "setBody"),
                STRING
            )
            addStatement(
                "%M(%T.Application.Json)",
                MemberName("io.ktor.http", "contentType"),
                ClassName("io.ktor.http", "ContentType")
            )
            statement(MemberName("io.ktor.client.request", "url")) {
                "%M(${ktor.compositePropertyHttpUrl})"
            }
            controlFlow("for·(element·in·${ktor.compositeValHeaders})") {
                addStatement("headers[element.key]·=·element.value")
            }
        }
        addStatement(".%M()", MemberName("io.ktor.client.statement", "bodyAsText"))
        addStatement("")

        statement(graphQlResultClass) {
            "val·${ktor.compositeValResult}·=·" +
                    "${ktor.compositePropertyMapper}.${context.mapperFunDeserialize}(" +
                    "${ktor.compositeValContent}, %T::class)"
        }
        addStatement("")

        controlFlow(
            "${ktor.compositeValResult}.errors?.%M·{ it.%M() }?.%M",
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

        addStatement(
            "val·${ktor.compositeValHeaders}:·%T·=·${ktor.compositePropertyRequestHeaders}"
                    + (if (ktor.dynamicHttpHeaders) "()" else ""),
            MAP.parameterizedBy(STRING, STRING)
        )
        addStatement(
            "val·${ktor.compositeValPayload}:·%T·=·${ktor.compositePropertySubscriptionPayload}"
                    + (if (ktor.dynamicHttpHeaders) "()" else ""),
            MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
        )
        controlFlow(
            "${ktor.compositePropertyClient}.%M(\n⇥${ktor.compositePropertyWebSocketUrl},\n" +
                    "request = {\n⇥" +
                    ktor.compositeValHeaders +
                    ".%M·{·(key,·value)·->\n⇥headers[key] = value⇤\n}" +
                    "⇤\n}⇤\n)",
            MemberName("io.ktor.client.plugins.websocket", "webSocket"),
            Kotlin.forEach
        ) {
            statement(MAP.parameterizedBy(STRING, ANY.nullable()).nullable()) {
                "var·${ktor.compositeValInitPayload}:·%T·=·${ktor.compositeValPayload}"
            }
            controlFlow(
                "if (${ktor.compositePropertyHttpTokenHeader} " +
                        "%M ${ktor.compositeValHeaders})",
                Kotlin.contains
            ) {
                controlFlow("if (${ktor.compositeValInitPayload}·==·null)") {
                    addStatement("${ktor.compositeValInitPayload} = %M()", Kotlin.mapOf)
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
                                "%M(${ktor.compositePropertyWebSocketTokenHeader} " +
                                "%M ${ktor.compositeValHeaders}" +
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
    val subscriptionId = ktor.compositeValSubscriptionId
    val result = ktor.compositeValResult

    buildFunction(ktor.compositeFunExecuteSubscriptionImpl) {
        addModifiers(PROTECTED, OPEN, SUSPEND)
        receiver(ClassName("io.ktor.websocket", "WebSocketSession"))
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

            buildAnonymousClass {
                addSuperinterface(context.receiverClass.parameterizedBy(schema.subscription.dtoClass))

                buildFunction(context.receiverFunReceive) {
                    addModifiers(SUSPEND, OVERRIDE)
                    returns(schema.subscription.dtoClass)

                    controlFlow("while·(true)") {
                        controlFlow("when·(val·$reply·=·$receiveMessage())") {
                            controlFlow("is·%T·->", message(GQL_DATA)) {
                                addStatement("%M($reply.id·==·$subscriptionId)", Kotlin.require)
                                addStatement("")

                                addStatement("val·$result·=·$reply.payload")
                                controlFlow(
                                    "$result.errors?.%M·{ it.%M() }?.%M",
                                    Kotlin.takeIf, Kotlin.isNotEmpty, Kotlin.let
                                ) {
                                    addStatement(
                                        "throw·%T(%S, $request, it)",
                                        graphql.exceptionClass,
                                        "GraphQL subscription failed"
                                    )
                                }
                                addStatement(
                                    "return·$result.data ?: throw·%T(\n⇥%S,\n$request⇤\n)",
                                    graphql.exceptionClass,
                                    "GraphQL subscription completes successfully but returns no data"
                                )
                            }

                            controlFlow("is·%T·->", message(GQL_ERROR)) {
                                addStatement("%M($reply.id·==·$subscriptionId)", Kotlin.require)
                                addStatement(
                                    "throw·%T(%S, $request, $reply.payload.errors)",
                                    graphql.exceptionClass,
                                    "Subscription failed"
                                )
                            }

                            controlFlow("is·%T·->", message(GQL_COMPLETE)) {
                                addStatement("%M($reply.id·==·$subscriptionId)", Kotlin.require)
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
                    addStatement("%M(%S)", Kotlin.error, "Invalid algorithm")
                }

                if (context.commitEnabled) {
                    buildFunction(context.receiverFunCommit) {
                        addModifiers(SUSPEND, OVERRIDE)
                        returns(UNIT)

                        addStatement("//·Do·nothing")
                    }
                }
            }.also {
                controlFlow("try") {
                    addStatement("$block.invoke(%L)", it)
                }
                controlFlow("finally") {
                    buildSendMessage(layout, GQL_STOP) {
                        subscriptionId
                    }
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
    val also = MemberName("kotlin", "also")
    val takeIf = MemberName("kotlin", "takeIf")
    val let = MemberName("kotlin", "let")
    val to = MemberName("kotlin", "to")
    val error = MemberName("kotlin", "error")
    val require = MemberName("kotlin", "require")

    val isNotEmpty = MemberName("kotlin.collections", "isNotEmpty")
    val forEach = MemberName("kotlin.collections", "forEach")
    val mapOf = MemberName("kotlin.collections", "mapOf")

    val contains = MemberName("kotlin.collections", KOperator.CONTAINS)
    val notContains = MemberName("kotlin.collections", KOperator.NOT_CONTAINS)
    val plus = MemberName("kotlin.collections", KOperator.PLUS)
}

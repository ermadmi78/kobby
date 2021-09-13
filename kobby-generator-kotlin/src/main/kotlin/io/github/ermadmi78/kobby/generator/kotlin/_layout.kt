@file:Suppress("unused")

package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.model.decorate
import io.github.ermadmi78.kobby.model.isNotEmpty
import java.io.File
import java.nio.file.Path

/**
 * Created on 24.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

const val EQUALS_FUN = "equals"
const val EQUALS_ARG = "other"
const val HASH_CODE_FUN = "hashCode"
const val HASH_CODE_RES = "result"
const val TO_STRING_FUN = "toString"

//******************************************************************************************************************
//                                   KotlinFile
//******************************************************************************************************************

internal fun FileSpec.toKotlinFile(): KotlinFile {
    val spec = this
    return object : KotlinFile {
        override fun writeTo(out: Appendable) {
            spec.writeTo(out)
        }

        override fun writeTo(directory: Path) {
            spec.writeTo(directory)
        }

        override fun writeTo(directory: File) {
            spec.writeTo(directory)
        }
    }
}

//******************************************************************************************************************
//                                   KotlinType
//******************************************************************************************************************

internal val KotlinType.typeName: TypeName
    get() = ClassName(packageName, className.toKotlinPath())
        .let { res ->
            if (generics.isEmpty()) {
                res.let { if (allowNull) it.copy(true) else it }
            } else {
                res.parameterizedBy(generics.map { it.typeName })
                    .let { if (allowNull) it.copy(true) else it }
            }
        }

private fun String.toKotlinPath(): List<String> = splitToSequence('.')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .onEach {
        require(it.isKotlinIdentifier()) {
            "Invalid path [$this] - [$it] is not identifier"
        }
    }.toList()

internal fun String.validateKotlinPath(): String =
    toKotlinPath().joinToString(".") { it }

private fun String.isKotlinIdentifier(): Boolean {
    if (isEmpty()) {
        return false
    }
    for (i in indices) {
        val c = get(i)
        if (i == 0) {
            if (!Character.isJavaIdentifierStart(c)) {
                return false
            }
        } else {
            if (!Character.isJavaIdentifierPart(c)) {
                return false
            }
        }
    }
    return true
}

//******************************************************************************************************************
//                                   KotlinDtoGraphQLLayout
//******************************************************************************************************************

internal val KotlinDtoGraphQLLayout.requestName: String
    get() = "Request".decorate(decoration)

internal val KotlinDtoGraphQLLayout.requestClass: ClassName
    get() = ClassName(packageName, requestName)

internal val KotlinDtoGraphQLLayout.errorTypeName: String
    get() = "ErrorType".decorate(decoration)

internal val KotlinDtoGraphQLLayout.errorTypeClass: ClassName
    get() = ClassName(packageName, errorTypeName)

internal val KotlinDtoGraphQLLayout.errorSourceLocationName: String
    get() = "ErrorSourceLocation".decorate(decoration)

internal val KotlinDtoGraphQLLayout.errorSourceLocationClass: ClassName
    get() = ClassName(packageName, errorSourceLocationName)

internal val KotlinDtoGraphQLLayout.errorName: String
    get() = "Error".decorate(decoration)

internal val KotlinDtoGraphQLLayout.errorClass: ClassName
    get() = ClassName(packageName, errorName)

internal val KotlinDtoGraphQLLayout.exceptionName: String
    get() = "Exception".decorate(decoration)

internal val KotlinDtoGraphQLLayout.exceptionClass: ClassName
    get() = ClassName(packageName, exceptionName)

internal val KotlinDtoGraphQLLayout.queryResultName: String
    get() = "QueryResult".decorate(decoration)

internal val KotlinDtoGraphQLLayout.queryResultClass: ClassName
    get() =
        ClassName(packageName, queryResultName)

internal val KotlinDtoGraphQLLayout.mutationResultName: String
    get() = "MutationResult".decorate(decoration)

internal val KotlinDtoGraphQLLayout.mutationResultClass: ClassName
    get() = ClassName(packageName, mutationResultName)

internal val KotlinDtoGraphQLLayout.subscriptionResultName: String
    get() = "SubscriptionResult".decorate(decoration)

internal val KotlinDtoGraphQLLayout.subscriptionResultClass: ClassName
    get() = ClassName(packageName, subscriptionResultName)

internal val KotlinDtoGraphQLLayout.errorResultName: String
    get() = "ErrorResult".decorate(decoration)

internal val KotlinDtoGraphQLLayout.errorResultClass: ClassName
    get() = ClassName(packageName, errorResultName)

//******************************************************************************************************************
//                                   Messaging
//******************************************************************************************************************

internal val KotlinDtoGraphQLLayout.messageName: String
    get() = "Message".decorate(decoration)

internal val KotlinDtoGraphQLLayout.messageClass: ClassName
    get() = ClassName(packageName, messageName)

internal val KotlinDtoGraphQLLayout.clientMessageName: String
    get() = "ClientMessage".decorate(decoration)

internal val KotlinDtoGraphQLLayout.clientMessageClass: ClassName
    get() = ClassName(packageName, clientMessageName)

internal val KotlinDtoGraphQLLayout.serverMessageName: String
    get() = "ServerMessage".decorate(decoration)

internal val KotlinDtoGraphQLLayout.serverMessageClass: ClassName
    get() = ClassName(packageName, serverMessageName)

internal enum class GqlMessage(val type: String, internal val _name: String, val client: Boolean, val server: Boolean) {
    GQL_CONNECTION_INIT("connection_init", "ConnectionInit", true, false),
    GQL_START("start", "Start", true, false),
    GQL_STOP("stop", "Stop", true, false),
    GQL_CONNECTION_TERMINATE("connection_terminate", "ConnectionTerminate", true, false),
    GQL_CONNECTION_ERROR("connection_error", "ConnectionError", false, true),
    GQL_CONNECTION_ACK("connection_ack", "ConnectionAck", false, true),
    GQL_DATA("data", "Data", false, true),
    GQL_ERROR("error", "Error", false, true),
    GQL_COMPLETE("complete", "Complete", false, true),
    GQL_CONNECTION_KEEP_ALIVE("ka", "ConnectionKeepAlive", false, true)
}

internal fun KotlinDtoGraphQLLayout.messageImplName(massage: GqlMessage) =
    "Message${massage._name}".decorate(decoration)

internal fun KotlinDtoGraphQLLayout.messageImplClass(massage: GqlMessage) =
    ClassName(packageName, messageImplName(massage))

//******************************************************************************************************************
//                                   KotlinContextLayout
//******************************************************************************************************************

internal val KotlinContextLayout.contextName: String
    get() = "Context".decorate(decoration)

internal val KotlinContextLayout.contextClass: ClassName
    get() = ClassName(packageName, contextName)

internal val KotlinLayout.contextImplName: String
    get() = context.contextName.run {
        if (impl.decoration.isNotEmpty()) decorate(impl.decoration) else decorate(null, "Impl")
    }

internal val KotlinLayout.contextImplClass: ClassName
    get() = ClassName(context.packageName, contextImplName)

internal val KotlinContextLayout.subscriberName: String
    get() = "Subscriber".decorate(decoration)

internal val KotlinContextLayout.subscriberClass: ClassName
    get() = ClassName(packageName, subscriberName)

internal val KotlinContextLayout.subscriberFunSubscribe: String
    get() = "subscribe"

internal val KotlinContextLayout.subscriberArgBlock: String
    get() = "block"

internal val KotlinContextLayout.dslName: String
    get() = "DSL".decorate(decoration)

internal val KotlinContextLayout.dslClass: ClassName
    get() = ClassName(packageName, dslName)

internal val KotlinContextLayout.adapterName: String
    get() = "Adapter".decorate(decoration)

internal val KotlinContextLayout.adapterClass: ClassName
    get() = ClassName(packageName, adapterName)

internal val KotlinContextLayout.adapterFunExecuteQuery: String
    get() = "executeQuery"

internal val KotlinContextLayout.adapterFunExecuteMutation: String
    get() = "executeMutation"

internal val KotlinContextLayout.adapterFunExecuteSubscription: String
    get() = "executeSubscription"

internal val KotlinContextLayout.adapterArgQuery: Pair<String, TypeName>
    get() = "query" to STRING

internal val KotlinContextLayout.adapterArgVariables: Pair<String, TypeName>
    get() = "variables" to MAP.parameterizedBy(STRING, ANY.nullable())

internal val KotlinContextLayout.adapterArgBlock: String
    get() = "block"

internal val KotlinContextLayout.receiverName: String
    get() = "Receiver".decorate(decoration)

internal val KotlinContextLayout.receiverClass: ClassName
    get() = ClassName(packageName, receiverName)

internal val KotlinContextLayout.receiverFunReceive: String
    get() = "receive"

internal fun KotlinContextLayout.receiverLambda(receiverType: TypeName): LambdaTypeName =
    LambdaTypeName.get(
        receiverClass.parameterizedBy(receiverType),
        emptyList(),
        UNIT
    ).copy(suspending = true)

internal val KotlinContextLayout.mapperName: String
    get() = "Mapper".decorate(decoration)

internal val KotlinContextLayout.mapperClass: ClassName
    get() = ClassName(packageName, mapperName)

internal val KotlinContextLayout.mapperFunSerialize: String
    get() = "serialize"

internal val KotlinContextLayout.mapperFunDeserialize: String
    get() = "deserialize"

//******************************************************************************************************************
//                                   KotlinImplLayout
//******************************************************************************************************************

internal val KotlinImplLayout.contextPropertyName: String
    get() = "context".decorate(innerDecoration)

internal val KotlinImplLayout.projectionPropertyName: String
    get() = "projection".decorate(innerDecoration)

internal val KotlinImplLayout.dtoPropertyName: String
    get() = "dto".decorate(innerDecoration)

internal val KotlinImplLayout.repeatProjectionFunName: String
    get() = "_" + "repeatProjection".decorate(innerDecoration)

internal val KotlinImplLayout.repeatSelectionFunName: String
    get() = "_" + "repeatSelection".decorate(innerDecoration)

internal val KotlinImplLayout.buildFunName: String
    get() = "_" + "build".decorate(innerDecoration)

internal val KotlinImplLayout.interfaceIgnore: Pair<String, TypeName>
    get() = Pair("_" + "ignore".decorate(innerDecoration), MUTABLE_SET.parameterizedBy(STRING))

internal val buildFunArgIgnore: Pair<String, TypeName> =
    Pair("ignore", SET.parameterizedBy(STRING))

internal val buildFunArgHeader: Pair<String, TypeName> =
    Pair("header", ClassName("kotlin.text", "StringBuilder"))

internal val buildFunArgBody: Pair<String, TypeName> =
    Pair("body", ClassName("kotlin.text", "StringBuilder"))

internal val buildFunArgArguments: Pair<String, TypeName> =
    Pair("arguments", MUTABLE_MAP.parameterizedBy(STRING, ANY))

internal val argPrefix: String get() = "arg"

internal val buildFunValSubBody: Pair<String, TypeName> =
    Pair("subBody", ClassName("kotlin.text", "StringBuilder"))

//******************************************************************************************************************
//                                   KotlinAdapterKtorLayout
//******************************************************************************************************************

internal val KotlinAdapterKtorLayout.simpleName: String
    get() = "Simple".decorate(decoration)

internal val KotlinAdapterKtorLayout.simpleClass: ClassName
    get() = ClassName(packageName, simpleName)

internal val KotlinAdapterKtorLayout.simplePropertyClient: String get() = "client"
internal val KotlinAdapterKtorLayout.simplePropertyUrl: String get() = "url"
internal val KotlinAdapterKtorLayout.simplePropertyHeaders: String get() = "headers"
internal val KotlinAdapterKtorLayout.simplePropertyListener: String get() = "listener"
internal val KotlinAdapterKtorLayout.simpleValRequest: String get() = "request"
internal val KotlinAdapterKtorLayout.simpleValResult: String get() = "result"

//******************************************************************************************************************

internal val KotlinAdapterKtorLayout.compositeName: String
    get() = "Composite".decorate(decoration)

internal val KotlinAdapterKtorLayout.compositeClass: ClassName
    get() = ClassName(packageName, compositeName)

internal val KotlinAdapterKtorLayout.compositePropertyClient: String get() = "client"
internal val KotlinAdapterKtorLayout.compositePropertyHttpUrl: String get() = "httpUrl"
internal val KotlinAdapterKtorLayout.compositePropertyWebSocketUrl: String get() = "webSocketUrl"
internal val KotlinAdapterKtorLayout.compositePropertyMapper: String get() = "mapper"
internal val KotlinAdapterKtorLayout.compositePropertyRequestHeaders: String get() = "requestHeaders"
internal val KotlinAdapterKtorLayout.compositePropertySubscriptionPayload: String get() = "subscriptionPayload"
internal val KotlinAdapterKtorLayout.compositePropertyHttpTokenHeader: String
    get() = "httpAuthorizationTokenHeader"
internal val KotlinAdapterKtorLayout.compositePropertyWebSocketTokenHeader: String
    get() = "webSocketAuthorizationTokenHeader"
internal val KotlinAdapterKtorLayout.compositePropertyIdGenerator: String get() = "idGenerator"
internal val KotlinAdapterKtorLayout.compositePropertyListener: String get() = "listener"
internal val KotlinAdapterKtorLayout.compositeValRequest: String get() = "request"
internal val KotlinAdapterKtorLayout.compositeValContent: String get() = "content"
internal val KotlinAdapterKtorLayout.compositeValResult: String get() = "result"
internal val KotlinAdapterKtorLayout.compositeValInitPayload: String get() = "initPayload"

internal val KotlinAdapterKtorLayout.compositeFunExecuteSubscriptionImpl: String get() = "executeSubscriptionImpl"
internal val KotlinAdapterKtorLayout.compositeValReply: String get() = "reply"
internal val KotlinAdapterKtorLayout.compositeValReceiver: String get() = "receiver"
internal val KotlinAdapterKtorLayout.compositeValSubscriptionId: String get() = "subscriptionId"

internal val KotlinAdapterKtorLayout.compositeFunSendMessage: String get() = "sendMessage"
internal val KotlinAdapterKtorLayout.compositePropertyMessage: String get() = "message"
internal val KotlinAdapterKtorLayout.compositeFunReceiveMessage: String get() = "receiveMessage"

//******************************************************************************************************************
@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
internal val KotlinDtoLayout.adapterListenerLambda: LambdaTypeName
    get() = LambdaTypeName.get(parameters = arrayOf(graphql.requestClass), returnType = UNIT)
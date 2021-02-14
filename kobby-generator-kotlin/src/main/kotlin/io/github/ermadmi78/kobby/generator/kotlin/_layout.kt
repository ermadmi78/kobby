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

internal val KotlinContextLayout.adapterArgQuery: Pair<String, TypeName>
    get() = "query" to STRING

internal val KotlinContextLayout.adapterArgVariables: Pair<String, TypeName>
    get() = "variables" to MAP.parameterizedBy(STRING, ANY.nullable())

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
    get() = Pair("_" + "IGNORE".decorate(innerDecoration), SET.parameterizedBy(STRING))

internal val buildFunArgIgnore: Pair<String, TypeName> =
    Pair("ignore", SET.parameterizedBy(STRING))

internal val buildFunArgHeader: Pair<String, TypeName> =
    Pair("header", ClassName("kotlin.text", "StringBuilder"))

internal val buildFunArgBody: Pair<String, TypeName> =
    Pair("body", ClassName("kotlin.text", "StringBuilder"))

internal val buildFunArgArguments: Pair<String, TypeName> =
    Pair("arguments", MUTABLE_MAP.parameterizedBy(STRING, ANY))

internal val argPrefix: String = "arg"

internal val buildFunValSubBody: Pair<String, TypeName> =
    Pair("subBody", ClassName("kotlin.text", "StringBuilder"))
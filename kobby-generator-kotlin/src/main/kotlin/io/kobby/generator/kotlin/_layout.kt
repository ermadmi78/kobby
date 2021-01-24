package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.kobby.model.decorate
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

internal val KotlinContextLayout.dslName: String
    get() = "DSL".decorate(decoration)

internal val KotlinContextLayout.dslClass: ClassName
    get() = ClassName(packageName, dslName)


package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import graphql.language.FieldDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.Type
import java.io.File
import java.nio.file.Path

/**
 * Created on 30.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

internal fun KotlinType.toTypeName(): TypeName =
    ClassName(packageName, className.toKotlinPath())
        .let { res ->
            if (generics.isEmpty()) {
                res.let { if (allowNull) it.copy(true) else it }
            } else {
                res.parameterizedBy(generics.map { it.toTypeName() })
                    .let { if (allowNull) it.copy(true) else it }
            }
        }

internal fun String.toKotlinPath(): List<String> = splitToSequence('.')
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

internal fun String.decorate(decoration: Decoration): String =
    decorate(decoration.prefix, decoration.postfix)

internal fun String.decorate(prefix: String?, postfix: String?): String {
    return if (prefix.isNullOrBlank() && postfix.isNullOrBlank()) {
        this
    } else if (prefix.isNullOrBlank()) {
        this + postfix!!.trim()
    } else if (postfix.isNullOrBlank()) {
        prefix.trim() + this.capitalize()
    } else {
        prefix.trim() + this.capitalize() + postfix.trim()
    }
}

//**********************************************************************************************************************

internal fun TypeName.nullable(): TypeName = copy(true)

//**********************************************************************************************************************

internal fun Type<*>.resolve(types: Map<String, TypeName>, nonNull: Boolean = false): TypeName = when (this) {
    is NonNullType -> type.resolve(types, true)
    is ListType -> LIST.parameterizedBy(type.resolve(types)).run {
        if (nonNull) this else copy(true)
    }
    is graphql.language.TypeName -> types[name]?.run {
        if (nonNull) this else copy(true)
    } ?: error("Scalar type is not configured: $name")
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun Type<*>.extractName(): String = when (this) {
    is NonNullType -> type.extractName()
    is ListType -> type.extractName()
    is graphql.language.TypeName -> name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun Type<*>.isId(): Boolean = when (this) {
    is NonNullType -> type.isId()
    is ListType -> false
    is graphql.language.TypeName -> "ID" == name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

//**********************************************************************************************************************

internal fun FieldDefinition.isRequired(): Boolean =
    type.isId() && inputValueDefinitions.isEmpty()

internal fun FieldDefinition.isDefault(): Boolean = false //todo

internal fun String.projection(layout: KotlinEntityLayout): ClassName =
    ClassName(layout.packageName, this.decorate(layout.projection.decoration))

internal fun FieldDefinition.projectionName(withDecoration: Decoration, withoutDecoration: Decoration): String =
    isDefault().let { if (it) withoutDecoration else withDecoration }.let { name.decorate(it) }

internal fun FieldDefinition.projectionName(layout: KotlinEntityLayout): String = layout.projection.run {
    projectionName(withDecoration, withoutDecoration)
}

internal fun FunSpec.Builder.addProjectionParameter(layout: KotlinEntityLayout, typeName: String): FunSpec.Builder =
    addParameter(
        ParameterSpec.builder(
            layout.projection.argument,
            LambdaTypeName.get(typeName.projection(layout), emptyList(), UNIT)
        ).defaultValue("{}").build()
    )
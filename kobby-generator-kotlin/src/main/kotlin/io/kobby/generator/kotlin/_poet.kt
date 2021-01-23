package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import io.kobby.model.KobbyScope

/**
 * Created on 23.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@KobbyScope
internal typealias FileSpecBuilder = FileSpec.Builder

@KobbyScope
internal typealias TypeSpecBuilder = TypeSpec.Builder

@KobbyScope
internal typealias PropertySpecBuilder = PropertySpec.Builder

@KobbyScope
internal typealias FunSpecBuilder = FunSpec.Builder

@KobbyScope
internal typealias ParameterSpecBuilder = ParameterSpec.Builder

internal fun buildFile(
    packageName: String,
    fileName: String,
    block: FileSpecBuilder.() -> Unit
): FileSpec = FileSpec.builder(packageName, fileName).apply(block).build()

internal fun FileSpecBuilder.buildClass(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.classBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildInterface(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.interfaceBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildEnum(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.enumBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildFunction(
    name: String,
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.builder(name).apply(block).build().also {
    addFunction(it)
}

internal fun TypeSpecBuilder.buildPrimaryConstructor(
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.constructorBuilder().apply(block).build().also {
    primaryConstructor(it)
}

internal fun TypeSpecBuilder.buildProperty(
    name: String,
    type: TypeName,
    block: PropertySpecBuilder.() -> Unit
): PropertySpec = PropertySpec.builder(name, type).apply(block).build().also {
    addProperty(it)
}

internal fun TypeSpecBuilder.buildEnumConstant(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.anonymousClassBuilder().apply(block).build().also {
    addEnumConstant(name, it)
}

internal fun FunSpecBuilder.buildParameter(
    name: String,
    type: TypeName,
    block: ParameterSpecBuilder.() -> Unit
): ParameterSpec = ParameterSpec.builder(name, type).apply(block).build().also {
    addParameter(it)
}

//**********************************************************************************************************************
//                                              Jackson
//**********************************************************************************************************************

internal object JacksonAnnotations1 {
    val JSON_CREATOR = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonCreator"
    )

    val JSON_TYPE_NAME = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeName"
    )

    val JSON_TYPE_INFO = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeInfo"
    )

    val JSON_INCLUDE = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonInclude"
    )
}

internal enum class JacksonInclude1 {
    ALWAYS,
    NON_NULL,
    NON_ABSENT,
    NON_EMPTY,
    NON_DEFAULT,
    CUSTOM,
    USE_DEFAULTS
}
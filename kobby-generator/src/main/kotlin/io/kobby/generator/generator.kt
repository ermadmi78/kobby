package io.kobby.generator

import com.squareup.kotlinpoet.*
import graphql.schema.idl.SchemaParser
import java.io.Reader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class GeneratorLayout(
    val dtoPackage: PackageSpec,
    val apiPackage: PackageSpec,
    val implPackage: PackageSpec,
    val dtoPrefix: String? = null,
    val dtoPostfix: String? = "Dto",
    val implPrefix: String? = null,
    val implPostfix: String? = "Impl",
    val scalars: Map<String, TypeName> = Scalars.PREDEFINED
)

data class FilesLayout(
    val dtoFiles: List<FileSpec> = listOf(),
    val apiFiles: List<FileSpec> = listOf(),
    val implFiles: List<FileSpec> = listOf()
)

fun generate(layout: GeneratorLayout, schema: Reader): FilesLayout {
    val graphQLSchema = SchemaParser().parse(schema)

    return FilesLayout()
}

object Scalars {
    val PREDEFINED: Map<String, TypeName> = mapOf(
        "ID" to LONG,
        "Int" to INT,
        "Long" to LONG,
        "Float" to DOUBLE,
        "Double" to DOUBLE,
        "String" to STRING,
        "Boolean" to BOOLEAN
    )
}
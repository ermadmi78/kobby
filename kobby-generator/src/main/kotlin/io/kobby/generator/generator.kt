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
    val dto: DtoLayout,
    val api: ApiLayout,
    val impl: ImplLayout,
    val scalars: Map<String, TypeName> = Scalars.PREDEFINED
)

data class DtoLayout(
    val packageSpec: PackageSpec,
    val prefix: String? = null,
    val postfix: String? = "Dto",
    val jacksonized: Boolean = true,
    val builders: Boolean = true
) {
    val packageName: String get() = packageSpec.name
}

data class ApiLayout(
    val packageSpec: PackageSpec
) {
    val packageName: String get() = packageSpec.name
}

data class ImplLayout(
    val packageSpec: PackageSpec,
    val prefix: String? = null,
    val postfix: String? = "Impl"
) {
    val packageName: String get() = packageSpec.name
}

data class FilesLayout(
    val dtoFiles: List<FileSpec> = listOf(),
    val apiFiles: List<FileSpec> = listOf(),
    val implFiles: List<FileSpec> = listOf()
)

fun generate(layout: GeneratorLayout, schema: Reader): FilesLayout {
    val graphQLSchema = SchemaParser().parse(schema)

    val dto = generateDto(layout, graphQLSchema)


    return FilesLayout(
        dtoFiles = dto.values.map { FileSpec.get(layout.dto.packageName, it) }
    )
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
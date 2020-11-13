package io.kobby.generator

import com.squareup.kotlinpoet.FileSpec
import graphql.schema.idl.SchemaParser
import java.io.Reader

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

data class GeneratorConfig(
    val rootPackage: String,
    val dtoPackage: String?
)

fun generate(config: GeneratorConfig, schema: Reader): List<FileSpec> {
    val graphQLSchema = SchemaParser().parse(schema)

    TODO()
}
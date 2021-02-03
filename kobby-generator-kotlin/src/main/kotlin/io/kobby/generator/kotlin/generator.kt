package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier.*
import io.kobby.model.KobbySchema

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun generateKotlin(schema: KobbySchema, layout: KotlinLayout): List<KotlinFile> = with(layout) {
    val files = mutableListOf<FileSpec>()

    files += buildFile(context.packageName, context.name) {
        // Build DSL annotation
        buildClass(context.dslName) {
            addModifiers(ANNOTATION)
            addAnnotation(DslMarker::class)
        }

        buildInterface(context.adapterName) {
            buildFunction(context.adapterFunExecuteQuery) {
                addModifiers(SUSPEND, ABSTRACT)
                buildParameter(context.adapterArgQuery)
                buildParameter(context.adapterArgVariables)
                returns(schema.query.dtoClass)
            }

            buildFunction(context.adapterFunExecuteMutation) {
                addModifiers(SUSPEND, ABSTRACT)
                buildParameter(context.adapterArgQuery)
                buildParameter(context.adapterArgVariables)
                returns(schema.mutation.dtoClass)
            }
        }
    }

    files += generateDto(schema, layout)
    if (entity.enabled) {
        files += generateEntity(schema, layout)
        files += generateImpl(schema, layout)
    }

    files.map { it.toKotlinFile() }
}
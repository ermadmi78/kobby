package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier.*
import io.kobby.model.KobbyNode
import io.kobby.model.KobbySchema
import io.kobby.model.decorate
import io.kobby.model.isNotEmpty

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun generateKotlin(schema: KobbySchema, layout: KotlinLayout): List<KotlinFile> = with(layout) {
    val files = mutableListOf<FileSpec>()

    files += buildFile(context.packageName, context.name) {
        if (entity.enabled) {
            //Build context builder
            buildFunction(context.contextName.decapitalize() + "Of") {
                buildParameter(Const.ADAPTER, context.adapterClass)
                returns(context.contextClass)
                addStatement("return %T(${Const.ADAPTER})", layout.contextImplClass)
            }

            // Build context interface
            buildInterface(context.contextName) {
                buildFunction(context.query) {
                    addModifiers(SUSPEND, ABSTRACT)
                    buildParameter(entity.projection.projectionArgument, schema.query.projectionLambda)
                    returns(schema.query.entityClass)
                }

                buildFunction(context.mutation) {
                    addModifiers(SUSPEND, ABSTRACT)
                    buildParameter(entity.projection.projectionArgument, schema.mutation.projectionLambda)
                    returns(schema.mutation.entityClass)
                }
            }

            // Build adapter interface
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

            buildContextImplementation(schema, layout)
        }

        // Build DSL annotation
        buildClass(context.dslName) {
            addModifiers(ANNOTATION)
            addAnnotation(DslMarker::class)
        }
    }

    files += generateDto(schema, layout)
    if (entity.enabled) {
        files += generateEntity(schema, layout)
        files += generateImpl(schema, layout)
    }

    files.map { it.toKotlinFile() }
}

private fun FileSpecBuilder.buildContextImplementation(schema: KobbySchema, layout: KotlinLayout) = with(layout) {
    buildClass(contextImplName) {
        addModifiers(PRIVATE)
        addSuperinterface(context.contextClass)

        buildPrimaryConstructorProperties {
            buildProperty(Const.ADAPTER, context.adapterClass) {
                addModifiers(PRIVATE)
            }
        }

        // query
        buildContextFunction(
            schema.query,
            layout,
            context.query,
            "query",
            context.adapterFunExecuteQuery
        )

        // mutation
        buildContextFunction(
            schema.mutation,
            layout,
            context.mutation,
            "mutation",
            context.adapterFunExecuteMutation
        )
    }
}

private fun TypeSpecBuilder.buildContextFunction(
    node: KobbyNode,
    layout: KotlinLayout,
    name: String,
    operation: String,
    adapterFun: String
) = with(layout) {
    buildFunction(name) {
        addModifiers(SUSPEND, OVERRIDE)
        buildParameter(entity.projection.projectionArgument, node.projectionLambda)
        addKdoc("https://youtrack.jetbrains.com/issue/KTIJ-844")
        suppressBlocking()
        returns(node.entityClass)

        val projectionRef = entity.projection.projectionArgument.trim('_').decorate(null, "Ref")
        statement(node.implProjectionClass) {
            "val $projectionRef = %T().apply(${entity.projection.projectionArgument})"
        }

        val header = buildFunArgHeader
        val body = buildFunArgBody
        val arguments = buildFunArgArguments

        addStatement("")
        addStatement("val ${header.first} = %T()", header.second)
        addStatement("val ${body.first} = %T(64)", body.second)
        addStatement(
            "val ${arguments.first}: %T = %T()", arguments.second,
            ClassName("kotlin.collections", "mutableMapOf")
        )
        addStatement(
            "$projectionRef.${impl.buildFunName}(%T(), ${header.first}, ${body.first}, ${arguments.first})",
            ClassName("kotlin.collections", "setOf")
        )

        addStatement("")
        controlFlow(
            "val $operation = buildString(" +
                    "${header.first}.length + ${body.first}.length + ${operation.length + 2})"
        ) {
            append(operation)
            ifFlow("${header.first}.isNotEmpty()") {
                append('(')
                addStatement("append(${header.first})")
                append(')')
            }
            addStatement("append(${body.first})")
        }

        val dto = operation.run {
            if (dto.decoration.isNotEmpty()) decorate(dto.decoration) else decorate(null, "Dto")
        }
        addStatement("")
        addStatement("val $dto: %T = ${Const.ADAPTER}.$adapterFun($operation, ${arguments.first})", node.dtoClass)
        addStatement("return $dto.%T(this, $projectionRef)", ClassName(impl.packageName, node.entityBuilderName))
    }
}

private object Const {
    const val ADAPTER = "adapter"
}
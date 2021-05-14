package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 17.05.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateResolver(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    schema.objects.values.asSequence().filter { it.isResolve }.forEach { node ->
        files += buildFile(resolver.packageName, node.resolverName) {
            buildInterface(node.resolverName) {
                node.comments {
                    addKdoc(it)
                }
                addSuperinterface(
                    when {
                        node.isQuery -> ClassName(
                            "graphql.kickstart.tools", "GraphQLQueryResolver"
                        )
                        node.isMutation -> ClassName(
                            "graphql.kickstart.tools", "GraphQLMutationResolver"
                        )
                        else -> ClassName(
                            "graphql.kickstart.tools", "GraphQLResolver"
                        ).parameterizedBy(node.dtoClass)
                    }
                )

                node.fields.values.asSequence().filter { it.isResolve }.forEach { field ->
                    buildFunction(field.name) {
                        addModifiers(KModifier.SUSPEND)
                        if (resolver.toDoMessage == null) {
                            addModifiers(KModifier.ABSTRACT)
                        }
                        field.comments {
                            addKdoc(it)
                        }
                        returns(field.type.dtoType)

                        if (!node.isOperation) {
                            buildParameter(field.resolverArgument, node.dtoClass)
                        }
                        field.arguments { arg ->
                            buildParameter(arg.name, arg.type.dtoType) {
                                arg.comments {
                                    addKdoc(it)
                                }
                            }
                        }

                        resolver.toDoMessage?.also {
                            addStatement("return %T(%S)", ClassName("kotlin", "TODO"), it)
                        }
                    }
                }
            }
        }
    }

    files
}
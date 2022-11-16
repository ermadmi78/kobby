package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.KModifier.ANNOTATION
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.model.*

/**
 * Created on 12.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun generateKotlin(schema: KobbySchema, layout: KotlinLayout): List<KotlinFile> = with(layout) {
    val files = mutableListOf<FileSpec>()

    files += buildFile(context.packageName, context.name) {
        if (entity.enabled) {
            // Build context builder
            buildFunction(context.contextName._decapitalize() + "Of") {
                buildParameter(Const.ADAPTER, context.adapterClass)
                returns(context.contextClass)
                addStatement("return·%T(${Const.ADAPTER})", layout.contextImplClass)
            }

            // Static query builder function
            buildBuilderFunction(
                schema.query,
                layout,
                context.buildQueryFun,
                "query"
            )

            // Static mutation builder function
            buildBuilderFunction(
                schema.mutation,
                layout,
                context.buildMutationFun,
                "mutation"
            )

            // Static subscription builder function
            buildBuilderFunction(
                schema.subscription,
                layout,
                context.buildSubscriptionFun,
                "subscription"
            )

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

                buildFunction(context.subscription) {
                    addModifiers(ABSTRACT)
                    buildParameter(entity.projection.projectionArgument, schema.subscription.projectionLambda)
                    returns(context.subscriberClass.parameterizedBy(schema.subscription.entityClass))
                }
            }

            // Build subscriber interface
            buildInterface(context.subscriberName, true) {
                val typeVariable = TypeVariableName.invoke("T")
                addTypeVariable(typeVariable)
                buildFunction(context.subscriberFunSubscribe) {
                    addModifiers(SUSPEND, ABSTRACT)
                    addParameter(context.subscriberArgBlock, context.receiverLambda(typeVariable))
                    returns(UNIT)
                }
            }

            // Build receiver interface
            buildInterface(context.receiverName, !context.commitEnabled) {
                addAnnotation(context.dslClass)

                val typeVariable = TypeVariableName.invoke("T", OUT)
                addTypeVariable(typeVariable)

                buildFunction(context.receiverFunReceive) {
                    addModifiers(SUSPEND, ABSTRACT)
                    returns(typeVariable)
                }

                if (context.commitEnabled) {
                    buildFunction(context.receiverFunCommit) {
                        addModifiers(SUSPEND, ABSTRACT)
                        returns(UNIT)
                    }
                }
            }

            // Build adapter interface
            buildInterface(context.adapterName) {
                val notImplementedClass = ClassName("kotlin", "NotImplementedError")

                buildFunction(context.adapterFunExecuteQuery) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(context.adapterArgVariables)
                    returns(schema.query.dtoClass)

                    addStatement(
                        "return·throw·%T(%S)",
                        notImplementedClass,
                        "Adapter function ${context.adapterFunExecuteQuery} is not implemented"
                    )
                }

                buildFunction(context.adapterFunExecuteMutation) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(context.adapterArgVariables)
                    returns(schema.mutation.dtoClass)

                    addStatement(
                        "return·throw·%T(%S)",
                        notImplementedClass,
                        "Adapter function ${context.adapterFunExecuteMutation} is not implemented"
                    )
                }

                buildFunction(context.adapterFunExecuteSubscription) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(context.adapterArgVariables)
                    buildParameter(context.adapterArgBlock, schema.receiverSubscriptionDtoLambda)
                    returns(UNIT)

                    addStatement(
                        "return·throw·%T(%S)",
                        notImplementedClass,
                        "Adapter function ${context.adapterFunExecuteSubscription} is not implemented"
                    )
                }
            }

            buildContextImplementation(schema, layout)

            // Build mapper interface
            buildInterface(context.mapperName) {
                addKdoc("%L", "Helper interface for default adapter implementations")

                buildFunction(context.mapperFunSerialize) {
                    addModifiers(ABSTRACT)
                    buildParameter("value", ANY)
                    returns(STRING)
                }

                buildFunction(context.mapperFunDeserialize) {
                    addModifiers(ABSTRACT)
                    val typeVariable = TypeVariableName.invoke("T", ANY)
                    addTypeVariable(typeVariable)
                    buildParameter("content", STRING)
                    buildParameter(
                        "contentType",
                        ClassName("kotlin.reflect", "KClass").parameterizedBy(typeVariable)
                    )
                    returns(typeVariable)
                }
            }
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
        if (dto.graphql.enabled) {
            files += generateKtorAdapter(schema, layout)
        }
    }
    if (resolver.enabled) {
        files += generateResolver(schema, layout)
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
            context.adapterFunExecuteQuery,
            false
        )

        // mutation
        buildContextFunction(
            schema.mutation,
            layout,
            context.mutation,
            "mutation",
            context.adapterFunExecuteMutation,
            false
        )

        // subscription
        buildContextFunction(
            schema.subscription,
            layout,
            context.subscription,
            "subscription",
            context.adapterFunExecuteSubscription,
            true
        )
    }
}

private fun TypeSpecBuilder.buildContextFunction(
    node: KobbyNode,
    layout: KotlinLayout,
    name: String,
    operation: String,
    adapterFun: String,
    subscription: Boolean
) = with(layout) {
    buildFunction(name) {
        addModifiers(OVERRIDE)
        if (!subscription) {
            addModifiers(SUSPEND)
        }
        buildParameter(entity.projection.projectionArgument, node.projectionLambda)

        if (!subscription) {
            addKdoc("%L", "https://youtrack.jetbrains.com/issue/KTIJ-844")
            suppressBlocking()
        }

        returns(if (subscription) context.subscriberClass.parameterizedBy(node.entityClass) else node.entityClass)

        val projectionRef = entity.projection.projectionArgument.trim('_').decorate(null, "Ref")
        statement(node.implProjectionClass, MemberName("kotlin", "apply")) {
            "val·$projectionRef·=·%T().%M(${entity.projection.projectionArgument})"
        }

        val header = buildFunArgHeader
        val body = buildFunArgBody
        val arguments = buildFunArgArguments

        addStatement("")
        addStatement("val·${header.first}·=·%T()", header.second)
        addStatement("val·${body.first}·=·%T(64)", body.second)
        addStatement(
            "val·${arguments.first}:·%T·=·%T()", arguments.second,
            ClassName("kotlin.collections", "mutableMapOf")
        )
        addStatement(
            "$projectionRef.${impl.buildFunName}(%T(), ${header.first}, ${body.first}, ${arguments.first})",
            ClassName("kotlin.collections", "setOf")
        )

        addStatement("")
        controlFlow(
            "val·$operation·=·%M(" +
                    "${header.first}.length·+·${body.first}.length·+·${operation.length + 2})",
            MemberName("kotlin.text", "buildString")
        ) {
            buildAppendChain { appendLiteral(operation) }
            ifFlow(
                "${header.first}.%M()",
                MemberName("kotlin.text", "isNotEmpty")
            ) {
                buildAppendChain {
                    appendLiteral('(').appendExactly(header.first).appendLiteral(')')
                }
            }
            buildAppendChain { appendExactly(body.first) }
        }
        addStatement("")

        val dtoVal = operation.run {
            if (dto.decoration.isNotEmpty()) decorate(dto.decoration) else decorate(null, "Dto")
        }
        if (subscription) {
            controlFlow("return·%T", context.subscriberClass.parameterizedBy(node.entityClass)) {
                controlFlow("${Const.ADAPTER}.$adapterFun($operation, ${arguments.first})") {
                    buildAnonymousClass {
                        addSuperinterface(context.receiverClass.parameterizedBy(node.entityClass))

                        buildFunction(context.receiverFunReceive) {
                            addModifiers(SUSPEND, OVERRIDE)
                            returns(node.entityClass)

                            statement(node.dtoClass) {
                                "val·$dtoVal:·%T·=·this@$adapterFun.${context.receiverFunReceive}()"
                            }
                            statement(ClassName(impl.packageName, node.entityBuilderName), contextImplClass) {
                                "return·$dtoVal.%T(this@%T, $projectionRef)"
                            }
                        }

                        if (context.commitEnabled) {
                            buildFunction(context.receiverFunCommit) {
                                addModifiers(SUSPEND, OVERRIDE)
                                returns(UNIT)

                                addStatement("this@$adapterFun.${context.receiverFunCommit}()")
                            }
                        }
                    }.also {
                        addStatement("it.invoke(%L)", it)
                    }
                }
            }
        } else {
            statement(node.dtoClass) {
                "val·$dtoVal:·%T·=·${Const.ADAPTER}.$adapterFun($operation, ${arguments.first})"
            }
            statement(ClassName(impl.packageName, node.entityBuilderName)) {
                "return·$dtoVal.%T(this,·$projectionRef)"
            }
        }
    }
}

private fun FileSpecBuilder.buildBuilderFunction(
    node: KobbyNode,
    layout: KotlinLayout,
    name: String,
    operation: String
) = with(layout) {
    buildFunction(name) {
        buildParameter(entity.projection.projectionArgument, node.projectionLambda)
        returns(ClassName("kotlin", "Pair").parameterizedBy(STRING, MAP.parameterizedBy(STRING, ANY)))

        val projectionRef = entity.projection.projectionArgument.trim('_').decorate(null, "Ref")
        statement(node.implProjectionClass, MemberName("kotlin", "apply")) {
            "val·$projectionRef·=·%T().%M(${entity.projection.projectionArgument})"
        }

        val header = buildFunArgHeader
        val body = buildFunArgBody
        val arguments = buildFunArgArguments

        addStatement("")
        addStatement("val·${header.first}·=·%T()", header.second)
        addStatement("val·${body.first}·=·%T(64)", body.second)
        addStatement(
            "val·${arguments.first}:·%T·=·%T()", arguments.second,
            ClassName("kotlin.collections", "mutableMapOf")
        )
        addStatement(
            "$projectionRef.${impl.buildFunName}(%T(), ${header.first}, ${body.first}, ${arguments.first})",
            ClassName("kotlin.collections", "setOf")
        )

        addStatement("")
        controlFlow(
            "val·$operation·=·%M(" +
                    "${header.first}.length·+·${body.first}.length·+·${operation.length + 2})",
            MemberName("kotlin.text", "buildString")
        ) {
            buildAppendChain { appendLiteral(operation) }
            ifFlow(
                "${header.first}.%M()",
                MemberName("kotlin.text", "isNotEmpty")
            ) {
                buildAppendChain {
                    appendLiteral('(').appendExactly(header.first).appendLiteral(')')
                }
            }
            buildAppendChain { appendExactly(body.first) }
        }

        addStatement("")
        addStatement(
            "return·$operation·%T·${arguments.first}",
            ClassName("kotlin", "to")
        )
    }
}

private object Const {
    const val ADAPTER = "adapter"
}
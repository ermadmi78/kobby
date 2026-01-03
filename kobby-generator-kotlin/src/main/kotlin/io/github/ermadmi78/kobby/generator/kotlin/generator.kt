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
        if (dto.serialization.enabled) {
            // Build default mapper property
            buildProperty(
                context.jsonName,
                ClassName("kotlinx.serialization.json", "Json")
            ) {
                addKdoc("Default entry point to work with JSON serialization.")
                buildInitializer {
                    controlFlow("Json") {
                        addStatement("classDiscriminator·=·%S", dto.serialization.classDiscriminator)
                        addStatement("ignoreUnknownKeys·=·%L", dto.serialization.ignoreUnknownKeys)
                        addStatement("encodeDefaults·=·%L", dto.serialization.encodeDefaults)
                        addStatement("prettyPrint·=·%L", dto.serialization.prettyPrint)
                        controlFlow(
                            "serializersModule·=·%M",
                            MemberName("kotlinx.serialization.modules", "SerializersModule")
                        ) {
                            schema.interfacesAndUnions { polymorphicNode ->
                                controlFlow(
                                    "%M(%T::class)",
                                    MemberName(
                                        "kotlinx.serialization.modules",
                                        "polymorphic"
                                    ),
                                    polymorphicNode.dtoClass
                                ) {
                                    polymorphicNode.subObjects { subObjectNode ->
                                        addStatement(
                                            "%M(%T::class)",
                                            MemberName(
                                                "kotlinx.serialization.modules",
                                                "subclass"
                                            ),
                                            subObjectNode.dtoClass
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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

            val extendedApi = adapter.extendedApi

            if (extendedApi) {
                // Build response interface
                buildInterface(context.responseName) {
                    buildFunction(entity.errorsFunName) {
                        addModifiers(ABSTRACT)
                        returns(dto.errorsType)

                        var kDoc = "GraphQL response errors access function generated for adapters with extended API."
                        if (adapter.throwException) {
                            kDoc += " To enable GraphQL error propagation to the entity layer, " +
                                    "set Kobby configuration property `adapter.throwException` to `false`."
                        }
                        addKdoc("%L", kDoc)
                    }

                    buildFunction(entity.extensionsFunName) {
                        addModifiers(ABSTRACT)
                        returns(extensionsType)

                        addKdoc(
                            "%L",
                            "GraphQL response extensions access function generated for adapters with extended API."
                        )
                    }
                }
            }

            // Build adapter interface
            buildInterface(context.adapterName) {
                val notImplementedClass = ClassName("kotlin", "NotImplementedError")

                buildFunction(context.adapterFunExecuteQuery) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(adapterArgVariables)
                    returns(if (extendedApi) dto.graphql.queryResultClass else schema.query.dtoClass)

                    addStatement(
                        "return·throw·%T(%S)",
                        notImplementedClass,
                        "Adapter function ${context.adapterFunExecuteQuery} is not implemented"
                    )
                }

                buildFunction(context.adapterFunExecuteMutation) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(adapterArgVariables)
                    returns(if (extendedApi) dto.graphql.mutationResultClass else schema.mutation.dtoClass)

                    addStatement(
                        "return·throw·%T(%S)",
                        notImplementedClass,
                        "Adapter function ${context.adapterFunExecuteMutation} is not implemented"
                    )
                }

                buildFunction(context.adapterFunExecuteSubscription) {
                    addModifiers(SUSPEND)
                    buildParameter(context.adapterArgQuery)
                    buildParameter(adapterArgVariables)
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

            if (dto.serialization.enabled) {
                if (extendedApi) {
                    buildProperty(context.emptyJsonObjectName, SerializationJson.JSON_OBJECT) {
                        buildInitializer {
                            addStatement(
                                "%T(%M())",
                                SerializationJson.JSON_OBJECT,
                                MemberName("kotlin.collections", "emptyMap")
                            )
                        }
                    }
                }
            } else {
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

        val extendedApi = adapter.extendedApi
        val dtoVal = operation.run {
            if (dto.decoration.isNotEmpty()) decorate(dto.decoration) else decorate(null, "Dto")
        }
        val resultVal = operation.decorate(null, "Result")

        if (subscription) {
            controlFlow("return·%T", context.subscriberClass.parameterizedBy(node.entityClass)) {
                controlFlow("${Const.ADAPTER}.$adapterFun($operation, %L)", objectCode(arguments.first)) {
                    buildAnonymousClass {
                        addSuperinterface(context.receiverClass.parameterizedBy(node.entityClass))

                        buildFunction(context.receiverFunReceive) {
                            addModifiers(SUSPEND, OVERRIDE)
                            returns(node.entityClass)

                            if (extendedApi) {
                                statement(dto.graphql.subscriptionResultClass) {
                                    "val·$resultVal:·%T·=·this@$adapterFun.${context.receiverFunReceive}()"
                                }
                                statement(ClassName(impl.packageName, node.entityBuilderName), contextImplClass) {
                                    "return·$resultVal.%T(this@%T, $projectionRef)"
                                }
                            } else {
                                statement(node.dtoClass) {
                                    "val·$dtoVal:·%T·=·this@$adapterFun.${context.receiverFunReceive}()"
                                }
                                statement(ClassName(impl.packageName, node.entityBuilderName), contextImplClass) {
                                    "return·$dtoVal.%T(this@%T, $projectionRef)"
                                }
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
            if (extendedApi) {
                val resultClass: ClassName = when {
                    node.isQuery -> dto.graphql.queryResultClass
                    node.isMutation -> dto.graphql.mutationResultClass
                    else -> error("Invalid algorithm")
                }
                statement(resultClass, objectCode(arguments.first)) {
                    "val·$resultVal:·%T·=·${Const.ADAPTER}.$adapterFun($operation, %L)"
                }
                statement(ClassName(impl.packageName, node.entityBuilderName)) {
                    "return·$resultVal.%T(this,·$projectionRef)"
                }
            } else {
                statement(node.dtoClass, objectCode(arguments.first)) {
                    "val·$dtoVal:·%T·=·${Const.ADAPTER}.$adapterFun($operation, %L)"
                }
                statement(ClassName(impl.packageName, node.entityBuilderName)) {
                    "return·$dtoVal.%T(this,·$projectionRef)"
                }
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
        returns(
            ClassName("kotlin", "Pair")
                .parameterizedBy(STRING, objectType(nullable = false))
        )

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
            "return·$operation·%M·%L",
            MemberName("kotlin", "to"),
            objectCode(arguments.first)
        )
    }
}

private object Const {
    const val ADAPTER = "adapter"
}
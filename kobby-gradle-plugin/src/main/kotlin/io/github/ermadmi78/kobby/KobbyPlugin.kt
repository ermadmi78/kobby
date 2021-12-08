@file:Suppress(
    "unused"
)

package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.github.ermadmi78.kobby.task.KobbyKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.SourceSetContainer


/**
 * Created on 03.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyPlugin : Plugin<Project> {
    companion object {
        const val KOBBY = "kobby"
        const val COMPILE_KOTLIN_TASK_NAME = "compileKotlin"
    }

    private val <T> Lazy<T>.valueOrNull: T? get() = if (isInitialized()) value else null

    override fun apply(project: Project) {
        val extension = project.extensions.create(KOBBY, KobbyExtension::class.java)
        project.tasks.register(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java)

        project.afterEvaluate { p ->
            val kotlinTask = p.tasks.named(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java).get()
            if (extension.kotlinExtension.valueOrNull?.enabled == false) {
                kotlinTask.enabled = false
                p.logger.warn("$KOBBY: [${KobbyKotlin.TASK_NAME}] task disabled")
                return@afterEvaluate
            }

            extension.schemaExtension.valueOrNull?.apply {
                files?.also {
                    kotlinTask.schemaFiles.convention(p.provider<Iterable<RegularFile>> {
                        it.map { p.layout.file(p.provider { it }).get() }
                    })
                }
                scanExtension.valueOrNull?.apply {
                    dir?.also {
                        kotlinTask.schemaScanDir.convention(it)
                    }
                    includes?.also {
                        kotlinTask.schemaScanIncludes.convention(it)
                    }
                    excludes?.also {
                        kotlinTask.schemaScanExcludes.convention(it)
                    }
                }
                directiveExtension.valueOrNull?.apply {
                    primaryKey?.also {
                        kotlinTask.schemaDirectivePrimaryKey.convention(it)
                    }
                    required?.also {
                        kotlinTask.schemaDirectiveRequired.convention(it)
                    }
                    default?.also {
                        kotlinTask.schemaDirectiveDefault.convention(it)
                    }
                    selection?.also {
                        kotlinTask.schemaDirectiveSelection.convention(it)
                    }
                    resolve?.also {
                        kotlinTask.schemaDirectiveResolve.convention(it)
                    }
                }
            }
            extension.kotlinExtension.valueOrNull?.also { kotlinExtension ->
                kotlinExtension.scalars?.also {
                    kotlinTask.scalars.convention(PREDEFINED_SCALARS + it)
                }
                kotlinExtension.relativePackage?.also {
                    kotlinTask.relativePackage.convention(it)
                }
                kotlinExtension.packageName?.also {
                    kotlinTask.rootPackageName.convention(it)
                }
                kotlinExtension.outputDirectory?.also {
                    kotlinTask.outputDirectory.convention(it)
                }
                kotlinExtension.contextExtension.valueOrNull?.apply {
                    packageName?.also {
                        kotlinTask.contextPackageName.convention(it)
                    }
                    name?.also {
                        kotlinTask.contextName.convention(it)
                    }
                    prefix?.also {
                        kotlinTask.contextPrefix.convention(it)
                    }
                    postfix?.also {
                        kotlinTask.contextPostfix.convention(it)
                    }
                    query?.also {
                        kotlinTask.contextQuery.convention(it)
                    }
                    mutation?.also {
                        kotlinTask.contextMutation.convention(it)
                    }
                    subscription?.also {
                        kotlinTask.contextSubscription.convention(it)
                    }
                }
                kotlinExtension.dtoExtension.valueOrNull?.apply {
                    packageName?.also {
                        kotlinTask.dtoPackageName.convention(it)
                    }
                    prefix?.also {
                        kotlinTask.dtoPrefix.convention(it)
                    }
                    postfix?.also {
                        kotlinTask.dtoPostfix.convention(it)
                    }
                    enumPrefix?.also {
                        kotlinTask.dtoEnumPrefix.convention(it)
                    }
                    enumPostfix?.also {
                        kotlinTask.dtoEnumPostfix.convention(it)
                    }
                    inputPrefix?.also {
                        kotlinTask.dtoInputPrefix.convention(it)
                    }
                    inputPostfix?.also {
                        kotlinTask.dtoInputPostfix.convention(it)
                    }
                    applyPrimaryKeys?.also {
                        kotlinTask.dtoApplyPrimaryKeys.convention(it)
                    }
                    jacksonExtension.valueOrNull?.apply {
                        enabled?.also {
                            kotlinTask.dtoJacksonEnabled.convention(it)
                        }
                        typeInfoUse?.also {
                            kotlinTask.dtoJacksonTypeInfoUse.convention(it)
                        }
                        typeInfoInclude?.also {
                            kotlinTask.dtoJacksonTypeInfoInclude.convention(it)
                        }
                        typeInfoProperty?.also {
                            kotlinTask.dtoJacksonTypeInfoProperty.convention(it)
                        }
                        jsonInclude?.also {
                            kotlinTask.dtoJacksonJsonInclude.convention(it)
                        }
                    }
                    builderExtension.valueOrNull?.apply {
                        enabled?.also {
                            kotlinTask.dtoBuilderEnabled.convention(it)
                        }
                        prefix?.also {
                            kotlinTask.dtoBuilderPrefix.convention(it)
                        }
                        postfix?.also {
                            kotlinTask.dtoBuilderPostfix.convention(it)
                        }
                        copyFun?.also {
                            kotlinTask.dtoBuilderCopyFun.convention(it)
                        }
                    }
                    graphQLExtension.valueOrNull?.apply {
                        enabled?.also {
                            kotlinTask.dtoGraphQLEnabled.convention(it)
                        }
                        packageName?.also {
                            kotlinTask.dtoGraphQLPackageName.convention(it)
                        }
                        prefix?.also {
                            kotlinTask.dtoGraphQLPrefix.convention(it)
                        }
                        postfix?.also {
                            kotlinTask.dtoGraphQLPostfix.convention(it)
                        }
                    }
                }
                kotlinExtension.entityExtension.valueOrNull?.apply {
                    enabled?.also {
                        kotlinTask.entityEnabled.convention(it)
                    }
                    packageName?.also {
                        kotlinTask.entityPackageName.convention(it)
                    }
                    prefix?.also {
                        kotlinTask.entityPrefix.convention(it)
                    }
                    postfix?.also {
                        kotlinTask.entityPostfix.convention(it)
                    }
                    contextInheritanceEnabled?.also {
                        kotlinTask.entityContextInheritanceEnabled.convention(it)
                    }
                    contextFunEnabled?.also {
                        kotlinTask.entityContextFunEnabled.convention(it)
                    }
                    contextFunName?.also {
                        kotlinTask.entityContextFunName.convention(it)
                    }
                    withCurrentProjectionFun?.also {
                        kotlinTask.entityWithCurrentProjectionFun.convention(it)
                    }
                    projectionExtension.valueOrNull?.apply {
                        projectionPrefix?.also {
                            kotlinTask.entityProjectionPrefix.convention(it)
                        }
                        projectionPostfix?.also {
                            kotlinTask.entityProjectionPostfix.convention(it)
                        }
                        projectionArgument?.also {
                            kotlinTask.entityProjectionArgument.convention(it)
                        }
                        withPrefix?.also {
                            kotlinTask.entityWithPrefix.convention(it)
                        }
                        withPostfix?.also {
                            kotlinTask.entityWithPostfix.convention(it)
                        }
                        withoutPrefix?.also {
                            kotlinTask.entityWithoutPrefix.convention(it)
                        }
                        withoutPostfix?.also {
                            kotlinTask.entityWithoutPostfix.convention(it)
                        }
                        minimizeFun?.also {
                            kotlinTask.entityMinimizeFun.convention(it)
                        }
                        qualificationPrefix?.also {
                            kotlinTask.entityQualificationPrefix.convention(it)
                        }
                        qualificationPostfix?.also {
                            kotlinTask.entityQualificationPostfix.convention(it)
                        }
                        qualifiedProjectionPrefix?.also {
                            kotlinTask.entityQualifiedProjectionPrefix.convention(it)
                        }
                        qualifiedProjectionPostfix?.also {
                            kotlinTask.entityQualifiedProjectionPostfix.convention(it)
                        }
                        onPrefix?.also {
                            kotlinTask.entityOnPrefix.convention(it)
                        }
                        onPostfix?.also {
                            kotlinTask.entityOnPostfix.convention(it)
                        }
                    }
                    selectionExtension.valueOrNull?.apply {
                        selectionPrefix?.also {
                            kotlinTask.entitySelectionPrefix.convention(it)
                        }
                        selectionPostfix?.also {
                            kotlinTask.entitySelectionPostfix.convention(it)
                        }
                        selectionArgument?.also {
                            kotlinTask.entitySelectionArgument.convention(it)
                        }
                        queryPrefix?.also {
                            kotlinTask.entityQueryPrefix.convention(it)
                        }
                        queryPostfix?.also {
                            kotlinTask.entityQueryPostfix.convention(it)
                        }
                        queryArgument?.also {
                            kotlinTask.entityQueryArgument.convention(it)
                        }
                    }
                }
                kotlinExtension.implExtension.valueOrNull?.apply {
                    packageName?.also {
                        kotlinTask.implPackageName.convention(it)
                    }
                    prefix?.also {
                        kotlinTask.implPrefix.convention(it)
                    }
                    postfix?.also {
                        kotlinTask.implPostfix.convention(it)
                    }
                    internal?.also {
                        kotlinTask.implInternal.convention(it)
                    }
                    innerPrefix?.also {
                        kotlinTask.implInnerPrefix.convention(it)
                    }
                    innerPostfix?.also {
                        kotlinTask.implInnerPostfix.convention(it)
                    }
                }
                kotlinExtension.adapterExtension.valueOrNull?.apply {
                    ktorExtension.valueOrNull?.apply {
                        simpleEnabled?.also {
                            kotlinTask.adapterKtorSimpleEnabled.convention(it)
                        }
                        compositeEnabled?.also {
                            kotlinTask.adapterKtorCompositeEnabled.convention(it)
                        }
                        packageName?.also {
                            kotlinTask.adapterKtorPackageName.convention(it)
                        }
                        prefix?.also {
                            kotlinTask.adapterKtorPrefix.convention(it)
                        }
                        postfix?.also {
                            kotlinTask.adapterKtorPostfix.convention(it)
                        }
                    }
                }
                kotlinExtension.resolverExtension.valueOrNull?.apply {
                    enabled?.also {
                        kotlinTask.resolverEnabled.convention(it)
                    }
                    publisherEnabled?.also {
                        kotlinTask.resolverPublisherEnabled.convention(it)
                    }
                    packageName?.also {
                        kotlinTask.resolverPackageName.convention(it)
                    }
                    prefix?.also {
                        kotlinTask.resolverPrefix.convention(it)
                    }
                    postfix?.also {
                        kotlinTask.resolverPostfix.convention(it)
                    }
                    argument?.also {
                        kotlinTask.resolverArgument.convention(it)
                    }
                    toDoMessage?.also {
                        kotlinTask.resolverToDoMessage.convention(it)
                    }
                }
            }

            p.tasks.findByPath(COMPILE_KOTLIN_TASK_NAME)?.dependsOn(kotlinTask.path) ?: p.logger.warn(
                "$KOBBY: Please, configure '$COMPILE_KOTLIN_TASK_NAME' task to compile generated sources"
            )

            val outputDirectory = kotlinTask.outputDirectory.get().asFile
            (p.findProperty("sourceSets") as? SourceSetContainer)?.apply {
                outputDirectory.mkdirs()
                findByName("main")?.java?.srcDir(outputDirectory.path)
            }
        }
    }
}
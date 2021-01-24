package io.kobby

import io.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.kobby.task.KobbyKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskInstantiationException


/**
 * Created on 03.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Suppress("UnstableApiUsage")
class KobbyPlugin : Plugin<Project> {
    companion object {
        const val KOBBY = "kobby"
        const val COMPILE_KOTLIN_TASK_NAME = "compileKotlin"
    }

    private val <T> Lazy<T>.valueOrNull: T? get() = if (isInitialized()) value else null

    override fun apply(project: Project) {
        val extension = project.extensions.create(KOBBY, KobbyExtension::class.java)
        project.tasks.register(KOBBY)
        project.tasks.register(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java)

        project.afterEvaluate { p ->
            p.tasks.findByPath(COMPILE_KOTLIN_TASK_NAME)?.also { compileKotlinTask ->
                extension.kotlinExtension.valueOrNull?.also { kotlinExtension ->
                    val kotlinTask = p.tasks.named(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java).get()
                    if (kotlinExtension.enabled) {
                        extension.schemaExtension.valueOrNull?.apply {
                            local?.also {
                                kotlinTask.schemaFile.convention(p.layout.file(p.provider { it }))
                            }
                            directiveExtension.valueOrNull?.apply {
                                required?.also {
                                    kotlinTask.schemaDirectiveRequired.convention(it)
                                }
                                default?.also {
                                    kotlinTask.schemaDirectiveDefault.convention(it)
                                }
                            }
                        }

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
                            jacksonExtension.valueOrNull?.apply {
                                enabled?.also {
                                    kotlinTask.dtoJacksonEnabled.convention(it)
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
                            projectionExtension.valueOrNull?.apply {
                                prefix?.also {
                                    kotlinTask.entityProjectionPrefix.convention(it)
                                }
                                postfix?.also {
                                    kotlinTask.entityProjectionPostfix.convention(it)
                                }
                                argument?.also {
                                    kotlinTask.entityProjectionArgument.convention(it)
                                }
                                withPrefix?.also {
                                    kotlinTask.entityProjectionWithPrefix.convention(it)
                                }
                                withPostfix?.also {
                                    kotlinTask.entityProjectionWithPostfix.convention(it)
                                }
                                withoutPrefix?.also {
                                    kotlinTask.entityProjectionWithoutPrefix.convention(it)
                                }
                                withoutPostfix?.also {
                                    kotlinTask.entityProjectionWithoutPostfix.convention(it)
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
                        }
                    } else {
                        kotlinTask.enabled = false
                        p.logger.warn("$KOBBY: [${KobbyKotlin.TASK_NAME}] task disabled")
                    }
                }

                if (extension.kotlinExtension.valueOrNull?.enabled != false) {
                    val kobbyTask = p.tasks.getByName(KOBBY)
                    val kobbyKotlinTask = p.tasks.named(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java).get()

                    kobbyTask.dependsOn(kobbyKotlinTask.path)
                    compileKotlinTask.dependsOn(kobbyKotlinTask.path)

                    val outputDirectory = kobbyKotlinTask.outputDirectory.get().asFile
                    (p.findProperty("sourceSets") as? SourceSetContainer)?.apply {
                        outputDirectory.mkdirs()
                        findByName("main")?.java?.srcDir(outputDirectory.path)
                    }
                }
            } ?: extension.kotlinExtension.valueOrNull?.also { kotlinExtension ->
                if (kotlinExtension.enabled) {
                    throw TaskInstantiationException(
                        "$KOBBY: [${KobbyKotlin.TASK_NAME}] task cannot be created " +
                                "because [$COMPILE_KOTLIN_TASK_NAME] task is not configures. " +
                                "Configure Kotlin plugin please."
                    )
                }
            }
        }
    }
}
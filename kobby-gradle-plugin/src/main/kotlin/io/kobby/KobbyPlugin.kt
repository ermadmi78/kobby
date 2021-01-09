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

    override fun apply(project: Project) {
        val extension = project.extensions.create(KOBBY, KobbyExtension::class.java)
        project.tasks.register(KOBBY)
        project.tasks.register(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java)

        project.afterEvaluate { p ->
            p.tasks.findByPath(COMPILE_KOTLIN_TASK_NAME)?.also { compileKotlinTask ->
                extension.kotlinExtension?.also { kotlinExtension ->
                    val kobbyTask = p.tasks.getByName(KOBBY)
                    val kobbyKotlinTask = p.tasks.named(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java).get()
                    if (kotlinExtension.enabled) {
                        kobbyTask.dependsOn(kobbyKotlinTask.path)
                        compileKotlinTask.dependsOn(kobbyKotlinTask.path)

                        extension.schemaExtension?.local?.also {
                            kobbyKotlinTask.schemaFile.convention(p.layout.file(p.provider { it }))
                        }

                        kotlinExtension.scalars?.also {
                            kobbyKotlinTask.scalars.convention(PREDEFINED_SCALARS + it)
                        }
                        kotlinExtension.relativePackage?.also {
                            kobbyKotlinTask.relativePackage.convention(it)
                        }
                        kotlinExtension.packageName?.also {
                            kobbyKotlinTask.rootPackageName.convention(it)
                        }
                        kotlinExtension.outputDirectory?.also {
                            kobbyKotlinTask.outputDirectory.convention(it)
                        }
                        kotlinExtension.dtoExtension?.apply {
                            packageName?.also {
                                kobbyKotlinTask.dtoPackageName.convention(it)
                            }
                            prefix?.also {
                                kobbyKotlinTask.dtoPrefix.convention(it)
                            }
                            postfix?.also {
                                kobbyKotlinTask.dtoPostfix.convention(it)
                            }
                            jacksonized?.also {
                                kobbyKotlinTask.dtoJacksonized.convention(it)
                            }
                            builderExtension?.apply {
                                enabled?.also {
                                    kobbyKotlinTask.dtoBuilderEnabled.convention(it)
                                }
                                prefix?.also {
                                    kobbyKotlinTask.dtoBuilderPrefix.convention(it)
                                }
                                postfix?.also {
                                    kobbyKotlinTask.dtoBuilderPostfix.convention(it)
                                }
                            }
                        }
                        kotlinExtension.apiExtension?.apply {
                            enabled?.also {
                                kobbyKotlinTask.apiEnabled.convention(it)
                            }
                            packageName?.also {
                                kobbyKotlinTask.apiPackageName.convention(it)
                            }
                        }
                        kotlinExtension.implExtension?.apply {
                            packageName?.also {
                                kobbyKotlinTask.implPackageName.convention(it)
                            }
                            prefix?.also {
                                kobbyKotlinTask.implPrefix.convention(it)
                            }
                            postfix?.also {
                                kobbyKotlinTask.implPostfix.convention(it)
                            }
                        }
                    } else {
                        kobbyKotlinTask.enabled = false
                        p.logger.warn("$KOBBY: [${KobbyKotlin.TASK_NAME}] task disabled")
                    }
                }

                if (extension.kotlinExtension?.enabled != false) {
                    val kobbyKotlinTask = p.tasks.named(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java).get()
                    val outputDirectory = kobbyKotlinTask.outputDirectory.get().asFile
                    (p.findProperty("sourceSets") as? SourceSetContainer)?.apply {
                        outputDirectory.mkdirs()
                        findByName("main")?.java?.srcDir(outputDirectory.path)
                    }
                }
            } ?: extension.kotlinExtension?.also { kotlinExtension ->
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
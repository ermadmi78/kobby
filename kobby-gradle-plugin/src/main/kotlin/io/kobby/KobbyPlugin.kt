package io.kobby

import io.kobby.task.KobbyKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskInstantiationException


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
                    } else {
                        kobbyKotlinTask.enabled = false
                        p.logger.warn("$KOBBY: [${KobbyKotlin.TASK_NAME}] task disabled")
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

//        project.tasks.register(KOBBY) { kobbyTask ->
//            project.logger.info("$KOBBY: \"${KOBBY}\" task registered")
//            project.tasks.findByPath(COMPILE_KOTLIN_TASK_NAME)?.also { compileKotlinTask ->
//                if (extension.isKotlinConfigured() && !extension.kotlinExtension.enabled) {
//                    project.logger.warn("$KOBBY: \"${KobbyKotlin.TASK_NAME}\" task disabled")
//                    return@also
//                }
//
//                project.tasks.register(KobbyKotlin.TASK_NAME, KobbyKotlin::class.java) { kobbyKotlin ->
//                    kobbyTask.dependsOn(kobbyKotlin.path)
//                    compileKotlinTask.dependsOn(kobbyKotlin.path)
//                    project.logger.info("$KOBBY: \"${KobbyKotlin.TASK_NAME}\" task registered")
//                }
//            } ?: if (extension.isKotlinConfigured() && extension.kotlinExtension.enabled) {
//
//            }
//        }

        /*
        project.tasks.register(KOBBY) { task ->
            task.doLast { _ ->
                if (extension.isSchemaConfigured()) {
                    val schemaExtension = extension.schemaExtension
                    println("$KOBBY schema configured: $schemaExtension")
                }

                extension.schemaSearchTree?.also { tree ->
                    var counter = 0
                    tree.filter { it.isFile }.forEach {
                        counter++
                        println("$KOBBY schema found: $it")
                    }
                    if (counter == 0) {
                        println("$KOBBY schema search tree is empty")
                    }
                } ?: println("$KOBBY is not configured")
                if (extension.isKotlinConfigured()) {
                    val kotlinExtension = extension.kotlinExtension
                    println("$KOBBY kotlin configured: $kotlinExtension")

                    if (kotlinExtension.isDtoConfigured()) {
                        val dtoExtension = kotlinExtension.dtoExtension
                        println("$KOBBY kotlin dto configured: $dtoExtension")
                    }

                    if (kotlinExtension.isApiConfigured()) {
                        val apiExtension = kotlinExtension.apiExtension
                        println("$KOBBY kotlin api configured: $apiExtension")
                    }

                    if (kotlinExtension.isImplConfigured()) {
                        val implExtension = kotlinExtension.implExtension
                        println("$KOBBY kotlin impl configured: $implExtension")
                    }
                }
            }
            project.tasks.findByPath("compileKotlin")!!.dependsOn(task.path)
        }
        */
    }
}
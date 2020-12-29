package io.kobby.dsl

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Created on 03.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyDslPlugin : Plugin<Project> {
    companion object {
        const val DSL_TASK_NAME = "kobbyDSL"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(DSL_TASK_NAME, KobbyDslExtension::class.java)
        project.tasks.register(DSL_TASK_NAME) { task ->
            task.doLast { _ ->
                if (extension.isSchemaConfigured()) {
                    val schemaExtension = extension.schemaExtension
                    println("$DSL_TASK_NAME schema configured: $schemaExtension")
                }

                extension.schemaSearchTree?.also { tree ->
                    var counter = 0
                    tree.filter { it.isFile }.forEach {
                        counter++
                        println("$DSL_TASK_NAME schema found: $it")
                    }
                    if (counter == 0) {
                        println("$DSL_TASK_NAME schema search tree is empty")
                    }
                } ?: println("$DSL_TASK_NAME is not configured")
                if (extension.isKotlinConfigured()) {
                    val kotlinExtension = extension.kotlinExtension
                    println("$DSL_TASK_NAME kotlin configured: $kotlinExtension")

                    if (kotlinExtension.isDtoConfigured()) {
                        val dtoExtension = kotlinExtension.dtoExtension
                        println("$DSL_TASK_NAME kotlin dto configured: $dtoExtension")
                    }

                    if (kotlinExtension.isApiConfigured()) {
                        val apiExtension = kotlinExtension.apiExtension
                        println("$DSL_TASK_NAME kotlin api configured: $apiExtension")
                    }

                    if (kotlinExtension.isImplConfigured()) {
                        val implExtension = kotlinExtension.implExtension
                        println("$DSL_TASK_NAME kotlin impl configured: $implExtension")
                    }
                }
            }
            project.tasks.findByPath("compileKotlin")!!.dependsOn(task.path)
        }
    }
}
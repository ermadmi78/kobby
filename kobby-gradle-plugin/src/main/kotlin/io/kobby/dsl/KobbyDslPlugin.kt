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
            task.doLast {
                extension.source?.also { tree ->
                    var counter = 0
                    tree.filter { it.isFile }.forEach {
                        counter++
                        println("$DSL_TASK_NAME source: $it")
                    }
                    if (counter == 0) {
                        println("$DSL_TASK_NAME source tree is empty")
                    }
                } ?: println("$DSL_TASK_NAME is not configured")
            }
            project.tasks.findByPath("compileKotlin")!!.dependsOn(task.path)
        }
    }
}
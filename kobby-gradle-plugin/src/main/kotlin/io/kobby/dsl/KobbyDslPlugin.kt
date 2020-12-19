package io.kobby.dsl

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Created on 03.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyDslPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("kobbyDSL") {
            it.doLast {
                println("kobbyDSL !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            }
            project.tasks.findByPath("compileKotlin")!!.dependsOn("kobbyDSL")
        }
    }
}
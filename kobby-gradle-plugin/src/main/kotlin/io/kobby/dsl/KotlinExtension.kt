package io.kobby.dsl

import org.gradle.api.Project

/**
 * Kobby Kotlin Gradle Plugin project extension
 */
fun Project.kobbyDSL(configure: KobbyDslExtension.() -> Unit) =
    extensions.configure(KobbyDslExtension::class.java, configure)
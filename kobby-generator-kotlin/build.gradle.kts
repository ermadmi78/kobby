import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kobby Kotlin DSL Generator"

val graphQLJavaVersion: String by project
val kotlinPoetVersion: String by project

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 */
val shadowImplementation by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    api(project(":kobby-model"))
    shadowImplementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
}

plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

tasks {
    val shadowJarTask = named("shadowJar", ShadowJar::class.java)
    val relocateShadowJar = register("relocateShadowJar", ConfigureShadowRelocation::class.java) {
        target = shadowJarTask.get()
        prefix = "io.kobby.generator.kotlin.shadow"
    }
    shadowJarTask.configure {
        dependsOn(relocateShadowJar)
        archiveClassifier.set("")
        mergeServiceFiles()
        minimize()
        configurations = listOf(shadowImplementation)
    }
}
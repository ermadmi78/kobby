import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kobby Model"

val graphQLJavaVersion: String by project

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 */
val shadowImplementation: Configuration by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    shadowImplementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
}

plugins {
    id("com.github.johnrengelman.shadow")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        mergeServiceFiles()
        minimize()
        configurations = listOf(shadowImplementation)

        isEnableRelocation = true
        relocationPrefix = "io.github.ermadmi78.kobby.model.shadow"
    }
}

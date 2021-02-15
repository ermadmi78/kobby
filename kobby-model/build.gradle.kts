import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
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
    val shadowJarTask = named("shadowJar", ShadowJar::class.java)
    val relocateShadowJar = register("relocateShadowJar", ConfigureShadowRelocation::class.java) {
        target = shadowJarTask.get()
        prefix = "io.github.ermadmi78.kobby.model.shadow"
    }
    shadowJarTask.configure {
        dependsOn(relocateShadowJar)
        archiveClassifier.set("")
        mergeServiceFiles()
        minimize()
        configurations = listOf(shadowImplementation)
    }
}

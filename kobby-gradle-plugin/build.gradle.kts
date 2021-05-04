val title = "Kobby - GraphQL Kotlin DSL Client Gradle Plugin"
description = title

dependencies {
    implementation(project(":kobby-generator-kotlin"))
}

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

gradlePlugin {
    plugins {
        create("KobbyPlugin") {
            id = "io.github.ermadmi78.kobby"
            implementationClass = "io.github.ermadmi78.kobby.KobbyPlugin"
            displayName = title
            description = "Kobby is a codegen plugin for Kotlin DSL Client from GraphQL schema"
        }
    }
}

pluginBundle {
    website = "https://github.com/ermadmi78/kobby"
    vcsUrl = "https://github.com/ermadmi78/kobby"
    tags = listOf("graphql", "kotlin", "graphql-client", "dsl")
}

tasks {
    publishPlugins {
        doFirst {
            System.setProperty("gradle.publish.key", System.getenv("PLUGIN_PORTAL_KEY"))
            System.setProperty("gradle.publish.secret", System.getenv("PLUGIN_PORTAL_SECRET"))
        }
    }
}
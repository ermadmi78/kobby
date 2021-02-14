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
            description = "Kobby is a Gradle plugin for generating Kotlin DSL Client by GraphQL schema"
        }
    }
}

pluginBundle {
    website = "https://github.com/ermadmi78/kobby"
    vcsUrl = "https://github.com/ermadmi78/kobby"
    tags = listOf("graphql", "kotlin", "graphql-client", "dsl")
}

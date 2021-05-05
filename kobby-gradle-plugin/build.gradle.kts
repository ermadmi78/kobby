val title = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema"
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
            description = title
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
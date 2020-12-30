val title = "Kobby Gradle Plugin"
description = title

dependencies {
    implementation(project(":kobby-generator-kotlin"))
}

plugins {
    `java-gradle-plugin`
    //id("com.gradle.plugin-publish")
}

gradlePlugin {
    plugins {
        create("KobbyPlugin") {
            id = "io.kobby"
            implementationClass = "io.kobby.KobbyPlugin"
            displayName = title
            description = "Kobby is a generator of smart client API by GraphQL schema"
        }
    }
}

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
        create("kobbyDSLPlugin") {
            id = "io.kobby"
            implementationClass = "io.kobby.dsl.KobbyDslPlugin"
            displayName = title
        }
    }
}

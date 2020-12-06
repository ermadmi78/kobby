description = "Kobby Gradle Plugin"

dependencies {
    implementation(project(":kobby-generator-kotlin"))
}

plugins {
    `java-gradle-plugin`
    //id("com.gradle.plugin-publish")
}

gradlePlugin {
}
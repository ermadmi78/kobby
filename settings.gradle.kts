rootProject.name = "kobby"

pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val pluginPublishVersion: String by settings
    val pluginShadowVersion: String by settings
    val nexusPublishVersion: String by settings
    val mavenPluginDevelopmentVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.gradle.plugin-publish") version pluginPublishVersion
        id("com.github.johnrengelman.shadow") version pluginShadowVersion
        id("io.github.gradle-nexus.publish-plugin") version nexusPublishVersion
        id("de.benediktritter.maven-plugin-development") version mavenPluginDevelopmentVersion
    }
}

include(":kobby-model")
include(":kobby-generator-kotlin")
include(":kobby-gradle-plugin")
include(":kobby-maven-plugin")


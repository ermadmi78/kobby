@file:Suppress("UnstableApiUsage")

rootProject.name = "kobby"

pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val pluginPublishVersion: String by settings
    val pluginShadowVersion: String by settings
    val nexusPublishVersion: String by settings
    val nexusStagingVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.gradle.plugin-publish") version pluginPublishVersion
        id("com.github.johnrengelman.shadow") version pluginShadowVersion
        id("de.marcphilipp.nexus-publish") version nexusPublishVersion
        id("io.codearte.nexus-staging") version nexusStagingVersion
    }
}

include(":kobby-model")
include(":kobby-generator-kotlin")
include(":kobby-gradle-plugin")
include(":kobby-maven-plugin")


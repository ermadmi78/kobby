@file:Suppress("UnstableApiUsage")

rootProject.name = "kobby"

pluginManagement {
    val kotlinVersion: String by settings
    val pluginPublishVersion: String by settings
    val pluginShadowVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.gradle.plugin-publish") version pluginPublishVersion
        id("com.github.johnrengelman.shadow") version pluginShadowVersion
    }
}

include(":kobby-model")
include(":kobby-generator-kotlin")
include(":kobby-gradle-plugin")
include(":kobby-maven-plugin")


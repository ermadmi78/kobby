rootProject.name = "kobby"

pluginManagement {
    val kotlinVersion: String by settings

    plugins{
        kotlin("jvm") version kotlinVersion
    }
}

include(":kobby-generator-kotlin")
include(":kobby-gradle-plugin")
include(":kobby-maven-plugin")


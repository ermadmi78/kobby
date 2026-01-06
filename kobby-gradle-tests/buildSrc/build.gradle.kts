import java.nio.file.Paths
import java.util.*

plugins {
    `kotlin-dsl`
}

val props = Properties().apply {
    Paths.get(projectDir.parentFile.parent, "gradle.properties").toFile()
        .inputStream().use { load(it) }
}

val snapshotKobbyVersion = props["version"]

dependencies {
    implementation(testLibs.ktor.server.netty)
    implementation(testLibs.ktor.server.websockets)
    implementation(testLibs.ktor.server.cors)

    implementation(testLibs.graphql.kotlin)
    implementation(testLibs.extended.scalars) {
        exclude(group = "com.graphql-java", module = "graphql-java")
    }

    implementation(testLibs.kotlin.gradle.plugin)

    // firstly run `publishToMavenLocal`
    implementation("io.github.ermadmi78:kobby-gradle-plugin:$snapshotKobbyVersion")
}

kotlin {
    jvmToolchain(props.getProperty("kotlinJdkVersion")!!.toInt())
}

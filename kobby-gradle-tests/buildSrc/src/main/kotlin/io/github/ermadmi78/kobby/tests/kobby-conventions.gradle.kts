import io.github.ermadmi78.kobby.server.WebServer
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import java.nio.file.Paths
import java.util.Properties
import kotlin.apply

plugins {
    kotlin("jvm")
    id("io.github.ermadmi78.kobby")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks {
    test {
        testLogging.showStandardStreams = true
        testLogging.exceptionFormat = FULL
        useJUnitPlatform()
    }
}

dependencies {
    testImplementation(testLibs.bundles.slf4j)
    testImplementation(testLibs.bundles.kotest)
}

val sharedServerServiceProvider = gradle.sharedServices.registerIfAbsent("sharedServer", WebServer::class.java)

tasks.named("test", Test::class.java) {
    usesService(sharedServerServiceProvider)

    val serverUrl = sharedServerServiceProvider.get().getServerUrl()
    systemProperty("kobby-gradle-tests.server-url", serverUrl)
}

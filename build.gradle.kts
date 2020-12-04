import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin DSL over GraphQL schema generator"

plugins {
    kotlin("jvm")
}

allprojects {
    buildscript {
        repositories {
            mavenLocal()
            jcenter()
            mavenCentral()
        }
    }

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }
}

subprojects {
    val kotlinJvmVersion: String by project
    val kotlinVersion: String by project
    val kotestVersion: String by project

    apply(plugin = "kotlin")

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = kotlinJvmVersion
                freeCompilerArgs = listOf("-Xjsr305=strict")
            }
        }

        test {
            testLogging.showStandardStreams = true
            testLogging.exceptionFormat = FULL
            useJUnitPlatform()
        }
    }

    dependencies {
        implementation(kotlin("stdlib", kotlinVersion))
        implementation(kotlin("reflect", kotlinVersion))
        testImplementation(kotlin("test", kotlinVersion))
        testImplementation(kotlin("test-junit5", kotlinVersion))

        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.3.72")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.72")
            force("org.jetbrains.kotlin:kotlin-reflect:1.3.72")
        }
    }
}

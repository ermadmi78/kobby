import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin DSL over GraphQL schema generator"

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
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

    val currentProject = this

    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    @Suppress("UnstableApiUsage")
    java {
        withJavadocJar()
        withSourcesJar()
    }

    println("${currentProject.group}:${currentProject.name}")

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

        publishing {
            repositories {
                mavenLocal()
            }
            publications {
                withType<MavenPublication> {
                    pom {
                        name.set("${currentProject.group}:${currentProject.name}")
                        url.set("https://github.com/ermadmi78/kobby")
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                name.set("Dmitry Ermakov")
                                email.set("ermadmi78@gmail.com")
                                url.set("https://www.linkedin.com/in/ermadmi78")
                            }
                        }
                        scm {
                            connection.set("scm:git:git@github.com:ermadmi78/kobby.git")
                            developerConnection.set("scm:git:git@github.com:ermadmi78/kobby.git")
                            url.set("https://github.com/ermadmi78/kobby")
                        }

                        // child projects need to be evaluated before their description can be read
                        val mavenPom = this
                        afterEvaluate {
                            mavenPom.description.set(currentProject.description)
                        }
                    }
                }
                create<MavenPublication>("pluginMaven") {
                    if (currentProject.name != "kobby-gradle-plugin") {
                        from(currentProject.components["java"])
                    }
                }
            }
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
}

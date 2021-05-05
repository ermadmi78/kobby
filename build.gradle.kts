import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration
import java.time.Instant

description = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema"
extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") apply false
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
    buildscript {
        repositories {
            mavenLocal()
            mavenCentral()
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    val kotlinJvmVersion: String by project
    val kotlinVersion: String by project
    val kotestVersion: String by project

    val currentProject = this

    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    println("${currentProject.group}:${currentProject.name}")

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = kotlinJvmVersion
                freeCompilerArgs = listOf("-Xjsr305=strict")
            }
        }

        jar {
            manifest {
                attributes["Built-By"] = "https://github.com/ermadmi78"
                attributes["Build-Jdk"] =
                    "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
                attributes["Build-Timestamp"] = Instant.now().toString()
                attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
                attributes["Implementation-Title"] = currentProject.name
                attributes["Implementation-Version"] = project.version
            }
        }

        test {
            testLogging.showStandardStreams = true
            testLogging.exceptionFormat = FULL
            useJUnitPlatform()
        }

        // published artifacts
        val jarComponent = currentProject.components.getByName("java")
        val sourcesJar by registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        val dokka = named("dokkaJavadoc", DokkaTask::class)
        val javadocJar by registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from("$buildDir/dokka/javadoc")
            dependsOn(dokka)
        }
        publishing {
            repositories {
                if (!(rootProject.extra["isReleaseVersion"] as Boolean)) {
                    mavenLocal()
                }
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
                create<MavenPublication>("mavenJava") {
                    from(jarComponent)
                    // no need to publish sources or javadocs for SNAPSHOT builds
                    if (rootProject.extra["isReleaseVersion"] as Boolean) {
                        artifact(sourcesJar.get())
                        artifact(javadocJar.get())
                    }
                }
            }
        }
        signing {
            setRequired {
                rootProject.extra["isReleaseVersion"] as Boolean &&
                        (gradle.taskGraph.hasTask("publish") || gradle.taskGraph.hasTask("publishPlugins"))
            }
            val signingKey: String? = System.getenv("GPG_SECRET")
            val signingPassword: String? = System.getenv("GPG_PASSPHRASE")
            @Suppress("UnstableApiUsage")
            useInMemoryPgpKeys(signingKey, signingPassword)

            sign(publishing.publications)
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

tasks {
    jar {
        enabled = false
    }
    if (rootProject.extra["isReleaseVersion"] as Boolean) {
        nexusPublishing {
            repositories {
                sonatype {
                    username.set(System.getenv("SONATYPE_USERNAME"))
                    password.set(System.getenv("SONATYPE_PASSWORD"))
                }
            }

            transitionCheckOptions {
                maxRetries.set(60)
                delayBetween.set(Duration.ofMillis(5000))
            }
        }
    }
}
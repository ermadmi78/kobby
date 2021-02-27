import de.marcphilipp.gradle.nexus.NexusPublishExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant

description = "Kotlin DSL over GraphQL schema generator"
extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") apply false
    `java-library`
    `maven-publish`
    signing
    id("de.marcphilipp.nexus-publish")
    id("io.codearte.nexus-staging")
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

    if (rootProject.extra["isReleaseVersion"] as Boolean) {
        apply(plugin = "de.marcphilipp.nexus-publish")

        configure<NexusPublishExtension> {
            repositories {
                sonatype {
                    username.set(System.getenv("SONATYPE_USERNAME"))
                    password.set(System.getenv("SONATYPE_PASSWORD"))
                }
            }
        }
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
        val dokka by getting(DokkaTask::class) {
            outputFormat = "javadoc"
            outputDirectory = "$buildDir/javadoc"
        }
        val javadocJar by registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from("$buildDir/javadoc")
            dependsOn(dokka.path)
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
    nexusStaging {
        username = System.getenv("SONATYPE_USERNAME")
        password = System.getenv("SONATYPE_PASSWORD")
        packageGroup = rootProject.group.toString()
        numberOfRetries = 60
        delayBetweenRetriesInMillis = 5000
    }
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}
group = "io.kobby"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation(project(":kobby-core"))
    testImplementation(kotlin("test-junit5"))
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
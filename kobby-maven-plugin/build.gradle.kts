description = "Kobby Maven Plugin"

plugins {
    id("de.benediktritter.maven-plugin-development")
}

tasks {
    named("generateMavenPluginDescriptor") {
        dependsOn(":kobby-model:shadowJar")
    }

    publishing {
        publications {
            val mavenPublication = findByName("mavenJava") as? MavenPublication
            mavenPublication?.pom {
                packaging = "maven-plugin"
            }
        }
    }
}

val mavenCoreVersion: String by project
val mavenPluginApiVersion: String by project
val mavenProjectVersion: String by project
val mavenPluginAnnotationVersion: String by project

dependencies {
    implementation(project(":kobby-generator-kotlin"))
    implementation("org.apache.maven:maven-core:$mavenCoreVersion")
    implementation("org.apache.maven:maven-plugin-api:$mavenPluginApiVersion")
    implementation("org.apache.maven:maven-project:$mavenProjectVersion")
    implementation(
        "org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginAnnotationVersion"
    )
}
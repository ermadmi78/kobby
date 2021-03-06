description = "Kobby Kotlin DSL Generator"

val kotlinPoetVersion: String by project
val graphQLJavaVersion: String by project

dependencies {
    api(project(":kobby-model"))
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    testImplementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
}
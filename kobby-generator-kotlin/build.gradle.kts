description = "Kobby Kotlin DSL Generator"

val graphQLJavaVersion: String by project
val kotlinPoetVersion: String by project

dependencies {
    implementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
    api("com.squareup:kotlinpoet:$kotlinPoetVersion")
}
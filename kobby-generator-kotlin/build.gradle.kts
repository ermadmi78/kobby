description = "Kobby Kotlin DSL Generator"

val graphQLJavaVersion: String by project
val kotlinPoetVersion: String by project

dependencies {
    implementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
}
description = "Kobby DSL generator"

val graphQLJavaVersion: String by project
val kotlinPoetVersion: String by project

dependencies {
    implementation("com.graphql-java:graphql-java:$graphQLJavaVersion")
    api("com.squareup:kotlinpoet:$kotlinPoetVersion")
}
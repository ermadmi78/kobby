description = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema"

dependencies {
    implementation(project(":kobby-generator-kotlin"))
}

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

gradlePlugin {
    plugins {
        create("KobbyPlugin") {
            id = "io.github.ermadmi78.kobby"
            implementationClass = "io.github.ermadmi78.kobby.KobbyPlugin"
            displayName = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema"
            description = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema. " +
                    "The generated DSL supports execution of complex GraphQL queries, mutation and subscriptions " +
                    "in Kotlin with syntax similar to native GraphQL syntax. Moreover, you can customize " +
                    "generated DSL by means of GraphQL schema directives and Kotlin extension functions."
        }
    }
}

pluginBundle {
    website = "https://github.com/ermadmi78/kobby"
    vcsUrl = "https://github.com/ermadmi78/kobby"
    tags = listOf(
        "graphql", "kotlin", "client", "dsl", "graphql-kotlin", "graphql-client",
        "codegeneration", "code-generation", "codegen", "generate"
    )
    description = "Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema. " +
            "The generated DSL supports execution of complex GraphQL queries, mutation and subscriptions " +
            "in Kotlin with syntax similar to native GraphQL syntax. Moreover, you can customize " +
            "generated DSL by means of GraphQL schema directives and Kotlin extension functions."
}

tasks {
    publishPlugins {
        doFirst {
            System.setProperty("gradle.publish.key", System.getenv("PLUGIN_PORTAL_KEY"))
            System.setProperty("gradle.publish.secret", System.getenv("PLUGIN_PORTAL_SECRET"))
        }
    }
    test {
        dependsOn(":resolveIntegrationTestDependencies")

        val testKotlinVersion: String by project
        val testJacksonVersion: String by project
        val testKtorVersion: String by project
        val testKickstartGraphqlJavaToolsVersion: String by project
        val testReactiveStreamsVersion: String by project

        systemProperty("testKotlinVersion", testKotlinVersion)
        systemProperty("testJacksonVersion", testJacksonVersion)
        systemProperty("testKtorVersion", testKtorVersion)
        systemProperty("testKickstartGraphqlJavaToolsVersion", testKickstartGraphqlJavaToolsVersion)
        systemProperty("testReactiveStreamsVersion", testReactiveStreamsVersion)
    }
    named("pluginUnderTestMetadata") {
        dependsOn(":kobby-model:shadowJar")
    }
}

val springVersion: String by project
dependencies {
    testImplementation("org.springframework:spring-core:$springVersion")
}
import java.nio.file.Files

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

val testcasesTaskName = "kobbyTestcasesActualize"
val testcasesActualizeProperty = "kobby.testcases.actualize"
val testcasesDirProperty = "kobby.testcases.dir"

tasks {
    publishPlugins {
        doFirst {
            System.setProperty("gradle.publish.key", System.getenv("PLUGIN_PORTAL_KEY"))
            System.setProperty("gradle.publish.secret", System.getenv("PLUGIN_PORTAL_SECRET"))
        }
    }

    test {
        dependsOn(":resolveIntegrationTestDependencies")

        if (System.getProperty(testcasesActualizeProperty)?.trim() == "true") {
            val testcasesDir = Files.createTempDirectory("kobby_kotlin_testcases_").toString()
            System.setProperty(testcasesDirProperty, testcasesDir)
            systemProperty(testcasesDirProperty, testcasesDir)
            finalizedBy(testcasesTaskName)

            println()
            println("Task $testcasesTaskName is configured: $testcasesDir")
            println()
        } else {
            System.getProperty(testcasesDirProperty)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.takeIf { it != "null" }
                ?.let {
                    systemProperty(testcasesDirProperty, it)
                }
        }

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

    register(testcasesTaskName) {
        doFirst {
            val testcasesDir = System.getProperty(testcasesDirProperty)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.takeIf { it != "null" }
                ?: error("Please, configure '$testcasesDirProperty' property")

            println()
            println("**************************************************************")
            println("Actualize testcases from: $testcasesDir")
            println("**************************************************************")

            val expectedMap: Map<String, File> = mutableMapOf<String, File>().also { map ->
                val expectedTestcasesDir = projectDir.resolve(
                    "src/test/resources/io/github/ermadmi78/kobby/testcases"
                )
                expectedTestcasesDir.list().forEach { testcase ->
                    map[testcase] = expectedTestcasesDir.resolve(testcase).resolve("expected")
                }
            }


            val actualTestcasesDir = file(testcasesDir)
            actualTestcasesDir.list().sorted().forEach { testcase ->
                println()
                println("Actualize testcase: $testcase")

                val expectedDir = expectedMap[testcase]
                if (expectedDir == null) {
                    println("Cannot find expected directory for testcase: $testcase")
                } else {
                    delete(expectedDir)
                    mkdir(expectedDir)
                    copy {
                        from(
                            actualTestcasesDir
                                .resolve(testcase)
                                .resolve("build/generated/sources/kobby/main/kotlin")
                        )
                        include("**/*.kt")
                        into(expectedDir)
                        rename {
                            "$it.txt"
                        }
                    }

                    println("Actualized successfully")
                }
            }

            println()
            println("**************************************************************")
            println()
        }
    }
}

val springVersion: String by project
dependencies {
    testImplementation("org.springframework:spring-core:$springVersion")
}
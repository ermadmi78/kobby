[![License](https://img.shields.io/badge/License-Apache%202.0-brightgreen)](https://github.com/ermadmi78/kobby/blob/main/LICENSE)
[![Plugin Portal](https://img.shields.io/badge/Plugin%20Portal-v1.4.1-orange)](https://plugins.gradle.org/plugin/io.github.ermadmi78.kobby)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-v1.4.1-orange)](https://search.maven.org/artifact/io.github.ermadmi78/kobby-maven-plugin)
[![Discussions](https://img.shields.io/badge/Discussions-On%20GitHub-blue)](https://github.com/ermadmi78/kobby/discussions)

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)](https://github.com/ermadmi78/kobby/wiki)

Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema. The generated DSL supports execution of complex
GraphQL queries, mutations and subscriptions in Kotlin with syntax similar to native GraphQL syntax. Moreover, you can
customize generated DSL by means of GraphQL schema directives and Kotlin extension functions.

### Documentation

* [Kobby Documentation](https://github.com/ermadmi78/kobby/wiki)

### Tutorials

* [How to generate Kotlin DSL Client by GraphQL schema](https://blog.kotlin-academy.com/how-to-generate-kotlin-dsl-client-by-graphql-schema-707fd0c55284)
* [Kobby Gradle Tutorial](https://github.com/ermadmi78/kobby-gradle-tutorial)
* [Kobby Maven Tutorial](https://github.com/ermadmi78/kobby-maven-tutorial)

### Requirements

* Gradle at least version 7.0 is required.
* Maven at least version 3.6.3 is required.
* Kotlin at least version 1.5 is required to compile generated client DSL.
* 1.5.0 <= Ktor version < 2.0.0 is required to generate default adapters.

### Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md)

### Gradle

```kotlin
plugins {
    id("io.github.ermadmi78.kobby") version "1.4.1"
}
```

### Maven

```xml

<build>
    <plugins>
        <plugin>
            <groupId>io.github.ermadmi78</groupId>
            <artifactId>kobby-maven-plugin</artifactId>
            <version>1.4.1</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate-kotlin</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Define your GraphQL schema

Put your GraphQL schema file in the project resources with `graphqls` extension. For example, let define
[cinema.graphqls](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-api/src/main/resources/io/github/ermadmi78/kobby/cinema/api/cinema.graphqls)
schema file and put it in `src/main/resources/io/github/ermadmi78/kobby/cinema/api/`

### Configure Kobby Gradle plugin

* Add Kobby plugin to your `build.gradle.kts`, to generate Kotlin DSL.
* Configure Kotlin data types for scalars, defined in the GraphQL schema (more details about the scalar mapping
  see [here](https://github.com/ermadmi78/kobby/wiki/Gradle-GraphQL-Scalar-Mapping)).
* Add Jackson dependency to generate Jackson annotations for DTO classes.
* Add Kotlin plugin to your `build.gradle.kts` to compile generated DSL.

```kotlin
import io.github.ermadmi78.kobby.kobby

description = "Cinema API Example"

plugins {
    kotlin("jvm")
    `java-library`
    id("io.github.ermadmi78.kobby")
}

kobby {
    kotlin {
        scalars = mapOf(
            "Date" to typeOf("java.time", "LocalDate"),
            "JSON" to typeMap.parameterize(typeString, typeAny.nullable())
        )
    }
}

val jacksonVersion: String by project
dependencies {
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
}
```

### Generate Kotlin DSL Client by your GraphQL schema

Execute `gradle build` command to generate Kotlin DSL Client. The entry point of the DSL will be placed in
the `cinema.kt` file (name of the DSL entry point file is the same as the name of GraphQL schema file):

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/cinema_api.png)](https://github.com/ermadmi78/kobby/wiki/Gradle-DSL-entry-point-configuration)

More details about the entry point configuration see
[here](https://github.com/ermadmi78/kobby/wiki/Gradle-DSL-entry-point-configuration).

### Instantiate DSL Context

The `cinema.kt` file will contain the `cinemaContextOf` builder function, which creates instance of the `CinemaContext`
interface - the entry point of the generated DSL. Note that the prefixes of the builder function, adapter and context
interfaces are the same as the name of the GraphQL schema file.

```kotlin
fun cinemaContextOf(adapter: CinemaAdapter): CinemaContext = CinemaContextImpl(adapter)
```

More details about the DSL context see
[here](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

We have to pass instance of the `CinemaAdapter` interface to the `cinemaContextOf` function to create instance of
the `CinemaContext` interface. By default, Kobby does not generate any adapter implementations. But we can ask Kobby to
generate default [Ktor](https://ktor.io/) implementations of the `CinemaAdapter` interface.

To do this just add `io.ktor:ktor-client-cio` dependency to you project:

```kotlin
val jacksonVersion: String by project
val ktorVersion: String by project
dependencies {
    // Add this dependency to enable Jackson annotation generation in DTO classes by Kobby
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")

    // Add this dependency to enable default Ktor adapters generation
    compileOnly("io.ktor:ktor-client-cio:$ktorVersion")
}
```

Rebuild the project, and Kobby will generate classes `CinemaSimpleKtorAdapter` and `CinemaCompositeKtorAdapter`
in subpackage `adapter.ktor`. The prefix `<Cinema>` by default is the schema name.

The `CinemaSimpleKtorAdapter` is simple to configure, but it does not support GraphQL subscriptions - so we can use only
queries and mutations:

```kotlin
val client = HttpClient {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            registerModule(JavaTimeModule())
            // Force Jackson to serialize dates as String
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

val context = cinemaContextOf(CinemaSimpleKtorAdapter(client, "http://localhost:8080/graphql"))
```

The `CinemaCompositeKtorAdapter` is more difficult to configure, but it supports all types of GraphQL operations,
including subscriptions:

```kotlin
val client = HttpClient {
    install(WebSockets)
}

val mapper = jacksonObjectMapper()
    .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
    .registerModule(JavaTimeModule())
    // Force Jackson to serialize dates as String
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val context = cinemaContextOf(
    CinemaCompositeKtorAdapter(
        client,
        "http://localhost:8080/graphql",
        "ws://localhost:8080/subscriptions",
        object : CinemaMapper {
            override fun serialize(value: Any): String =
                mapper.writeValueAsString(value)

            override fun <T : Any> deserialize(content: String, contentType: KClass<T>): T =
                mapper.readValue(content, contentType.java)
        }
    )
)
```

See [here](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-kotlin-client/src/main/kotlin/io/github/ermadmi78/kobby/cinema/kotlin/client/application.kt)
full example of adapters configuration. Note that you are not required to use the default adapters. You can write your
own adapter implementation. For example,
see [here](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-server/src/test/kotlin/io/github/ermadmi78/kobby/cinema/server/CinemaTestAdapter.kt)
adapter implementation for Spring Boot integration tests.

### Ok, we are ready to execute GraphQL queries by means of generated Kotlin DSL Client

#### Simple query

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

You can see more details about GraphQL queries support
[here](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

#### Simple mutation

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/mutation.png)](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

#### Simple subscription

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/subscription.png)](https://github.com/ermadmi78/kobby/wiki/Support-for-GraphQL-subscriptions)

You can see more details about GraphQL subscriptions support
[here](https://github.com/ermadmi78/kobby/wiki/Support-for-GraphQL-subscriptions)

#### We can download a complex graph of objects by means of our Kotlin DSL

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/complex_query.png)](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

#### GraphQL's unions and interfaces are supported too

[![alt text](https://github.com/ermadmi78/kobby/blob/main/images/union_query.png)](https://github.com/ermadmi78/kobby/wiki/Support-for-GraphQL-Abstract-Data-Types)

You can see more details about GraphQL abstract data types support
[here](https://github.com/ermadmi78/kobby/wiki/Support-for-GraphQL-Abstract-Data-Types)

### License

[Apache License Version 2.0](https://github.com/ermadmi78/kobby/blob/main/LICENSE)
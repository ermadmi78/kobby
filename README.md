# Overview

## Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)

The generated DSL supports execution of complex GraphQL queries, mutations and subscriptions in Kotlin with syntax 
similar to native GraphQL syntax. Moreover, you can customize generated DSL by means of GraphQL schema directives 
and Kotlin extension functions.

## Gradle

```kotlin
plugins {
    id("io.github.ermadmi78.kobby") version "1.0.0-beta.11"
}
```

Gradle's usage example see [here](https://github.com/ermadmi78/kobby-gradle-example).
Kotlin at least version 1.5 is required to compile generated DSL.

## Maven

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.ermadmi78</groupId>
            <artifactId>kobby-maven-plugin</artifactId>
            <version>1.0.0-beta.11</version>
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

Maven usage example see [here](https://github.com/ermadmi78/kobby-maven-example).
Kotlin at least version 1.5 is required to compile generated DSL.

# License

[Apache License Version 2.0](https://github.com/ermadmi78/kobby/blob/main/LICENSE)

# Documentation

See documentation in the project [Wiki](https://github.com/ermadmi78/kobby/wiki).

## Define your GraphQL schema

Put your GraphQL schema in project resources with `graphqls` extension. For example, let define
[cinema.graphqls](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-api/src/main/resources/io/github/ermadmi78/kobby/cinema/api/cinema.graphqls)
schema and put it in `src/main/resources/io/github/ermadmi78/kobby/cinema/api/`

## Configure Kobby Gradle plugin

* Add Kobby plugin to your `build.gradle.kts`, to generate Kotlin DSL.
* Configure Kotlin data types for scalars, defined in GraphQL schema (more about scalar mapping see [here](https://github.com/ermadmi78/kobby/wiki/Gradle-GraphQL-Scalar-Mapping)).
* Add Jackson dependency to generate Jackson annotations for DTO classes.
* Add Kotlin plugin to your `build.gradle.kts`, to compile generated DSL.

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

## Generate Kotlin DSL from your GraphQL schema

Execute `gradle build` command to generate Kotlin DSL. Entry point of DSL will be placed in `cinema.kt` file
(name of DSL entry point file is same as name of GraphQL schema file):

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/cinema_api.png)

More about entry point configuration see
[here](https://github.com/ermadmi78/kobby/wiki/Gradle-DSL-entry-point-configuration).

## Configure DSL Context

In `cinema.kt` will be placed `cinemaContextOf` builder function, that creates `CinemaContext` - the entry point of
generated DSL. Note, that prefixes of builder function, adapter and context interfaces are same as name of GraphQL
schema file.

```kotlin
public fun cinemaContextOf(adapter: CinemaAdapter): CinemaContext = CinemaContextImpl(adapter)

public interface CinemaContext {
    public suspend fun query(__projection: QueryProjection.() -> Unit): Query

    public suspend fun mutation(__projection: MutationProjection.() -> Unit): Mutation

    public fun subscription(__projection: SubscriptionProjection.() -> Unit):
            CinemaSubscriber<Subscription>
}

public fun interface CinemaSubscriber<T> {
    public suspend fun subscribe(block: suspend CinemaReceiver<T>.() -> Unit): Unit
}

@CinemaDSL
public fun interface CinemaReceiver<out T> {
    public suspend fun receive(): T
}

public interface CinemaAdapter {
    public suspend fun executeQuery(query: String, variables: Map<String, Any?>): QueryDto = throw
    NotImplementedError("Adapter function executeQuery is not implemented")

    public suspend fun executeMutation(query: String, variables: Map<String, Any?>): MutationDto =
        throw NotImplementedError("Adapter function executeMutation is not implemented")

    public suspend fun executeSubscription(
        query: String,
        variables: Map<String, Any?>,
        block: suspend CinemaReceiver<SubscriptionDto>.() -> Unit
    ): Unit = throw NotImplementedError("Adapter function executeSubscription is not implemented")
}
```

We have to pass instance of `CinemaAdapter` interface to `cinemaContextOf` function to create `CinemaContext`.
By default, the `CinemaAdapter` is not generated by Kobby Plugin, but we can ask Kobby to generate default
[Ktor](https://ktor.io/) implementations of `CinemaAdapter`.

**Explicit way to configure adapter generation** is to use `kobby` extension. 
Add `adapter` section to `kobby` extension in your `build.gradle.kts`:
```kotlin
kobby {
    kotlin {
        scalars = mapOf(
            "Date" to typeOf("java.time", "LocalDate"),
            "JSON" to typeMap.parameterize(typeString, typeAny.nullable())
        )
        adapter {
            ktor {
                simpleEnabled = true
                compositeEnabled = true
            }
        }
    }
}
```
Rebuild the project, and Kobby will generate classes `CinemaSimpleKtorAdapter` and `CinemaCompositeKtorAdapter`
in subpackage `adapter.ktor`. The prefix `<Cinema>` by default is schema name. 
You can change default prefix by means of `prefix` variable in `ktor` section of `kobby` extension.


**Implicit way to configure adapter generation** is to just add `ktor-client-cio` dependency to your `build.gradle.kts`:
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

The `CinemaSimpleKtorAdapter` is simple to configure, but it does not support 
GraphQL subscriptions - so we can use only queries and mutations:
```kotlin
        val client = HttpClient {
            Auth {
                basic {
                    username = "admin"
                    password = "admin"
                }
            }
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
                    registerModule(JavaTimeModule())
                    // Force Jackson to serialize dates as String
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
        }

        val context = cinemaContextOf(
            CinemaSimpleKtorAdapter(client, "http://localhost:8080/graphql")
        )
```

The `CinemaCompositeKtorAdapter` adapter is more difficult to configure, but it supports all types 
of GraphQL operations, including subscriptions:
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
                },
                mapOf("Authorization" to "Basic YWRtaW46YWRtaW4=")
            )
        )
```
Full example of adapters configuration you can see 
[here](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-kotlin-client/src/main/kotlin/io/github/ermadmi78/kobby/cinema/kotlin/client/application.kt).


Note that you are not required to use the default adapters. You can write your own adapter implementation. For example,
see [here](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-server/src/test/kotlin/io/github/ermadmi78/kobby/cinema/server/CinemaTestAdapter.kt)
adapter implementation for Spring Boot integration tests.

## Ok, we are ready to execute GraphQL queries by means of generated Kotlin DSL

You can see more details about the generated GraphQL DSL
[here](https://github.com/ermadmi78/kobby/wiki/Overview-of-generated-GraphQL-DSL)

### Simple query

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)

### Simple mutation

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/mutation.png)

### Simple subscription

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/subscription.png)

### We can download a complex graph of objects by means of our Kotlin DSL

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/complex_query.png)

### GraphQL's unions and interfaces are supported too

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/union_query.png)


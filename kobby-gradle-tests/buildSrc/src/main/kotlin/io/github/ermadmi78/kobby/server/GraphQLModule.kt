package io.github.ermadmi78.kobby.server


import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.expediagroup.graphql.server.ktor.*
import graphql.Scalars
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLType
import io.github.ermadmi78.kobby.server.controller.ActorQueryService
import io.github.ermadmi78.kobby.server.controller.CountryQueryService
import io.github.ermadmi78.kobby.server.controller.FilmQueryService
import io.github.ermadmi78.kobby.server.controller.MutationsService
import io.github.ermadmi78.kobby.server.controller.SubscriptionService
import io.github.ermadmi78.kobby.server.controller.TaggableQueryService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun Application.graphQLModule() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(1)
        contentConverter = JacksonWebsocketContentConverter()
    }
    install(StatusPages) {
        defaultGraphQLStatusPages()
    }
    install(CORS) {
        anyHost()
    }
    install(GraphQL) {
        schema {
            packages = listOf("io.github.ermadmi78.kobby.server")
            queries = listOf(
                FilmQueryService(), ActorQueryService(), CountryQueryService(), TaggableQueryService()
            )
            mutations = listOf(MutationsService())
            subscriptions = listOf(SubscriptionService())
            hooks = object : FlowSubscriptionSchemaGeneratorHooks() {
                override fun willGenerateGraphQLType(type: KType): GraphQLType? =
                    when (type.classifier as? KClass<*>) {
                        Long::class -> Scalars.GraphQLID
                        Map::class -> ExtendedScalars.Json
                        LocalDate::class -> ExtendedScalars.Date
                        else -> null
                    }
            }
        }
        engine {
            dataLoaderRegistryFactory = KotlinDataLoaderRegistryFactory()
        }
        server {
            contextFactory = DefaultKtorGraphQLContextFactory()
        }
    }
    routing {
        graphQLGetRoute()
        graphQLPostRoute()
        graphQLSubscriptionsRoute("graphql-ws")
        graphiQLRoute()
        graphQLSDLRoute()
    }
}
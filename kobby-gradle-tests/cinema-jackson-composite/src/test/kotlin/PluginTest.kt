import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import kobby.kotlin.CinemaContext
import kobby.kotlin.CinemaMapper
import kobby.kotlin.adapter.ktor.CinemaCompositeKtorAdapter
import kobby.kotlin.cinemaContextOf
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

class PluginTest : AnnotationSpec() {

    private fun schema(): CinemaContext {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }

        val mapper = object : CinemaMapper {
            private val objectMapper = jacksonObjectMapper()
            override fun serialize(value: Any): String = objectMapper.writeValueAsString(value)

            override fun <T : Any> deserialize(
                content: String,
                contentType: KClass<T>
            ): T = objectMapper.readValue(content, contentType.java)
        }

        val targetHost = System.getProperty("kobby-gradle-tests.server-url")
        checkNotNull(targetHost) {
            "webserver doesn't provide server url: check your gradle configuration"
        }

        return cinemaContextOf(
            CinemaCompositeKtorAdapter(
                client,
                "http://$targetHost/graphql",
                "ws://$targetHost/graphql-ws",
                mapper
            )
        )
    }

    @Test
    suspend fun `query request`() {
        val result = schema().query {
            films {
                limit = 5
                title
            }
        }

        result.films shouldHaveSize 5
    }
}
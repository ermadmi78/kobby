import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import kobby.kotlin.CinemaContext
import kobby.kotlin.adapter.ktor.CinemaSimpleKtorAdapter
import kobby.kotlin.cinemaContextOf
import kotlinx.coroutines.runBlocking

class PluginTest : AnnotationSpec() {

    private fun schema(): CinemaContext {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }

        val targetHost = System.getProperty("kobby-gradle-tests.server-url")
        checkNotNull(targetHost) {
            "webserver doesn't provide server url: check your gradle configuration"
        }

        return cinemaContextOf(
            CinemaSimpleKtorAdapter(
                client,
                "http://$targetHost/graphql"
            )
        )
    }

    @Test
    fun `query request`() = runBlocking {
        val result = schema().query {
            films {
                limit = 5
                title
            }
        }

        result.films shouldHaveSize 5
    }
}
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kobby.kotlin.CinemaContext
import kobby.kotlin.adapter.ktor.CinemaCompositeKtorAdapter
import kobby.kotlin.cinemaContextOf
import kobby.kotlin.cinemaJson

object ContextHolder {

    val context: CinemaContext by lazy { schema() }

    private fun schema(): CinemaContext {
        val client = HttpClient(CIO) {
            expectSuccess = true
            install(WebSockets)
            install(DefaultRequest) {
                header("Sec-WebSocket-Protocol", "graphql-transport-ws")
            }
            install(ContentNegotiation) {
                json(cinemaJson)
            }
        }

        val targetHost = System.getProperty("kobby-gradle-tests.server-url") ?: "localhost:18080"
        checkNotNull(targetHost) {
            "webserver doesn't provide server url: check your gradle configuration"
        }

        return cinemaContextOf(
            CinemaCompositeKtorAdapter(
                client,
                "http://$targetHost/graphql",
                "ws://$targetHost/graphql-ws"
            )
        )
    }
}

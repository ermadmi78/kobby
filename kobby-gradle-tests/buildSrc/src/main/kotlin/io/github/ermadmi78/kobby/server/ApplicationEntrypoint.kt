package io.github.ermadmi78.kobby.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.net.ServerSocket

class ApplicationEntrypoint {
    fun createContext(port: Int? = null, wait: Boolean = false): NettyApplicationEngine {
        // disable development mode
        System.setProperty("io.ktor.development", "false")
        val localPort = port ?: ServerSocket(0).use { it.localPort }

        println("connecting on port $localPort")
        return embeddedServer(Netty, localPort) {
            graphQLModule()
        }.start(wait)
    }
}

fun main(args: Array<String>) {
    ApplicationEntrypoint().createContext(18080, true)
}

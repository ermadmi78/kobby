package io.github.ermadmi78.kobby.server

import io.ktor.server.netty.NettyApplicationEngine
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class WebServer : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private lateinit var context: NettyApplicationEngine

    fun getServerUrl(): String {
        if (!this::context.isInitialized) {
            this.context = ApplicationEntrypoint().createContext()
        }

        val port = context.environment.connectors.map { it.port }.first()
        return "localhost:$port"
    }

    override fun close() {
        if (this::context.isInitialized) {
            this.context.stop()
        }
    }
}
package no.nav.aap.komponenter.httpklient.httpclient

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.runBlocking

fun createFakeServer(block: Application.() -> Unit) = embeddedServer(Netty, port = 0) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    this.let(block)

}.apply { start() }

fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}
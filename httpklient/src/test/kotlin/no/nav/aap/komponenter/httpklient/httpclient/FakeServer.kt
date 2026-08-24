package no.nav.aap.komponenter.httpklient.httpclient

import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.runBlocking

fun createFakeServer(block: Application.() -> Unit) = embeddedServer(Netty, port = 0) {
    install(ContentNegotiation) {
        // java.time (de)serialization support is built into jackson-databind in Jackson 3
        jackson()
    }

    this.let(block)

}.apply { start() }

fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}
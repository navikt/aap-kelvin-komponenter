package no.nav.aap.komponenter.httpklient.httpclient

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class RestClientTest {

    @Test
    fun `tester at put requester funker`() {
        val server = embeddedServer(Netty, port = 0) {
            routing {
                put("/test") {
                    call.respondText("""{"status": "ok"}""")
                }
            }
        }.apply { start() }

        val tokenProvider = mockk<TokenProvider>(relaxed = true)
        val client = RestClient(ClientConfig(), tokenProvider, DefaultResponseHandler())
        val response: Map<String, String>? =
            client.put(URI("http://localhost:${server.port()}/test"), PutRequest("""{"This": "json"}"""))

        assertThat(response).isEqualTo(mapOf("status" to "ok"))
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

}
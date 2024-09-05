package no.nav.aap.komponenter.httpklient.httpclient

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.ContentType
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.Request
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders
import java.time.Duration

class RestClientTest {

    val mapper = {body: InputStream, _: HttpHeaders -> body.bufferedReader(Charsets.UTF_8).use { it.readText() } }
    val server = embeddedServer(Netty, port = 0) {
        routing {
            get("/test") {
                call.respondText("you got me")
            }
            post ("/test") {
                call.respondText(call.receiveText())
            }
            put ("/test") {
                call.respondText(call.receiveText())
            }
            patch ("/test") {
                call.respondText(call.receiveText())
            }
            delete ("/test") {
                call.respondText("y u delete me?")
            }
        }
    }.apply { start() }
    val tokenProvider = mockk<TokenProvider>(relaxed = true)
    val client = RestClient(ClientConfig(), tokenProvider, DefaultResponseHandler())
    val url = "http://localhost:${server.port()}/test"

    @Test
    fun `tester at get requester funker`() {
        val response: String? =
            client.get(URI(url), GetRequest(), mapper)
        assertThat(response).isEqualTo("you got me")
    }

    @Test
    fun `tester at post requester funker`() {
        val response: String? =
            client.post(URI(url), PostRequest("post me", ContentType.TEXT_PLAIN), mapper)
        assertThat(response).isEqualTo("post me")
    }

    @Test
    fun `tester at patch requester funker`() {
        val response: String? =
            client.patch(URI(url), PatchRequest("patch me", ContentType.TEXT_PLAIN), mapper)
        assertThat(response).isEqualTo("patch me")
    }

    @Test
    fun `tester at put requester funker`() {
        val response: String? =
            client.put(URI(url), PutRequest("put me", ContentType.TEXT_PLAIN), mapper)
        assertThat(response).isEqualTo("put me")
    }

    @Test
    fun `tester at delete requester funker`() {
        val response: String? =
            client.request(HttpMethod.Delete, URI(url), object :Request {
                override fun additionalHeaders() = emptyList<Header>()
                override fun timeout() = Duration.ofSeconds(1)
                override fun currentToken() = null
            }, mapper)
        assertThat(response).isEqualTo("y u delete me?")
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

}
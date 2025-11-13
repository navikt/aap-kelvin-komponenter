package no.nav.aap.komponenter.httpklient.httpclient

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.request.ContentType
import no.nav.aap.komponenter.httpklient.httpclient.request.DeleteRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.InputStream
import java.net.URI
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpHeaders

class RestClientTest {

    data class MyCustomRequest(val id: String)

    private val mapper =
        { body: InputStream, _: HttpHeaders -> body.bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private var failGetCounter = 0
    private var failPostCounter = 0
    private val MAX_FAIL_COUNT = 3
    private val server = createFakeServer {
        routing {
            get("/test") {
                call.respondText("you got me")
            }
            get("/returnerer-null") {
                call.respond(HttpStatusCode.NoContent)
            }
            get("/test-connection-exception") {
                failGetCounter++
                if (failGetCounter > MAX_FAIL_COUNT) {
                    call.respondText("Ok")
                }
                throw HttpConnectTimeoutException("Test-feil")
            }
            post("/test") {
                call.respondText(call.receiveText())
            }
            post("/test2") {
                val req = call.receive<MyCustomRequest>()

                call.respondText(req.id)
            }

            post("/test-connection-exception") {
                failPostCounter++
                if (failPostCounter > MAX_FAIL_COUNT) {
                    call.respondText("Ok")
                }
                throw HttpConnectTimeoutException("Test-feil")
            }
            put("/test") {
                call.respondText(call.receiveText())
            }
            patch("/test") {
                call.respondText(call.receiveText())
            }
            delete("/test") {
                call.respondText("y u delete me?")
            }
        }
    }

    private val client = RestClient(ClientConfig(), NoTokenTokenProvider(), DefaultResponseHandler())
    private val url = "http://localhost:${server.port()}/test"

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
    fun `å få custom datatype bør funke`() {
        val resp = client.post(
            URI("http://localhost:${server.port()}/test2"),
            PostRequest(MyCustomRequest("post me"), ContentType.APPLICATION_JSON),
            mapper
        )

        assertThat(resp).isEqualTo("post me")
    }

    @Test
    fun `skal returnere null ved no-content`() {
        val resp = client.get<Any?>(URI("http://localhost:${server.port()}/returnerer-null"), GetRequest())
        assertThat(resp).isNull()
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
            client.delete(URI(url), DeleteRequest(), mapper)
        assertThat(response).isEqualTo("y u delete me?")
    }

    @Test
    fun `404 returnerer IkkeFunnetException`() {
        assertThrows<IkkeFunnetException> {
            client.get(
                URI("http://localhost:${server.port()}/dxxcc"),
                GetRequest(),
                mapper
            )
        }
    }

    @Test
    fun `Skal retrye med ok resultat når man får http-connection-exception og nok retry-forsøk`() {
        failGetCounter = 0
        client.retryableGet(
            URI("http://localhost:${server.port()}/test-connection-exception"),
            GetRequest(),
            mapper,
            5
        )
        assertThat(failGetCounter).isGreaterThan(1)
    }

    @Test
    fun `Skal feile med retry når man får http-connection-exception med for få retries`() {
        failGetCounter = 0
        assertThrows<InternalServerErrorHttpResponsException> {
            client.retryableGet(
                URI("http://localhost:${server.port()}/test-connection-exception"),
                GetRequest(),
                mapper,
                2
            )
        }
        assertThat(failGetCounter).isGreaterThan(1)
    }

    @Test
    fun `Skal retrye med ok resultat når man får http-connection-exception ved post og nok retry-forsøk`() {
        failPostCounter = 0
        client.retryablePost(
            URI("http://localhost:${server.port()}/test-connection-exception"),
            PostRequest(MyCustomRequest("post me"), ContentType.APPLICATION_JSON),
            mapper,
            5
        )
        assertThat(failPostCounter).isGreaterThan(1)
    }

    @Test
    fun `Skal feile med retries når man får http-connection-exception ved post med for få retries`() {
        failPostCounter = 0
        assertThrows<InternalServerErrorHttpResponsException> {
            client.retryablePost(
                URI("http://localhost:${server.port()}/test-connection-exception"),
                PostRequest(MyCustomRequest("post me"), ContentType.APPLICATION_JSON),
                mapper,
                2
            )
        }
        assertThat(failPostCounter).isGreaterThan(1)
    }


}
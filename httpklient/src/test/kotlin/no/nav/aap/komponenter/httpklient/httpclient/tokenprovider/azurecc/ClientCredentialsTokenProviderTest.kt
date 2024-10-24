package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.httpklient.httpclient.createFakeServer
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.port
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import kotlin.reflect.KClass


class ClientCredentialsTokenProviderTest {

    @Test
    fun `skal generer tidspunkt frem i tid på utløpstidspunkt`() {

        val expiresTime = calculateExpiresTime(3600)
        val inTheFuture = LocalDateTime.now().plusSeconds(3550)
        val evenMoreinTheFuture = LocalDateTime.now().plusSeconds(3600)

        assertThat(inTheFuture).isBefore(expiresTime)
        assertThat(evenMoreinTheFuture).isAfter(expiresTime)
    }

}

class `håndtering av responsebody ved exception` {
    companion object {
        @JvmStatic
        fun testArgumenter() = listOf(
            Arguments.of(HttpStatusCode.BadRequest, BadRequestHttpResponsException::class),
            Arguments.of(HttpStatusCode.InternalServerError, InternalServerErrorHttpResponsException::class),
            Arguments.of(HttpStatusCode.Forbidden, ManglerTilgangException::class),
            Arguments.of(HttpStatusCode.NotFound, IkkeFunnetException::class),
        )
    }

    val fakeServer: EmbeddedServer<*, *>

    init {
        fakeServer = createFakeServer {
            routing {
                post("/") {
                    val response = ResponseProvider.message
                    if (response != null)
                        call.respond(ResponseProvider.response!!, response)
                    else call.respond(ResponseProvider.response!!)
                }
            }
        }

        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${fakeServer.port()}")
        System.setProperty("azure.app.client.id", "43")
        System.setProperty("azure.app.client.secret", "99")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${fakeServer.port()}")
        System.setProperty("azure.openid.config.issuer", "noe")
    }

    @ParameterizedTest(name = "response body blir lagt på exception ved {0}")
    @MethodSource("testArgumenter")
    fun `response body blir lagt på exception ved exception`(
        statusCode: HttpStatusCode, expectedException: KClass<Any>
    ) {
        ResponseProvider.response = statusCode
        ResponseProvider.message = """{"message": "error"}"""

        val actual = assertThrows<RuntimeException> {
            ClientCredentialsTokenProvider.getToken("scope", null)
        }

        assertThat(actual).isInstanceOf(expectedException.java)

        assertThat(actual.message).contains("error")
    }

    @Test
    fun `handle http exception response of type text`() {
        ResponseProvider.response = HttpStatusCode.BadRequest
        ResponseProvider.message = "HORROR"

        val actual = assertThrows<RuntimeException> {
            ClientCredentialsTokenProvider.getToken("scope", null)
        }

        assertThat(actual.message).contains("HORROR")
    }

    @Test
    fun `handle http exception response with empty body`() {
        ResponseProvider.response = HttpStatusCode.BadRequest
        ResponseProvider.message = null

        assertThrows<BadRequestHttpResponsException> {
            ClientCredentialsTokenProvider.getToken("scope", null)
        }
    }

}

object ResponseProvider {
    var response: HttpStatusCode? = null
    var message: String? = null
}

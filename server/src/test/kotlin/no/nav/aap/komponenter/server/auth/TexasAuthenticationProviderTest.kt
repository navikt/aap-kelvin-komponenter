package no.nav.aap.komponenter.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TexasAuthenticationProviderTest {

    @BeforeEach
    fun setSystemProperties() {
        System.setProperty("nais.token.introspection.endpoint", "http://texas/introspect")
    }

    @Test
    fun `same token is only introspected once`() {
        val callCount = AtomicInteger(0)
        val token = buildJwt(expiresInSeconds = 3600)

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = true)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val client = createClient {}
            repeat(3) {
                assertThat(client.get("/protected") { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.OK)
            }

            assertThat(callCount.get()).isEqualTo(1)
        }
    }

    @Test
    fun `different tokens each trigger their own introspect call`() {
        val callCount = AtomicInteger(0)

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = true)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val client = createClient {}
            client.get("/protected") { bearerAuth(buildJwt(expiresInSeconds = 3600)) }
            client.get("/protected") { bearerAuth(buildJwt(expiresInSeconds = 3600)) }

            assertThat(callCount.get()).isEqualTo(2)
        }
    }

    @Test
    fun `inactive token is not cached and Texas is called on every request`() {
        val callCount = AtomicInteger(0)
        val token = buildJwt(expiresInSeconds = 3600)

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = false)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val client = createClient {}
            repeat(2) {
                assertThat(client.get("/protected") { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.Unauthorized)
            }

            assertThat(callCount.get()).isEqualTo(2)
        }
    }

    @Test
    fun `non-JWT bearer token is rejected without calling Texas`() {
        val callCount = AtomicInteger(0)

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = true)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val resp = createClient {}.get("/protected") { bearerAuth("not-a-jwt-token") }

            assertThat(resp.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(callCount.get()).isEqualTo(0)
        }
    }

    @Test
    fun `request without Authorization header is rejected without calling Texas`() {
        val callCount = AtomicInteger(0)

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = true)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val resp = createClient {}.get("/protected")

            assertThat(resp.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(callCount.get()).isEqualTo(0)
        }
    }

    @Test
    fun `token without exp claim is not cached`() {
        val callCount = AtomicInteger(0)
        val token = JWT.create()
            .withSubject(UUID.randomUUID().toString())
            .sign(Algorithm.HMAC256("test-secret"))

        testApplication {
            install(Authentication) {
                register(providerWith(mockTexasClient(callCount, active = true)))
            }
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    get("/protected") { call.respond("OK") }
                }
            }

            val client = createClient {}
            repeat(2) { client.get("/protected") { bearerAuth(token) } }

            assertThat(callCount.get()).isEqualTo(2)
        }
    }

    // --- helpers ---

    private fun providerWith(client: HttpClient) =
        TexasAuthenticationProvider.Config(IdentityProvider.ENTRA_ID, client).build()

    private fun mockTexasClient(callCount: AtomicInteger, active: Boolean): HttpClient {
        val engine = MockEngine {
            callCount.incrementAndGet()
            respond(
                content = """{"active": $active}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }
    }
}

private fun buildJwt(expiresInSeconds: Long): String {
    val subject = UUID.randomUUID().toString()
    return JWT.create()
        .withSubject(subject)
        .withClaim("oid", subject)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .sign(Algorithm.HMAC256("test-secret"))
}

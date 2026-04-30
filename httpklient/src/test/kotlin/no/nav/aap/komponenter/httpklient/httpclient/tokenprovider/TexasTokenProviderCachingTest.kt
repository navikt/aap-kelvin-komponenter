package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.createFakeServer
import no.nav.aap.komponenter.httpklient.httpclient.port
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TexasTokenProviderCachingTest {

    private val texasCallCount = AtomicInteger(0)

    private val fakeTexas = createFakeServer {
        routing {
            post("/token") {
                texasCallCount.incrementAndGet()
                call.respond(
                    mapOf(
                        "access_token" to buildJwt(expiresInSeconds = 3600),
                        "token_type" to "Bearer",
                        "expires_in" to 3600,
                    )
                )
            }
            post("/token/exchange") {
                texasCallCount.incrementAndGet()
                call.respond(
                    mapOf(
                        "access_token" to buildJwt(expiresInSeconds = 3600),
                        "token_type" to "Bearer",
                        "expires_in" to 3600,
                    )
                )
            }
        }
    }

    @BeforeEach
    fun resetCounter() {
        texasCallCount.set(0)
    }

    // --- M2M ---

    @Test
    fun `M2M - fetches token from Texas on first call`() {
        val provider = m2mProvider()
        provider.getToken("api://scope-a/.default", null)
        assertThat(texasCallCount.get()).isEqualTo(1)
    }

    @Test
    fun `M2M - reuses cached token on subsequent calls for same scope`() {
        val provider = m2mProvider()
        provider.getToken("api://scope-a/.default", null)
        provider.getToken("api://scope-a/.default", null)
        provider.getToken("api://scope-a/.default", null)
        assertThat(texasCallCount.get()).isEqualTo(1)
    }

    @Test
    fun `M2M - fetches separate tokens for different scopes`() {
        val provider = m2mProvider()
        provider.getToken("api://scope-a/.default", null)
        provider.getToken("api://scope-b/.default", null)
        assertThat(texasCallCount.get()).isEqualTo(2)
    }

    @Test
    fun `M2M - refetches token after expiry`() {
        val provider = m2mProviderWithExpiredToken()
        provider.getToken("api://scope-a/.default", null)
        provider.getToken("api://scope-a/.default", null)
        // Both calls should hit Texas since the token is expired immediately
        assertThat(texasCallCount.get()).isEqualTo(2)
    }

    // --- OBO ---

    @Test
    fun `OBO - fetches token from Texas on first call`() {
        val provider = oboProvider()
        val userToken = OidcToken(buildUserJwt("Z123456"))
        provider.getToken("api://scope-a/.default", userToken)
        assertThat(texasCallCount.get()).isEqualTo(1)
    }

    @Test
    fun `OBO - reuses cached token on subsequent calls for same scope and user`() {
        val provider = oboProvider()
        val userToken = OidcToken(buildUserJwt("Z123456"))
        provider.getToken("api://scope-a/.default", userToken)
        provider.getToken("api://scope-a/.default", userToken)
        provider.getToken("api://scope-a/.default", userToken)
        assertThat(texasCallCount.get()).isEqualTo(1)
    }

    @Test
    fun `OBO - fetches new token for different users`() {
        val provider = oboProvider()
        val userToken1 = OidcToken(buildUserJwt("Z111111"))
        val userToken2 = OidcToken(buildUserJwt("Z222222"))
        provider.getToken("api://scope-a/.default", userToken1)
        provider.getToken("api://scope-a/.default", userToken2)
        assertThat(texasCallCount.get()).isEqualTo(2)
    }

    @Test
    fun `OBO - fetches separate tokens for different scopes with same user`() {
        val provider = oboProvider()
        val userToken = OidcToken(buildUserJwt("Z123456"))
        provider.getToken("api://scope-a/.default", userToken)
        provider.getToken("api://scope-b/.default", userToken)
        assertThat(texasCallCount.get()).isEqualTo(2)
    }

    // --- helpers ---

    private fun m2mProvider() = TexasM2MTokenProvider(
        identityProvider = "entra_id",
        texasUri = URI("http://localhost:${fakeTexas.port()}/token"),
        prometheus = SimpleMeterRegistry(),
    )

    /** Provider backed by a fake Texas that returns already-expired tokens */
    private fun m2mProviderWithExpiredToken(): TexasM2MTokenProvider {
        val fakeWithExpired = createFakeServer {
            routing {
                post("/token/expired") {
                    texasCallCount.incrementAndGet()
                    call.respond(
                        mapOf(
                            "access_token" to buildJwt(expiresInSeconds = -1),
                            "token_type" to "Bearer",
                            "expires_in" to 0,
                        )
                    )
                }
            }
        }
        return TexasM2MTokenProvider(
            identityProvider = "entra_id",
            texasUri = URI("http://localhost:${fakeWithExpired.port()}/token/expired"),
            prometheus = SimpleMeterRegistry(),
        )
    }

    private fun oboProvider() = TexasOBOTokenProvider(
        identityProvider = "entra_id",
        texasUri = URI("http://localhost:${fakeTexas.port()}/token/exchange"),
        prometheus = SimpleMeterRegistry(),
    )
}

private fun buildJwt(expiresInSeconds: Long): String {
    val expiry = if (expiresInSeconds > 0) {
        Date(System.currentTimeMillis() + expiresInSeconds * 1000)
    } else {
        Date(System.currentTimeMillis() - 1000) // already expired
    }
    val subject = UUID.randomUUID().toString()
    return JWT.create()
        .withSubject(subject)
        .withClaim("oid", subject) // oid == sub → client credentials token
        .withExpiresAt(expiry)
        .sign(Algorithm.HMAC256("test-secret"))
}

/** Builds a person (OBO) token with a NAVident claim. oid != sub so isClientCredentials() returns false. */
private fun buildUserJwt(navIdent: String, expiresInSeconds: Long = 3600): String {
    return JWT.create()
        .withSubject(UUID.randomUUID().toString())
        .withClaim("NAVident", navIdent)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .sign(Algorithm.HMAC256("test-secret"))
}

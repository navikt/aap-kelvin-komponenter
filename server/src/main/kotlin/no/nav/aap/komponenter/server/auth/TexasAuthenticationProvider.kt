package no.nav.aap.komponenter.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.DeserializationFeature
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/**
 * Ktor [AuthenticationProvider] that validates Bearer tokens using
 * [Texas (Token Exchange as a Service)](https://docs.nais.io/auth/explanations/#texas) -
 * a sidecar provided by the NAIS platform.
 *
 * Instead of performing JWT validation locally, this provider delegates token introspection to
 * the Texas sidecar via its introspection endpoint (`nais.token.introspection.endpoint`).
 * Texas communicates with the configured identity provider (e.g. Entra ID) and returns whether
 * the token is active and carries valid claims.
 *
 * Introspection results for active tokens are cached locally for up to [MAX_CACHE_TTL] (or until
 * token expiry, whichever comes first) to avoid hitting Texas on every inbound request.
 * Note: revocation is not reflected until the cached entry expires.
 *
 * On a successful introspection the call is authenticated and a [JWTPrincipal] is attached.
 * If the token is missing, inactive, or the introspection call fails, the request is rejected
 * with HTTP 401 Unauthorized.
 *
 * @see IdentityProvider
 **/
internal class TexasAuthenticationProvider(
    config: Config
) : AuthenticationProvider(config) {
    private val logger = LoggerFactory.getLogger(TexasAuthenticationProvider::class.java)

    private val introspectEndpoint = requiredConfigForKey("nais.token.introspection.endpoint")
    private val client = config.client
    private val identityProvider = config.identityProvider

    private data class CachedIntrospectResult(
        val response: IntrospectResponse,
        val expiresAt: Instant,
    )

    private val cache: Cache<String, CachedIntrospectResult> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfter(tokenExpiry())
        .build()

    internal class Config(
        val identityProvider: IdentityProvider,
        httpClient: HttpClient? = null,
    ) : AuthenticationProvider.Config(identityProvider.value) {
        val client = httpClient ?: HttpClient(CIO) {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

        internal fun build() = TexasAuthenticationProvider(this)
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val bearerToken = (context.call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)
            ?.takeIf { header -> header.authScheme.equals(AuthScheme.Bearer, ignoreCase = true) }
            ?.blob

        if (bearerToken == null) {
            logger.warn("unauthenticated: no Bearer token found in Authorization header")
            context.loginChallenge(AuthenticationFailedCause.NoCredentials)
            return
        }

        val decoded: DecodedJWT = try {
            JWT.decode(bearerToken)
        } catch (e: Exception) {
            logger.warn("unauthenticated: Bearer token is not a valid JWT", e)
            context.loginChallenge(AuthenticationFailedCause.InvalidCredentials)
            return
        }

        val cacheKey = sha256(bearerToken)
        val cached = cache.getIfPresent(cacheKey)

        val introspectResponse = if (cached != null) {
            cached.response
        } else {
            val newToken = try {
                client.submitForm(
                    introspectEndpoint,
                    parameters {
                        set("token", bearerToken)
                        set("identity_provider", identityProvider.value)
                    },
                ).body<IntrospectResponse>()
            } catch (e: Exception) {
                logger.error("unauthenticated: introspect request failed", e)
                context.loginChallenge(AuthenticationFailedCause.Error(e.message ?: "introspect request failed"))
                return
            }

            if (newToken.active) {
                // Only cache when we can determine a safe expiry from the token itself.
                // Cap at MAX_CACHE_TTL to preserve a reasonable revocation window.
                val tokenExp = decoded.expiresAt?.toInstant()
                if (tokenExp != null) {
                    val expiresAt = minOf(
                        tokenExp.minusSeconds(30),
                        Instant.now().plus(MAX_CACHE_TTL),
                    )
                    if (expiresAt.isAfter(Instant.now())) {
                        cache.put(cacheKey, CachedIntrospectResult(newToken, expiresAt))
                    }
                }
            }

            newToken
        }

        if (introspectResponse.active) {
            context.principal(JWTPrincipal(decoded))
        } else {
            logger.warn("unauthenticated: ${introspectResponse.error}")
            context.loginChallenge(AuthenticationFailedCause.InvalidCredentials)
        }
    }

    private data class IntrospectResponse(
        val active: Boolean,
        val error: String? = null,
    )

    private fun AuthenticationContext.loginChallenge(cause: AuthenticationFailedCause) {
        challenge("Texas", cause) { authenticationProcedureChallenge, call ->
            call.respond(HttpStatusCode.Unauthorized, "")
            authenticationProcedureChallenge.complete()
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun tokenExpiry() = object : Expiry<String, CachedIntrospectResult> {
        override fun expireAfterCreate(key: String, value: CachedIntrospectResult, currentTime: Long): Long =
            Duration.between(Instant.now(), value.expiresAt).toNanos().coerceAtLeast(0)

        override fun expireAfterUpdate(key: String, value: CachedIntrospectResult, currentTime: Long, currentDuration: Long) =
            expireAfterCreate(key, value, currentTime)

        override fun expireAfterRead(key: String, value: CachedIntrospectResult, currentTime: Long, currentDuration: Long) =
            currentDuration
    }

    private companion object {
        /** Maximum time an active introspection result is cached, regardless of token TTL. */
        val MAX_CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}

public enum class IdentityProvider(public val value: String) {
    ENTRA_ID("entra_id"), // Tidligere AzureAD
    TOKENX("tokenx"),
}

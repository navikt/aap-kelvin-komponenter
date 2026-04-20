package no.nav.aap.komponenter.server.auth

import com.auth0.jwt.JWT
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Ktor [AuthenticationProvider] that validates Bearer tokens using
 * [Texas (Token Exchange as a Service)](https://docs.nais.io/auth/explanations/#texas) –
 * a sidecar provided by the NAIS platform.
 *
 * Instead of performing JWT validation locally, this provider delegates token introspection to
 * the Texas sidecar via its introspection endpoint (`nais.token.introspection.endpoint`).
 * Texas communicates with the configured identity provider (e.g. Entra ID) and returns whether
 * the token is active and carries valid claims.
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

    private val introspectUrl = requireNotNull(System.getProperty("nais.token.introspection.endpoint"))
    private val client = config.client
    private val identityProvider = config.identityProvider

    internal class Config(
        val identityProvider: IdentityProvider
    ) : AuthenticationProvider.Config(identityProvider.value) {
        val client = RestClient.withDefaultResponseHandler(
            config = ClientConfig(),
            tokenProvider = NoTokenTokenProvider(),
        )

        internal fun build() = TexasAuthenticationProvider(this)
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val token = (context.call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)
            ?.takeIf { header -> header.authScheme.equals(AuthScheme.Bearer, ignoreCase = true) }
            ?.blob

        if (token == null) {
            logger.warn("unauthenticated: no Bearer token found in Authorization header")
            context.loginChallenge(AuthenticationFailedCause.NoCredentials)
            return
        }

        val introspectResponse =
            try {
                val postRequest = PostRequest(
                    body = IntrospectRequest(
                        token = token,
                        identityProvider = identityProvider.value
                    ),
                    additionalHeaders = listOf(
                        Header("Content-Type", "application/json")
                    )
                )

                val response = client.post<IntrospectRequest, IntrospectResponse>(
                    URI(introspectUrl),
                    postRequest
                )
                requireNotNull(response)
            } catch (e: Exception) {
                logger.error("unauthenticated: introspect request failed: ${e.message}")
                context.loginChallenge(AuthenticationFailedCause.Error(e.message ?: "introspect request failed"))
                return
            }

        if (!introspectResponse.active) {
            logger.warn("unauthenticated: ${introspectResponse.error}")
            context.loginChallenge(AuthenticationFailedCause.InvalidCredentials)
            return
        }

        // TODO: Erstatte med egendefinert Principal. Bruker JWT inntil alt er over på Texas.
        context.principal(JWTPrincipal(JWT.decode(token)))
    }

    private data class IntrospectRequest(
        val token: String,
        @param:JsonProperty("identity_provider")
        val identityProvider: String
    )

    private data class IntrospectResponse(
        val active: Boolean,
        val error: String? = null,
        val azp: String? = null,
        val idtyp: String? = null,
        val NAVident: String? = null
    )

    private fun AuthenticationContext.loginChallenge(cause: AuthenticationFailedCause) {
        challenge("Texas", cause) { authenticationProcedureChallenge, call ->
            call.respond(HttpStatusCode.Unauthorized, "")
            authenticationProcedureChallenge.complete()
        }
    }
}

public enum class IdentityProvider(public val value: String) {
    ENTRA_ID("entra_id"), // Tidligere AzureAD
    // TODO: Støtte TokenX i [NaisTokenAuthenticationProvider.kt]
}

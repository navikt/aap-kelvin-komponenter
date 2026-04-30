package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import java.net.URI

/**
 * Tokenprovider som benytter [Texas](https://doc.nais.io/auth/explanations/#texas).
 *
 * [How to consume OBO](https://docs.nais.io/auth/entra-id/how-to/consume-obo)
 **/
internal class TexasOBOTokenProvider(
    private val identityProvider: String,
    texasUri: URI? = null,
    private val prometheus: MeterRegistry,
) : TokenProvider {
    private val texasUri = texasUri ?: URI(requiredConfigForKey("nais.token.exchange.endpoint"))

    private val cache: Cache<String, OidcToken> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfter(tokenExpiry())
        .recordStats()
        .build()

    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus,
    )

    init {
        CaffeineCacheMetrics.monitor(prometheus, cache, "texas_obo_token")
    }

    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken {
        requireNotNull(scope) { "scope må være definert for token exchange med texas" }
        requireNotNull(currentToken) { "token må være tilstede for token exchange for texas" }

        val key = "$scope:${sha256(currentToken.token())}"
        return cache.get(key) { fetchToken(scope, currentToken) }
    }

    private fun fetchToken(scope: String, currentToken: OidcToken): OidcToken {
        val response: OidcTokenResponse = client.post(
            texasUri, PostRequest(
                body = mapOf(
                    "identity_provider" to identityProvider,
                    "target" to scope,
                    "user_token" to currentToken.token(),
                )
            )
        ) ?: error("oidc-token-response forventet fra texas")

        return OidcToken(response.access_token)
    }
}

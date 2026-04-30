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
 * [How to consume M2M](https://docs.nais.io/auth/entra-id/how-to/consume-m2m)
 **/
internal class TexasM2MTokenProvider(
    private val identityProvider: String,
    texasUri: URI? = null,
    private val prometheus: MeterRegistry,
) : TokenProvider {
    private val texasUri = texasUri ?: URI(requiredConfigForKey("nais.token.endpoint"))

    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
        prometheus = prometheus,
    )

    private val cache: Cache<String, OidcToken> = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfter(tokenExpiry())
        .recordStats()
        .build()

    init {
        CaffeineCacheMetrics.monitor(prometheus, cache, "texas_m2m_token")
    }

    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken {
        requireNotNull(scope) { "scope må være definert for token exchange med texas" }

        return cache.get(scope) { fetchToken(scope) }
    }

    private fun fetchToken(scope: String): OidcToken {
        val response: OidcTokenResponse = client.post(
            texasUri, PostRequest(
                body = mapOf(
                    "identity_provider" to identityProvider,
                    "target" to scope,
                )
            )
        ) ?: error("oidc-token-response forventet fra texas")

        return OidcToken(response.access_token)
    }
}

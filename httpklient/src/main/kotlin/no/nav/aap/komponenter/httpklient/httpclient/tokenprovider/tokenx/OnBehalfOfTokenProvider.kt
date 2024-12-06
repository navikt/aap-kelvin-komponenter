package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcTokenResponse
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI
import kotlin.IllegalArgumentException

public class OnBehalfOfTokenProvider(
    private val texasUri: URI = URI(requiredConfigForKey("nais.token.exchange.endpoint"))
): TokenProvider {
    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
    )

    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        if (scope == null) throw IllegalArgumentException("scope må være definert for tokenx")
        if (currentToken == null) throw IllegalArgumentException("token må være tilstede for tokenx")

        val response: OidcTokenResponse = client.post(texasUri, PostRequest(body = mapOf(
            "identity_provider" to "tokenx",
            "target" to scope,
            "user_token" to currentToken,
        ))) ?: error("oidc-token-response forventet fra texas")

        return OidcToken(response.access_token)
    }
}
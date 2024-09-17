package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.ContentType
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcTokenResponse
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URLEncoder
import java.time.Duration
import kotlin.text.Charsets.UTF_8

public class OnBehalfOfTokenProvider(private val config: AzureConfig = AzureConfig()) : TokenProvider {

    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
    )

    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        if (scope == null) {
            throw IllegalArgumentException("Kan ikke be om token uten å be om hvilket scope det skal gjelde for")
        }
        if (currentToken == null) {
            throw IllegalArgumentException("Kan ikke be om OBO-token uten å ha et token å be om det for")
        }

        val postRequest = PostRequest(
            body = formPost(scope, currentToken),
            contentType = ContentType.APPLICATION_FORM_URLENCODED,
            timeout = Duration.ofSeconds(10),
            additionalHeaders = listOf(Header("Cache-Control", "no-cache"))
        )

        val response: OidcTokenResponse? = client.post(uri = config.tokenEndpoint, request = postRequest)

        if (response == null) {
            return null
        }

        val oidcToken = OidcToken(response.access_token)

        return oidcToken
    }

    private fun formPost(scope: String, oidcToken: OidcToken): String {
        val encodedScope = URLEncoder.encode(scope, UTF_8)
        return "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
                "&client_id=" + config.clientId +
                "&client_secret=" + config.clientSecret +
                "&assertion=" + oidcToken.token() +
                "&scope=" + encodedScope +
                "&requested_token_use=on_behalf_of";
    }
}
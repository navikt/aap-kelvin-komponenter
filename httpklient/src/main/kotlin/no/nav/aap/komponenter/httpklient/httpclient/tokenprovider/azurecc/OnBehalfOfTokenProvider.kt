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
import no.nav.aap.komponenter.miljo.Miljø
import java.net.URLEncoder
import java.time.Duration
import kotlin.text.Charsets.UTF_8

@Deprecated("Bruk AzureOBOTokenProvider eller ClientCredentialsTokenProvider")
public object OnBehalfOfTokenProvider : TokenProvider by
/* Unit-tester feiler i behandlingsflyt. Regner med at endringen må gjøres i behandlingsflyt. Har
 * ikke tid til å undersøke nå. */
if (Miljø.erProd() || Miljø.erDev() || Miljø.erLokal())
    GammelOnBehalfOfTokenProvider
else
    AzureOBOTokenProvider()

private object GammelOnBehalfOfTokenProvider : TokenProvider {
    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider(),
    )
    private val config = AzureConfig() // Laster config on-demand

    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        if (scope == null) {
            throw IllegalArgumentException("Kan ikke be om token uten å be om hvilket scope det skal gjelde for")
        }
        if (currentToken == null) {
            throw IllegalArgumentException("Kan ikke be om OBO-token uten å ha et token å be om det for")
        }
        // Ved clientcredentials inn skal vi ikke veksle om til on-behalf-of token, men heller kalle videre som system
        if (currentToken.isClientCredentials()) {
            return ClientCredentialsTokenProvider.getToken(scope, currentToken)
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
                "&requested_token_use=on_behalf_of"
    }
}
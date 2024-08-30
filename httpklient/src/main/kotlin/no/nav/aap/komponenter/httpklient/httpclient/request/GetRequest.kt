package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.request.Request
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

class GetRequest(
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
) : Request {
    override fun additionalHeaders(): List<Header> {
        return additionalHeaders
    }

    override fun timeout(): Duration {
        return timeout
    }

    override fun currentToken(): OidcToken? {
        return currentToken
    }
}
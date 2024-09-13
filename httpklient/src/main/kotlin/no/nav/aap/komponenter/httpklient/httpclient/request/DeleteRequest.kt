package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

public class DeleteRequest(
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
) : Request {
    override fun body(): Any {
        throw UnsupportedOperationException("Get request body is not supported")
    }

    override fun contentType(): ContentType {
        throw UnsupportedOperationException("Get request contenttype is not supported")
    }

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
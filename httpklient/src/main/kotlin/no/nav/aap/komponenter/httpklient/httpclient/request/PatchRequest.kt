package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

public class PatchRequest<T : Any>(
    private val body: T,
    private val contentType: ContentType = ContentType.APPLICATION_JSON,
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
) : Request {
    override fun body(): Any {
        return body
    }

    override fun contentType(): ContentType {
        return contentType
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
package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.time.Duration

interface RequestWithBody<T: Any>: Request {
    fun body(): T
    fun contentType(): String
    fun convertBodyToString(): String
}

open class RequestWithBodyImpl<T : Any>(
    private val body: T,
    private val contentType: ContentType = ContentType.APPLICATION_JSON,
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
) : RequestWithBody<T> {
    override fun body() = body

    override fun contentType(): String {
        return contentType.toString()
    }

    override fun convertBodyToString(): String {
        return when(contentType) {
            ContentType.APPLICATION_JSON -> DefaultJsonMapper.toJson(body)
            ContentType.APPLICATION_FORM_URLENCODED -> {
                require(body is String)
                body}
            ContentType.TEXT_PLAIN -> body.toString()
        }
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
package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

sealed interface Request {

    fun body(): Any

    fun contentType(): ContentType

    fun additionalHeaders(): List<Header>

    fun timeout(): Duration

    fun currentToken(): OidcToken?
}
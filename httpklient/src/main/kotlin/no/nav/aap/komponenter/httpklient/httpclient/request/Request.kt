package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

public sealed interface Request {

    public fun body(): Any

    public fun contentType(): ContentType

    public fun additionalHeaders(): List<Header>

    public fun timeout(): Duration

    public fun currentToken(): OidcToken?
}
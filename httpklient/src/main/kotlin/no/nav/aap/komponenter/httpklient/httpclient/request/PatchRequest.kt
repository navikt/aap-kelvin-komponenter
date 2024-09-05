package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.Duration

class PatchRequest<T : Any>(
    body: T,
    contentType: ContentType = ContentType.APPLICATION_JSON,
    additionalHeaders: List<Header> = emptyList(),
    timeout: Duration = Duration.ofSeconds(60),
    currentToken: OidcToken? = null
) : RequestWithBodyImpl<T>(body, contentType, additionalHeaders, timeout, currentToken)
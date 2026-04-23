package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasOBOTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public object TokenxOBOTokenProvider : TokenProvider by TexasOBOTokenProvider(
    texasUri = URI(requiredConfigForKey("nais.token.exchange.endpoint")),
    identityProvider = "tokenx",
    prometheus = SimpleMeterRegistry(),
)

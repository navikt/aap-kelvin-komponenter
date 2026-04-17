package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasOBOTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public class TokenxOBOTokenProvider(
    texasUri: URI = URI(requiredConfigForKey("nais.token.exchange.endpoint")),
    prometheus: MeterRegistry = SimpleMeterRegistry(),
) : TokenProvider by TexasOBOTokenProvider(
    texasUri = texasUri,
    identityProvider = "tokenx",
    prometheus = prometheus,
)

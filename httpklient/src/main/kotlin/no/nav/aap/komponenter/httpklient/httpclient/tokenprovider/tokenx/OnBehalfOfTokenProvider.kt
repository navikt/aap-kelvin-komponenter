package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasOBOTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

@Deprecated("Brukt TokenxOBOTokenProvider")
public class OnBehalfOfTokenProvider(
    texasUri: URI = URI(requiredConfigForKey("nais.token.exchange.endpoint")),
    identityProvider: String = "tokenx",
) : TokenProvider by TexasOBOTokenProvider(
    texasUri = texasUri,
    identityProvider = identityProvider,
    prometheus = SimpleMeterRegistry(),
)
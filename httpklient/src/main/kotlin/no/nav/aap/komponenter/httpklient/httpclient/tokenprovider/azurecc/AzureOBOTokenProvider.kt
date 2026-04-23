package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasOBOTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public object AzureOBOTokenProvider : TokenProvider by TexasOBOTokenProvider(
    texasUri = URI(requiredConfigForKey("nais.token.exchange.endpoint")),
    identityProvider = "entra_id",
    prometheus = SimpleMeterRegistry(),
)

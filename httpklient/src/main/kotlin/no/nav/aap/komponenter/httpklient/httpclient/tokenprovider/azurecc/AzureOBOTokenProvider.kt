package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public class AzureOBOTokenProvider(
    texasUri: URI = URI(requiredConfigForKey("nais.token.exchange.endpoint")),
    prometheus: MeterRegistry = SimpleMeterRegistry(),
) : TokenProvider by TexasTokenProvider(
    texasUri = texasUri,
    identityProvider = "azuread",
    prometheus = prometheus,
)

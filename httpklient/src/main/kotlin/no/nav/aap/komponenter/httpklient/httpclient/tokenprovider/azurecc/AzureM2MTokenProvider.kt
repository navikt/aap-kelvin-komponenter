package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasM2MTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public class AzureM2MTokenProvider(
    texasUri: URI = URI(requiredConfigForKey("nais.token.endpoint")),
    prometheus: MeterRegistry = SimpleMeterRegistry(),
) : TokenProvider by TexasM2MTokenProvider(
    texasUri = texasUri,
    identityProvider = "entra_id",
    prometheus = prometheus,
)

package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TexasM2MTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

public object AzureM2MTokenProvider : TokenProvider by TexasM2MTokenProvider(
    texasUri = URI(requiredConfigForKey("nais.token.endpoint")),
    identityProvider = "entra_id",
    prometheus = SimpleMeterRegistry(),
)

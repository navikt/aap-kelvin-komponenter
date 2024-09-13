package no.nav.aap.komponenter.httpklient.httpclient

import java.time.Duration


public class ClientConfig(
    internal val scope: String? = null,
    internal val connectionTimeout: Duration = Duration.ofSeconds(15),
    internal val additionalHeaders: List<Header> = emptyList(),
    internal val additionalFunctionalHeaders: List<FunctionalHeader> = emptyList()
)
package no.nav.aap.komponenter.httpklient.httpclient

import no.nav.aap.komponenter.httpklient.httpclient.FunctionalHeader
import no.nav.aap.komponenter.httpklient.httpclient.Header
import java.time.Duration


class ClientConfig(
    val scope: String? = null,
    val connectionTimeout: Duration = Duration.ofSeconds(15),
    val additionalHeaders: List<Header> = emptyList(),
    val additionalFunctionalHeaders: List<FunctionalHeader> = emptyList()
)
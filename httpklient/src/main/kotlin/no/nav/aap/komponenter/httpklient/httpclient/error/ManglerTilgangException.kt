package no.nav.aap.komponenter.httpklient.httpclient.error

public class ManglerTilgangException(message: String, public val body: String?) : RuntimeException(message)
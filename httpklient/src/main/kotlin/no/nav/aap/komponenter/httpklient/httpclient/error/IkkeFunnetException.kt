package no.nav.aap.komponenter.httpklient.httpclient.error

public class IkkeFunnetException(message: String, public val body: String?) : RuntimeException(message)
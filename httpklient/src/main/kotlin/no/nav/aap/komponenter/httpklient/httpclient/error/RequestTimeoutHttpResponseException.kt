package no.nav.aap.komponenter.httpklient.httpclient.error

public class RequestTimeoutHttpResponseException(message: String, public val body: String?) : RuntimeException(message)

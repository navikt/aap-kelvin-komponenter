package no.nav.aap.komponenter.httpklient.httpclient.error

public class ConflictHttpResponseException(message: String, public val body: String?) : RuntimeException(message)
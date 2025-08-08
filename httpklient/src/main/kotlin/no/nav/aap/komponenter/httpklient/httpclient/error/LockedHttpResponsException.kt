package no.nav.aap.komponenter.httpklient.httpclient.error

public class LockedHttpResponsException(message: String, public val body: String?): RuntimeException(message)
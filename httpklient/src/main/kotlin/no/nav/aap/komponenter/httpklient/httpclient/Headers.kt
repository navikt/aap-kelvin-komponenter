package no.nav.aap.komponenter.httpklient.httpclient

public class Header(internal val key: String, internal val value: String)

public class FunctionalHeader(public val key: String, public val supplier: () -> String)

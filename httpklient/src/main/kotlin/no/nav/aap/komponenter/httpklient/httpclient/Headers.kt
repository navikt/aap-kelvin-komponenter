package no.nav.aap.komponenter.httpklient.httpclient

class Header(val key: String, val value: String)
class FunctionalHeader(val key: String, val supplier: () -> String)

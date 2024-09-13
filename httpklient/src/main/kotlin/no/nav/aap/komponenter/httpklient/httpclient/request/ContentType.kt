package no.nav.aap.komponenter.httpklient.httpclient.request

public enum class ContentType(private val string: String) {
    APPLICATION_JSON("application/json"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    TEXT_PLAIN("text/plain");

    override fun toString(): String {
        return string
    }
}
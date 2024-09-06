package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper

object BodyConverter {
    fun convert(body: Any, contentType: ContentType): String {
        return when (contentType) {
            ContentType.APPLICATION_JSON -> DefaultJsonMapper.toJson(body)
            ContentType.APPLICATION_FORM_URLENCODED -> requireString(body, contentType)
            ContentType.TEXT_PLAIN -> requireString(body, contentType)
        }
    }

    private fun requireString(body: Any, contentType: ContentType): String {
        require(body is String) {"Definert '${contentType}' men body er ikke av type String"}
        return body
    }
}
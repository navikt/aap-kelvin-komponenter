package no.nav.aap.komponenter.httpklient.httpclient.request

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper

object BodyConverter {
    fun convert(body: Any, contentType: ContentType): String {
        return when (contentType) {
            ContentType.APPLICATION_JSON -> DefaultJsonMapper.toJson(body)
            ContentType.APPLICATION_FORM_URLENCODED -> if (body is String) {
                body
            } else {
                throw IllegalArgumentException("Definert '${contentType}' men body er ikke av type String")
            }

            ContentType.TEXT_PLAIN -> if (body is String) {
                body
            } else {
                throw IllegalArgumentException("Definert '${contentType}' men body er ikke av type String")
            }
        }
    }
}
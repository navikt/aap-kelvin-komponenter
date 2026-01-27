package com.papsign.ktor.openapigen.content.type

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KType

interface ResponseSerializer: ContentTypeProvider {
    /**
     * used to determine which registered response serializer is used, based on the accept header
     */
    fun <T: Any> getSerializableContentTypes(type: KType): List<ContentType>
    suspend fun <T: Any> respond(response: T, request: RoutingContext, contentType: ContentType)
    suspend fun <T: Any> respond(statusCode: HttpStatusCode, response: T, request: RoutingContext, contentType: ContentType)
}

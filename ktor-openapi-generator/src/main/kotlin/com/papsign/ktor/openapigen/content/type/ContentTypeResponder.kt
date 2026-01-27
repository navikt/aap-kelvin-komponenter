package com.papsign.ktor.openapigen.content.type

import com.papsign.ktor.openapigen.route.response.Responder
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext

data class ContentTypeResponder(val responseSerializer: ResponseSerializer, val contentType: ContentType): Responder {
    override suspend fun <T : Any> respond(response: T, request: RoutingContext) {
        responseSerializer.respond(response, request, contentType)
    }

    override suspend fun <T : Any> respond(statusCode: HttpStatusCode, response: T, request: RoutingContext) {
        responseSerializer.respond(statusCode, response, request, contentType)
    }
}

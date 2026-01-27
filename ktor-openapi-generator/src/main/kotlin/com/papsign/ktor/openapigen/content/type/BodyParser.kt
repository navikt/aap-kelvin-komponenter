package com.papsign.ktor.openapigen.content.type

import io.ktor.http.ContentType
import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KType

interface BodyParser : ContentTypeProvider {
    fun <T : Any> getParseableContentTypes(type: KType): List<ContentType>
    suspend fun <T : Any> parseBody(clazz: KType, request: RoutingContext): T
}

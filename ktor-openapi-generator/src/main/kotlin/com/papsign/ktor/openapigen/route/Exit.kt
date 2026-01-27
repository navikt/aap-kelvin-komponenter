package com.papsign.ktor.openapigen.route

import io.ktor.server.routing.Route


//@ContextDsl
// TODO mark this as DSL
inline fun OpenAPIRoute<*>.exitAPI(crossinline fn: Route.() -> Unit) {
    ktorRoute.fn()
}

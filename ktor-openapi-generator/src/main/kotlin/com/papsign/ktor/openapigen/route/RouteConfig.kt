package com.papsign.ktor.openapigen.route

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.routing

/**
 * Wrapper for [io.ktor.server.routing.routing] to create the endpoints while configuring OpenAPI
 * documentation at the same time.
 */
fun Application.apiRouting(config: NormalOpenAPIRoute.() -> Unit) {
    routing {
        NormalOpenAPIRoute(
            this,
            application.plugin(OpenAPIGen).globalModuleProvider
        ).apply(config)
    }
}

/**
 * Wrapper for [io.ktor.server.routing.routing] to create the endpoints while configuring OpenAPI
 * documentation at the same time.
 *
 * @param config
 */
fun Route.apiRouting(config: NormalOpenAPIRoute.() -> Unit) {
    NormalOpenAPIRoute(
        this,
        application.plugin(OpenAPIGen).globalModuleProvider
    ).apply(config)
}


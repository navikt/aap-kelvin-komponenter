package com.papsign.ktor.openapigen.route.util

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext


fun Route.createConstantChild(): Route {
    return createChild(ConstantRouteSelector())
}

class ConstantRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext,
                                  segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Constant
    }
}

package com.papsign.ktor.openapigen.modules.providers

import com.papsign.ktor.openapigen.modules.OpenAPIModule

/**
 * Module interface for providing a custom description for the success response of an endpoint.
 *
 * Implement this interface and register the module on a route to override the description
 * that would otherwise be derived from the [@Response][com.papsign.ktor.openapigen.annotations.Response]
 * annotation or the default HTTP status description.
 *
 * @see com.papsign.ktor.openapigen.route.ResponseDescriptionModule
 */
interface ResponseDescriptionProvider : OpenAPIModule {
    val description: String
}

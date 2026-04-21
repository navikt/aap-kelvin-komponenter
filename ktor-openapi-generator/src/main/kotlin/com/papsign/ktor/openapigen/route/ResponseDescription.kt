package com.papsign.ktor.openapigen.route

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.model.operation.OperationModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.RouteOpenAPIModule
import com.papsign.ktor.openapigen.modules.openapi.OperationModule
import com.papsign.ktor.openapigen.modules.providers.ResponseDescriptionProvider

/**
 * Sets the description of the success response for this endpoint in the OpenAPI specification.
 *
 * Useful when the response type is a generic wrapper such as [List] and the [@Response][com.papsign.ktor.openapigen.annotations.Response]
 * annotation on the element type is not picked up automatically.
 *
 * Takes priority over the description from [@Response][com.papsign.ktor.openapigen.annotations.Response].
 *
 * Example:
 * ```
 * get<Unit, List<Sak>>(responseDescription("All saker for a person")) { ... }
 * ```
 *
 * @param description the description to show in the OpenAPI specification for the success response
 */
fun responseDescription(description: String): ResponseDescriptionModule = ResponseDescriptionModule(description)

/**
 * Module carrying a [description] for the success response.
 * Consumed by [com.papsign.ktor.openapigen.modules.handlers.ResponseHandlerModule] via [ResponseDescriptionProvider].
 */
data class ResponseDescriptionModule(override val description: String) :
    ResponseDescriptionProvider,
    RouteOpenAPIModule,
    OperationModule {
    override fun configure(apiGen: OpenAPIGen, provider: ModuleProvider<*>, operation: OperationModel) {
        // Description is read by ResponseHandlerModule via the ResponseDescriptionProvider interface.
    }
}

package com.papsign.ktor.openapigen.route.path.auth

import com.papsign.ktor.openapigen.modules.CachingModuleProvider
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.modules.registerModule
import com.papsign.ktor.openapigen.route.OpenAPIRoute
import com.papsign.ktor.openapigen.route.response.AuthResponseContextImpl
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineAuthContext
import io.ktor.server.routing.Route
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private val virtualThreadPerRequestDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

class OpenAPIAuthenticatedRoute<TAuth>(
    route: Route,
    provider: CachingModuleProvider = CachingModuleProvider(),
    val authProvider: AuthProvider<TAuth>
) : OpenAPIRoute<OpenAPIAuthenticatedRoute<TAuth>>(route, provider) {

    override fun child(route: Route): OpenAPIAuthenticatedRoute<TAuth> {
        return OpenAPIAuthenticatedRoute(route, provider.child(), authProvider)
    }

    @PublishedApi
    internal fun <TParams : Any, TResponse : Any, TRequest : Any> handle(
        paramsType: KType,
        responseType: KType,
        requestType: KType,
        body: suspend OpenAPIPipelineAuthContext<TAuth, TResponse>.(TParams, TRequest) -> Unit
    ) {
        child().apply {// child in case path is branch to prevent propagation of the mutable nature of the provider
            provider.registerModule(authProvider as AuthProvider<*>)
            handle<TParams, TResponse, TRequest>(
                paramsType,
                responseType,
                requestType
            ) { pipeline, responder, p, b ->
                withContext(virtualThreadPerRequestDispatcher) {
                    AuthResponseContextImpl<TAuth, TResponse>(
                        pipeline,
                        authProvider,
                        this@handle,
                        responder
                    ).body(p, b)
                }
            }
        }
    }

    @PublishedApi
    internal fun <TParams : Any, TResponse : Any> handle(
        paramsType: KType,
        responseType: KType,
        body: suspend OpenAPIPipelineAuthContext<TAuth, TResponse>.(TParams) -> Unit
    ) {
        child().apply {// child in case path is branch to prevent propagation of the mutable nature of the provider
            provider.registerModule(authProvider as AuthProvider<*>)
            handle<TParams, TResponse, Unit>(
                paramsType,
                responseType,
                typeOf<Unit>()
            ) { pipeline, responder, p: TParams, _ ->
                withContext(virtualThreadPerRequestDispatcher) {
                    AuthResponseContextImpl<TAuth, TResponse>(
                        pipeline,
                        authProvider,
                        this@handle,
                        responder
                    ).body(p)
                }
            }
        }
    }
}

package com.papsign.ktor.openapigen.route

import com.papsign.ktor.openapigen.APIException
import com.papsign.ktor.openapigen.modules.providers.ThrowInfoProvider
import com.papsign.ktor.openapigen.modules.registerModule
import com.papsign.ktor.openapigen.route.util.createConstantChild
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

data class ThrowsInfo(override val exceptions: List<APIException<*, *>>) : ThrowInfoProvider

/**
 * exists for simpler syntax
 */
inline fun <T: OpenAPIRoute<T>, reified EX : Throwable> T.throws(status: HttpStatusCode, @Suppress("UNUSED_PARAMETER") exClass: KClass<EX>, crossinline fn: T.() -> Unit = {}): T {
    return throws<T, EX>(status, fn)
}

inline fun <T: OpenAPIRoute<T>, reified EX : Throwable> T.throws(status: HttpStatusCode, crossinline fn: T.() -> Unit = {}): T {
    return throws<T, EX, Unit>(status, fn = fn)
}

/**
 * exists for simpler syntax
 */
inline fun <T: OpenAPIRoute<T>, reified EX : Throwable, reified B> T.throws(status: HttpStatusCode, example: B? = null, @Suppress("UNUSED_PARAMETER") exClass: KClass<EX>, crossinline fn: T.() -> Unit = {}): T {
    return throws<T, EX, B>(status, example, null, fn)
}

inline fun <T: OpenAPIRoute<T>, reified EX : Throwable, reified B> T.throws(status: HttpStatusCode, example: B? = null, noinline gen: ((EX) -> B)? = null, crossinline fn: T.() -> Unit = {}): T {
    return throws(APIException.apiException(status, example, gen), fn = fn)
}

/**
 * Route-scoped hook that runs on the [ApplicationCallPipeline.Monitoring] phase, replacing the
 * deprecated `Route.intercept(phase, block)` extension.
 */
private object ThrowsMonitoringHook : Hook<suspend PipelineContext<Unit, PipelineCall>.(Unit) -> Unit> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend PipelineContext<Unit, PipelineCall>.(Unit) -> Unit
    ) {
        pipeline.intercept(ApplicationCallPipeline.Monitoring, handler)
    }
}

@PublishedApi
internal fun throwsPlugin(responses: Array<out APIException<*, *>>) =
    createRouteScopedPlugin("ThrowsExceptionHandler") {
        val handler = makeExceptionHandler(responses)
        on(ThrowsMonitoringHook) {
            try {
                coroutineScope {
                    proceed()
                }
            } catch (exception: Throwable) {
                if (call.response.status() == null) {
                    handler(exception)
                    if (call.response.status() != null) {
                        finish()
                    }
                } else throw exception
            }
        }
    }

inline fun <T: OpenAPIRoute<T>> T.throws(vararg responses: APIException<*, *>, crossinline fn: T.() -> Unit = {}): T {
    return child(ktorRoute.createConstantChild()).apply {
        provider.registerModule(ThrowsInfo(responses.asList()))
        ktorRoute.install(throwsPlugin(responses))
        fn()
    }
}

fun makeExceptionHandler(info: Array<out APIException<*, *>>): suspend PipelineContext<*, PipelineCall>.(t: Throwable) -> Unit {
    val classes = info.associateBy { it.exceptionClass }
    fun findHandlerByType(clazz: KClass<*>): APIException<*, *>? {
        classes[clazz]?.let { return it }
        clazz.superclasses.forEach {
            findHandlerByType(it)?.let { return it }
        }
        return null
    }
    return { t: Throwable ->
        val handler: APIException<*, *> = findHandlerByType(t::class) ?: throw t
        // Safe: findHandlerByType matched `handler` by t::class (or a superclass), so `handler`'s
        // erased EX type is guaranteed compatible with `t` at runtime.
        @Suppress("UNCHECKED_CAST")
        val gen = handler.contentGen as ((Throwable) -> Any?)?
        val ex = handler.example
        when {
            gen != null -> {
                val ret = gen(t)
                if (ret != null) {
                    call.respond(handler.status, ret)
                } else {
                    call.respond(handler.status)
                }
            }
            ex != null -> {
                call.respond(handler.status, ex)
            }
            else -> {
                call.respond(handler.status)
            }
        }
    }
}

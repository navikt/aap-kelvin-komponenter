package no.nav.aap.komponenter.server.plugins

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.server.common.MdcKeys
import org.slf4j.MDC

/**
 * Hook som sikrer at bruker er autentisert før vi forsøker å hente ut NAVident
 * Se [io.ktor.server.auth.AuthenticationHook] for info om PipelinePhase("Authenticate")]
 */
private object NavIdentHook : Hook<suspend (ApplicationCall) -> Unit> {
    private val NavIdentHook: PipelinePhase = PipelinePhase("NavIdent")
    private val AuthenticatePhase: PipelinePhase = PipelinePhase("Authenticate")

    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit,
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, AuthenticatePhase)
        pipeline.insertPhaseAfter(AuthenticatePhase, NavIdentHook)
        pipeline.intercept(NavIdentHook) { handler(call) }
    }
}

/**
 * RouteScopedPlugin som legger til innlogget bruker sin ident på loggmeldinger
 */
public val NavIdentInterceptor: RouteScopedPlugin<Unit> = createRouteScopedPlugin(
    name = "NavIdentInterceptor"
) {
    on(NavIdentHook) { call ->
        runCatching {
            val bruker = call.bruker()

            MDC.put(MdcKeys.User, bruker.ident)
            call.attributes.put(AttributeKey<String>(MdcKeys.User), bruker.ident)
        }
        return@on
    }
}

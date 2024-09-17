package no.nav.aap.komponenter

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.generateOpenAPI
import no.nav.aap.behandlingsflyt.server.authenticate.authentication
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.util.UUID
import kotlin.collections.plus

/**
 * Installs common Ktor plugins:
 *  - MicrometerMetrics
 *  - ContentNegotiation
 *  - CallLogging
 *  - Sets up JWT authentication against Azure.
 *  - Genererer Swagger-dokumentasjon
 */
public fun Application.commonKtorModule(
    prometheus: PrometheusMeterRegistry, azureConfig: AzureConfig, swaggerTitle: String
) {
    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true))
    }
    install(CallLogging) {
        callIdMdc("callId")
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    authentication(azureConfig)

    generateOpenAPI(swaggerTitle)
}
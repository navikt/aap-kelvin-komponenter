package no.nav.aap.komponenter.server

import com.papsign.ktor.openapigen.model.info.InfoModel
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.generateOpenAPI
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.util.*

/**
 * Installs common Ktor plugins:
 *  - MicrometerMetrics
 *  - ContentNegotiation
 *  - CallLogging
 *  - Sets up JWT authentication against Azure.
 *  - Genererer Swagger-dokumentasjon
 */
public fun Application.commonKtorModule(
    prometheus: PrometheusMeterRegistry, azureConfig: AzureConfig, infoModel: InfoModel
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
        // For å unngå rare tegn i loggene
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    authentication(azureConfig)

    generateOpenAPI(
        infoModel = infoModel
    )
}
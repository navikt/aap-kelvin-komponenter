package no.nav.aap.komponenter.dbconnect

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode

internal val tracer = GlobalOpenTelemetry.getTracer("transaction")

internal inline fun <T> span(
    name: String,
    attributes: List<Pair<String, String>> = emptyList(),
    body: () -> T,
): T {
    val outerSpan = tracer.spanBuilder(name)
        .setSpanKind(SpanKind.INTERNAL)
        .apply {
            for ((key, value) in attributes) {
                setAttribute(key, value)
            }
        }
        .startSpan()
    try {
        return body()
    } catch (e: Throwable) {
        outerSpan.setStatus(StatusCode.ERROR)
        outerSpan.setAttribute(AttributeKey.stringKey("error.type"), e.javaClass.getCanonicalName())
        throw e
    } finally {
        outerSpan.end()
    }
}
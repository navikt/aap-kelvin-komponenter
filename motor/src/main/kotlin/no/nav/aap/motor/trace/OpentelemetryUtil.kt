package no.nav.aap.motor.trace

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import java.lang.RuntimeException
import java.util.function.UnaryOperator

internal object OpentelemetryUtil {
    private val TRACER: Tracer = GlobalOpenTelemetry.getTracer("kelvin-motor")

    fun <V> span(
        navn: String,
        behandlingId: Long?,
        sakId: Long?,
        jobbStatus: String,
        jobbId: String,
        spanBuilderTransformer: UnaryOperator<SpanBuilder>,
        block: () -> V
    ): V? {
        var spanBuilder: SpanBuilder =
            TRACER.spanBuilder(navn)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("jobbStatus", jobbStatus)
                .setAttribute("jobbId", jobbId)

        if (behandlingId != null) {
            spanBuilder = spanBuilder.setAttribute("behandlingId", behandlingId)
        }

        if (sakId != null) {
            spanBuilder = spanBuilder.setAttribute("sakId", sakId)
        }

        spanBuilder = spanBuilderTransformer.apply(spanBuilder)
        val span = spanBuilder.startSpan()

        try {
            span.makeCurrent().use { unused ->
                return block()
            }
        } catch (e: RuntimeException) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.javaClass.getSimpleName())
            throw e
        } finally {
            span.end()
        }
    }

    fun interface RunnableWithException {
        fun run()
    }
}
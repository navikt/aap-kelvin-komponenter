package no.nav.aap.motor.trace

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import java.lang.Exception
import java.lang.RuntimeException
import java.util.function.UnaryOperator

internal object OpentelemetryUtil {
    private val TRACER: Tracer = GlobalOpenTelemetry.getTracer("kelvin-motor")

    fun <V> span(navn: String, spanBuilderTransformer: UnaryOperator<SpanBuilder>, block: () -> V): V? {
        var spanBuilder: SpanBuilder = TRACER.spanBuilder(navn)
            .setSpanKind(SpanKind.INTERNAL)
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

    fun span(
        navn: String,
        spanBuilderTransformer: UnaryOperator<SpanBuilder>,
        runnable: RunnableWithException
    ) {
        var spanBuilder: SpanBuilder = TRACER.spanBuilder(navn)
            .setSpanKind(SpanKind.INTERNAL)
        spanBuilder = spanBuilderTransformer.apply(spanBuilder)
        val span = spanBuilder.startSpan()

        try {
            span.makeCurrent().use { _ ->
                runnable.run()
            }
        } catch (e: Exception) {
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
package no.nav.aap.motor.trace

import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.motor.JobbInput
import java.util.function.UnaryOperator

internal object JobbInfoSpanBuilder {
    fun jobbAttributter(jobbInput: JobbInput): UnaryOperator<SpanBuilder> {
        return UnaryOperator { spanBuilder: SpanBuilder? ->
            val builder = spanBuilder!!
                .setAttribute("jobbId", jobbInput.jobbId())
                .setAttribute("jobbType", jobbInput.type())
            builder.setSpanKind(SpanKind.INTERNAL)
        }
    }
}

package no.nav.aap.motor

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer

public fun MeterRegistry.motorFeiletTeller(jobbInput: JobbInput): Counter =
    this.counter("motor_jobb_feilet", listOf(ImmutableTag("type", jobbInput.type())))

public fun MeterRegistry.timer(jobbInput: JobbInput): Timer {
    return Timer.builder("motor_jobb_timer")
        .tags(listOf(ImmutableTag("type", jobbInput.type())))
        .publishPercentileHistogram()
        .register(this)
}

internal fun MeterRegistry.motorSchedulerTimer(): Timer =
    Timer.builder("motor_scheduler_timer")
        .publishPercentileHistogram()
        .register(this)

internal fun MeterRegistry.motorSchedulerFeiletTeller(): Counter =
    this.counter("motor_scheduler_feilet")
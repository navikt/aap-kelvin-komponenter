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

/**
 * Teller runder der en annen pod holdt skjedulerings-låsen. Forventet å være høy - det er
 * normaltilstanden for alle podder utenom én. Blir den null over tid samtidig som
 * motor_scheduler_timer også er tom, skjeduleres ingenting i det hele tatt.
 */
internal fun MeterRegistry.motorSchedulerHoppetOverTeller(): Counter =
    this.counter("motor_scheduler_hoppet_over")
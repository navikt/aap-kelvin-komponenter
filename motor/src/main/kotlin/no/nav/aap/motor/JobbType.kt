package no.nav.aap.motor

import no.nav.aap.motor.retry.RekjørFeiledeJobb

public object JobbType {
    private val jobber = HashMap<String, JobbSpesifikasjon>()

    init {
        jobber[RekjørFeiledeJobb.type] = RekjørFeiledeJobb
    }

    public fun leggTil(jobb: JobbSpesifikasjon) {
        jobber[jobb.type] = jobb
    }

    internal fun parse(type: String): JobbSpesifikasjon {
        return jobber.getValue(type)
    }

    internal fun cronTypes(): List<String> {
        return jobber.filter { it.value.cron != null }.keys.toList()
    }
}

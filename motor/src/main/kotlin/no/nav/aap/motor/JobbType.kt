package no.nav.aap.motor

import no.nav.aap.motor.retry.RekjørFeiledeJobb

internal object JobbType {
    private val jobber = HashMap<String, Jobb>()

    init {
        jobber[RekjørFeiledeJobb.type()] = RekjørFeiledeJobb
    }

    internal fun leggTil(jobb: Jobb) {
        jobber[jobb.type()] = jobb
    }

    internal fun parse(type: String): Jobb {
        return jobber.getValue(type)
    }

    internal fun cronTypes(): List<String> {
        return jobber.filter { it.value.cron() != null }.keys.toList()
    }
}

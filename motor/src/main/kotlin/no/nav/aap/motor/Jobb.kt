package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.cron.CronExpression

interface Jobb {

    fun konstruer(connection: DBConnection): JobbUtfører

    fun type(): String

    fun navn(): String

    fun beskrivelse(): String

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    fun retries(): Int {
        return 3
    }

    /**
     * ved fullføring vil oppgaven schedulere seg selv etter dette mønsteret
     */
    fun cron(): CronExpression? {
        return null
    }
}

package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.cron.CronExpression

public interface Jobb {

    public fun konstruer(connection: DBConnection): JobbUtfører

    public fun type(): String

    public fun navn(): String

    public fun beskrivelse(): String

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    public fun retries(): Int {
        return 3
    }

    /**
     * ved fullføring vil oppgaven schedulere seg selv etter dette mønsteret
     */
    public fun cron(): CronExpression? {
        return null
    }
}

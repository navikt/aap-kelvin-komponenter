package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.cron.CronExpression

/* For bakoverkompabilitet, bruk f.eks. ConnectionJobbSpesifikasjon i stedet. */
public interface Jobb: ConnectionJobbSpesifikasjon {
    public override fun konstruer(connection: DBConnection): JobbUtfører

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

    override val beskrivelse: String get() = beskrivelse()
    override val type: String get() = type()
    override val navn: String get() = navn()
    override val retries: Int get() = retries()
    override val cron: CronExpression? get() = cron()
}

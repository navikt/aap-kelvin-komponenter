package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.Prioritet

/**
 * Jobbtype som har satt en annen standardprioritet enn [Prioritet.NORMAL].
 * Brukes til å verifisere at typens default gjelder når innleggingen ikke overstyrer den.
 */
class LavPrioritetTestJobbUtfører : JobbUtfører {

    override fun utfør(input: JobbInput) {
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return LavPrioritetTestJobbUtfører()
        }

        override fun type(): String {
            return "lavPrioritet"
        }

        override fun navn(): String {
            return type()
        }

        override fun beskrivelse(): String {
            return type()
        }

        override fun prioritet(): Int {
            return Prioritet.LAV
        }
    }
}

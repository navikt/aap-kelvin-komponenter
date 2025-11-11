package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.ConnectionJobbSpesifikasjon
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.time.Duration

class AsynkronTullJobbUtfører() : JobbUtfører {

    override fun utfør(input: JobbInput) {
        println("Jobb utført")
    }

    companion object : ConnectionJobbSpesifikasjon {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return AsynkronTullJobbUtfører()
        }

        override val beskrivelse = "En beskrivelse for jobben"
        override val navn = "AsynkronTull Navn"
        override val type = "asynkron.tulljobb"
        override val retries = 10
        override val retryBackoffTid = Duration.ofMinutes(30)
    }
}

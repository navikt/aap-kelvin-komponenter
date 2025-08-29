package no.nav.aap.motor.retry

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.ConnectionJobbSpesifikasjon
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory

internal const val OPPGAVE_TYPE = "oppgave.retryFeilede"

internal class RekjørFeiledeJobb(private val repository: RetryFeiledeJobberRepository) : JobbUtfører {
    private val log = LoggerFactory.getLogger(RekjørFeiledeJobb::class.java)

    override fun utfør(input: JobbInput) {

        val feilendeOppgaverMarkertForRekjøring = repository.markerAlleFeiledeForKlare()
        log.info("Markert {} oppgaver for rekjøring", feilendeOppgaverMarkertForRekjøring)
    }

    companion object : ConnectionJobbSpesifikasjon   {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return RekjørFeiledeJobb(RetryFeiledeJobberRepository(connection))
        }

        override val type = OPPGAVE_TYPE

        override val navn = "Rekjør feilende jobber"

        override val beskrivelse =
            "Finner feilende jobber og markerer disse som klar for nytt forsøk ved hver kjøring av denne jobben."

        override val cron = CronExpression.create("0 0 7,12,15,20 * * *")

    }
}
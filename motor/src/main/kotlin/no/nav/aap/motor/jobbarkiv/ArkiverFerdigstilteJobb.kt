package no.nav.aap.motor.jobbarkiv

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.ConnectionJobbSpesifikasjon
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory

internal const val ARKIVER_FERDIGSTILTE_JOBB_TYPE = "jobber.arkiverFerdigstilte"

internal class ArkiverFerdigstilteJobb(private val repository: ArkiverFerdigstilteJobberRepository) : JobbUtfører {
    private val log = LoggerFactory.getLogger(ArkiverFerdigstilteJobb::class.java)

    override fun utfør(input: JobbInput) {
        if (repository.arkivtabellerFinnes()) {
            log.info("Starter på jobb for å arkivere ferdigstilte jobber")
            val antallArkiverteJobber = repository.arkiverFerdigstilteJobber()
            log.info("Arkivert {} jobber til jobbarkivet", antallArkiverteJobber)

        } else {
            log.info("Finnes ingen arkivtabeller - kan derfor ikke arkivere ferdigstilte jobber")
        }
    }

    companion object : ConnectionJobbSpesifikasjon {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ArkiverFerdigstilteJobb(ArkiverFerdigstilteJobberRepository(connection))
        }

        override val type = ARKIVER_FERDIGSTILTE_JOBB_TYPE

        override val navn = "Arkiver ferdigstilte jobber"

        override val beskrivelse =
            "Finner ferdigstilte jobber og flytter disse til arkivtabeller dersom det eksisterer."

        /**
         * Hver dag kl 01:00
         */
        override val cron = CronExpression.create("0 0 1 * * *")

    }
}
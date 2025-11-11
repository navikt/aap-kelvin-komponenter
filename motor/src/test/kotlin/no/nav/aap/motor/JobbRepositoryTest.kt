package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.AsynkronTullJobbUtfører
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.help.TøysOgTullTestJobbUtfører
import no.nav.aap.motor.help.TøysTestJobbUtfører
import no.nav.aap.motor.testutil.TestJobbRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class JobbRepositoryTest {

    @AutoClose
    private val dataSource = TestDataSource()

    init {
        JobbType.leggTil(TøysOgTullTestJobbUtfører)
        JobbType.leggTil(TøysTestJobbUtfører)
        JobbType.leggTil(TullTestJobbUtfører)
        JobbType.leggTil(AsynkronTullJobbUtfører)
    }

    @Test
    fun `skal plukke jobber på sak i en bestemt rekkefølge`() {
        val plukketIRekkefølge = LinkedList<JobbInput>()

        val last = LocalDateTime.now().minusMinutes(1)
        val second = LocalDateTime.now().minusHours(1)
        val first = LocalDateTime.now().minusDays(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører).medNesteKjøring(
                    last
                )
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører).medNesteKjøring(
                    second
                )
            )
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).medNesteKjøring(
                    first
                )
            )
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            var plukket = jobbRepository.plukkJobb()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                jobbRepository.markerSomFerdig(plukket)
                plukket = jobbRepository.plukkJobb()
            }
        }

        assertThat(plukketIRekkefølge).hasSize(3)
        assertThat(plukketIRekkefølge[0].type()).isEqualTo(TøysOgTullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[1].type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[2].type()).isEqualTo(TøysTestJobbUtfører.type())
    }

    @Test
    fun `skal forsøke fullføre en jobb som feiler før den prøver på neste`() {
        val plukketIRekkefølge = LinkedList<JobbInput>()

        val last = LocalDateTime.now().minusMinutes(1)
        val second = LocalDateTime.now().minusHours(1)
        val first = LocalDateTime.now().minusDays(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).medNesteKjøring(
                    last
                )
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører).medNesteKjøring(
                    second
                )
            )
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).medNesteKjøring(
                    first
                )
            )
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            var plukket = jobbRepository.plukkJobb()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                if (plukket.type() == TullTestJobbUtfører.type()) {
                    jobbRepository.markerSomFeilet(plukket, IllegalStateException())
                } else {
                    jobbRepository.markerSomFerdig(plukket)
                }
                plukket = jobbRepository.plukkJobb()
            }
        }

        assertThat(plukketIRekkefølge).hasSize(5)
        assertThat(plukketIRekkefølge[0].type()).isEqualTo(TøysOgTullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[1].type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[2].type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[3].type()).isEqualTo(TullTestJobbUtfører.type())
        assertThat(plukketIRekkefølge[4].type()).isEqualTo(TøysOgTullTestJobbUtfører.type())
    }


    @Test
    fun `skal oppdatere neste kjøring for en jobb som feiler med retryBackoffTid`() {
        val nå = LocalDateTime.now().minusMinutes(1)
        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(JobbInput(AsynkronTullJobbUtfører).medNesteKjøring(nå))
            jobbRepository.leggTil(JobbInput(TullTestJobbUtfører).medNesteKjøring(nå))
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.plukkJobb()?.let {
                jobbRepository.markerSomFeilet(it, IllegalStateException())
            }
            jobbRepository.plukkJobb()?.let {
                jobbRepository.markerSomFeilet(it, IllegalStateException())
            }
        }

        dataSource.transaction { connection ->
            val testJobbRepository = TestJobbRepository(connection)
            val asynkronTullJobber =
                testJobbRepository.hentJobberAvTypeMedAttributter(AsynkronTullJobbUtfører.type, null, null)
            val tullTestJobber =
                testJobbRepository.hentJobberAvTypeMedAttributter(TullTestJobbUtfører.type, null, null)

            /**
             * Verifiser at neste kjøring er etter "nå"
             */
            assertThat(asynkronTullJobber).hasSize(1)
            assertThat(asynkronTullJobber.first().nesteKjøring()).isAfter(LocalDateTime.now())
            assertThat(asynkronTullJobber.first().nesteKjøringTidspunkt()).isAfter(LocalDateTime.now())
            assertThat(asynkronTullJobber.first().antallRetriesForsøkt()).isEqualTo(1)

            /**
             * Verifiser at neste kjøring ikke er endret og derfor før "nå"
             */
            assertThat(tullTestJobber).hasSize(1)
            assertThat(tullTestJobber.first().nesteKjøring()).isBefore(LocalDateTime.now())
            assertThat(tullTestJobber.first().nesteKjøringTidspunkt()).isBefore(LocalDateTime.now())
            assertThat(tullTestJobber.first().antallRetriesForsøkt()).isEqualTo(1)
        }
    }

    @Test
    fun `kan telle riktig antall jobber`() {
        val typer = listOf(TøysOgTullTestJobbUtfører, TullTestJobbUtfører, TøysTestJobbUtfører)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)

            // Opprett noen jobber i forskjellige statuser
            repeat(2) {
                val jobbInput = JobbInput(typer.random())
                val dbId = jobbRepository.leggTil(jobbInput)
                jobbRepository.markerSomFerdig(jobbInput.medId(dbId)) // Får status FERDIG
            }
            repeat(3) {
                jobbRepository.leggTil(JobbInput(typer.random())) // Får status KLAR
            }

            // Kontroller at vi kan telle dem riktig
            val antallKlar = jobbRepository.antallJobber(JobbStatus.KLAR)
            assertThat(antallKlar).isEqualTo(3)
            val antallFerdig = jobbRepository.antallJobber(JobbStatus.FERDIG)
            assertThat(antallFerdig).isEqualTo(2)
        }

    }

}
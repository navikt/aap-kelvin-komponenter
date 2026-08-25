package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.LavPrioritetTestJobbUtfører
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.help.TøysOgTullTestJobbUtfører
import no.nav.aap.motor.help.TøysTestJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.LinkedList

private const val TAG = "tag"

class JobbPrioritetTest {

    @AutoClose
    private val dataSource = TestDataSource()

    init {
        JobbType.leggTil(TøysOgTullTestJobbUtfører)
        JobbType.leggTil(TøysTestJobbUtfører)
        JobbType.leggTil(TullTestJobbUtfører)
        JobbType.leggTil(LavPrioritetTestJobbUtfører)
    }

    @Test
    fun `jobb med høyest prioritet plukkes først selv om den er lagt inn sist`() {
        val forLengeSiden = LocalDateTime.now().minusDays(1)
        val nettopp = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(forLengeSiden)
                    .medPrioritet(Prioritet.LAV)
                    .medParameter(TAG, "lav")
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører)
                    .medNesteKjøring(nettopp)
                    .medPrioritet(Prioritet.KRITISK)
                    .medParameter(TAG, "kritisk")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("kritisk", "lav")
    }

    @Test
    fun `jobber med lik prioritet plukkes fortsatt etter neste_kjoring`() {
        val eldst = LocalDateTime.now().minusDays(1)
        val nyest = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(nyest)
                    .medPrioritet(Prioritet.HØY)
                    .medParameter(TAG, "nyest")
            )
            jobbRepository.leggTil(
                JobbInput(TullTestJobbUtfører)
                    .medNesteKjøring(eldst)
                    .medPrioritet(Prioritet.HØY)
                    .medParameter(TAG, "eldst")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("eldst", "nyest")
    }

    @Test
    fun `prioritet settes per instans og overstyrer jobbtypens default`() {
        val nesteKjøring = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // Samme jobbtype lagt inn to ganger, men med ulik prioritet. Dette er kjernen i
            // kravet: en automatisk opprettet jobb skal kunne nedprioriteres selv om en
            // manuelt opprettet jobb av nøyaktig samme type skal prioriteres.
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(nesteKjøring.minusHours(1))
                    .medPrioritet(Prioritet.LAV)
                    .medParameter(TAG, "automatisk")
            )
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(nesteKjøring)
                    .medPrioritet(Prioritet.KRITISK)
                    .medParameter(TAG, "manuelt")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("manuelt", "automatisk")
    }

    @Test
    fun `jobbtypens standardprioritet brukes når innleggingen ikke overstyrer den`() {
        val nesteKjøring = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(LavPrioritetTestJobbUtfører)
                    .medNesteKjøring(nesteKjøring.minusHours(1))
                    .medParameter(TAG, "lav-som-default")
            )
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(nesteKjøring)
                    .medParameter(TAG, "normal-som-default")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("normal-som-default", "lav-som-default")
    }

    @Test
    fun `prioritet lagres og leses tilbake fra databasen`() {
        dataSource.transaction { connection ->
            JobbRepository(connection).leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .medNesteKjøring(LocalDateTime.now().minusMinutes(1))
                    .medPrioritet(Prioritet.KRITISK)
            )
        }

        dataSource.transaction { connection ->
            JobbRepository(connection).skjedulerJobber()
            val plukket = JobbRepository(connection).plukkJobbV2()

            assertThat(plukket).isNotNull
            assertThat(plukket!!.prioritet()).isEqualTo(Prioritet.KRITISK)
        }
    }

    @Test
    fun `prioritet påvirker ikke rekkefølgen innad i en eksklusivitetsgruppe`() {
        val eldst = LocalDateTime.now().minusDays(1)
        val nyest = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // Samme sak og samme type => samme eksklusivitetsgruppe. Rekkefølgegarantien er
            // sterkere enn prioritet, så den eldste jobben skal kjøre først selv om den yngre
            // har langt høyere prioritet.
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .forSak(1L)
                    .medNesteKjøring(eldst)
                    .medPrioritet(Prioritet.BAKGRUNN)
                    .medParameter(TAG, "eldst-lav-prioritet")
            )
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .forSak(1L)
                    .medNesteKjøring(nyest)
                    .medPrioritet(Prioritet.KRITISK)
                    .medParameter(TAG, "nyest-høy-prioritet")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("eldst-lav-prioritet", "nyest-høy-prioritet")
    }

    @Test
    fun `lav-prioritet-jobb med ulik jobbtype og samme behandling sperrer ikke for høy-prioritets-jobb`() {
        val eldst = LocalDateTime.now().minusDays(1)
        val nyest = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // Samme sak og samme type => samme eksklusivitetsgruppe. Rekkefølgegarantien er
            // sterkere enn prioritet, så den eldste jobben skal kjøre først selv om den yngre
            // har langt høyere prioritet.
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .forBehandling(1L, 1L)
                    .medNesteKjøring(eldst)
                    .medPrioritet(Prioritet.BAKGRUNN)
                    .medParameter(TAG, "eldst-lav-prioritet-jobbX")
            )
            jobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører)
                    .forBehandling(1L, 1L)
                    .medNesteKjøring(nyest)
                    .medPrioritet(Prioritet.KRITISK)
                    .medParameter(TAG, "nyest-høy-prioritet-jobbY")
            )
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.skjedulerJobber()
            val plukket = jobbRepository.plukkJobbV2()
            assertThat(plukket).isNotNull
            assertThat(plukket!!.parameter(TAG)).isEqualTo("nyest-høy-prioritet-jobbY")
        }
    }

    @Test
    fun `prioritet avgjør rekkefølgen på tvers av eksklusivitetsgrupper`() {
        val eldst = LocalDateTime.now().minusDays(1)
        val nyest = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // Ulike saker => ulike eksklusivitetsgrupper. Begge blir kjørbare i samme
            // skjedulering, og da er det prioritet som avgjør hvem som plukkes først.
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .forSak(1L)
                    .medNesteKjøring(eldst)
                    .medPrioritet(Prioritet.LAV)
                    .medParameter(TAG, "sak-1-lav")
            )
            jobbRepository.leggTil(
                JobbInput(TøysTestJobbUtfører)
                    .forSak(2L)
                    .medNesteKjøring(nyest)
                    .medPrioritet(Prioritet.KRITISK)
                    .medParameter(TAG, "sak-2-kritisk")
            )
        }

        val rekkefølge = plukkAlle()

        assertThat(rekkefølge).containsExactly("sak-2-kritisk", "sak-1-lav")
    }

    /**
     * Kjører skjedulering og plukking om hverandre til køen er tom, og returnerer taggene i
     * den rekkefølgen jobbene ble plukket. Skjeduleringen må gjentas fordi en eksklusivitets-
     * gruppe først slipper fram neste jobb etter at den forrige er ferdig.
     */
    private fun plukkAlle(): List<String> {
        val rekkefølge = LinkedList<String>()

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            while (true) {
                jobbRepository.skjedulerJobber()
                val plukket = jobbRepository.plukkJobbV2() ?: break
                rekkefølge.add(plukket.parameter(TAG))
                jobbRepository.markerSomFerdig(plukket)
            }
        }

        return rekkefølge
    }
}

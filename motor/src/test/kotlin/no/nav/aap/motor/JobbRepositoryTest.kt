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
            jobbRepository.skjedulerJobber()
            var plukket = jobbRepository.plukkJobbV2()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                jobbRepository.markerSomFerdig(plukket)
                plukket = jobbRepository.plukkJobbV2()
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
            jobbRepository.skjedulerJobber()
            var plukket = jobbRepository.plukkJobbV2()
            while (plukket != null) {
                plukketIRekkefølge.add(plukket)
                if (plukket.type() == TullTestJobbUtfører.type()) {
                    jobbRepository.markerSomFeilet(plukket, IllegalStateException())
                } else {
                    jobbRepository.markerSomFerdig(plukket)
                }
                plukket = jobbRepository.plukkJobbV2()
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
            jobbRepository.skjedulerJobber()
            jobbRepository.plukkJobbV2()?.let {
                jobbRepository.markerSomFeilet(it, IllegalStateException())
            }
            jobbRepository.skjedulerJobber()
            jobbRepository.plukkJobbV2()?.let {
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
    fun `skal ikke gjøre søsken-jobb i samme eksklusivitetsgruppe kjørbar mens en jobb venter på retryBackoffTid`() {
        val sakId = 42L
        val tidligst = LocalDateTime.now().minusDays(1)
        val senest = LocalDateTime.now().minusHours(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            jobbRepository.leggTil(
                JobbInput(AsynkronTullJobbUtfører).forSak(sakId).medNesteKjøring(tidligst)
            )
            jobbRepository.leggTil(
                JobbInput(AsynkronTullJobbUtfører).forSak(sakId).medNesteKjøring(senest)
            )
        }

        val ider = mutableListOf<Long>()
        dataSource.transaction { connection ->
            val testJobbRepository = TestJobbRepository(connection)
            val jobber = testJobbRepository
                .hentJobberAvTypeMedAttributter(AsynkronTullJobbUtfører.type, sakId, null)
                .sortedBy { it.nesteKjøring() }
            ider.addAll(jobber.map { it.jobbId() })
        }
        val xId = ider[0]
        val yId = ider[1]

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)

            // App1: skjedulerer og plukker X (den med tidligst neste_kjoring)
            jobbRepository.skjedulerJobber()
            val plukketX = jobbRepository.plukkJobbV2()
            assertThat(plukketX).isNotNull
            assertThat(plukketX!!.jobbId()).isEqualTo(xId)

            // X feiler og settes i backoff - neste_kjoring flyttes forbi Y sin neste_kjoring
            jobbRepository.markerSomFeilet(plukketX, IllegalStateException("simulert feil"))

            // App2 (simulert): skjedulerer på nytt. Y skal IKKE bli forfremmet selv om
            // X sin neste_kjoring nå er senere enn Y sin.
            jobbRepository.skjedulerJobber()
        }

        dataSource.transaction { connection ->
            val kjørbareRader = connection.queryList(
                "select id, kjorbar from jobb where sak_id = ? order by id"
            ) {
                setParams { setLong(1, sakId) }
                setRowMapper { row -> row.getLong("id") to row.getBoolean("kjorbar") }
            }

            val aktive = kjørbareRader.filter { it.second }
            // Kun X skal fortsatt eie eksklusivitets-slotten, aldri begge samtidig.
            assertThat(aktive).hasSize(1)
            assertThat(aktive.single().first).isEqualTo(xId)

            // Y skal fortsatt være blokkert (ikke kjørbar)
            assertThat(kjørbareRader.first { it.first == yId }.second).isFalse()
        }

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // Ingen jobb skal kunne plukkes nå: X er ikke forfalt (backoff), Y er ikke kjørbar
            assertThat(jobbRepository.plukkJobbV2()).isNull()
        }
    }

    @Test
    fun `selvstendige jobber av samme type skal kunne være kjørbare samtidig`() {
        val nå = LocalDateTime.now().minusMinutes(1)

        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            // To selvstendige jobber (uten sak_id/behandling_id) av samme type - skal IKKE
            // behandles som en eksklusivitetsgruppe. Sikkerhetsnett-indeksen (UX_JOBB_EKSKLUSIV_AKTIV)
            // må derfor ekskludere disse, ellers ville denne transaksjonen feilet med
            // unique constraint violation.
            jobbRepository.leggTil(JobbInput(AsynkronTullJobbUtfører).medNesteKjøring(nå))
            jobbRepository.leggTil(JobbInput(AsynkronTullJobbUtfører).medNesteKjøring(nå))
            jobbRepository.skjedulerJobber()
        }

        dataSource.transaction { connection ->
            val kjørbareRader = connection.queryList(
                "select id, kjorbar from jobb where type = ? and sak_id is null and behandling_id is null"
            ) {
                setParams { setString(1, AsynkronTullJobbUtfører.type) }
                setRowMapper { row -> row.getBoolean("kjorbar") }
            }

            assertThat(kjørbareRader).hasSize(2)
            assertThat(kjørbareRader).allMatch { it }
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

    @Test
    fun `hentTilleggsinfo returnerer tom tilleggsinfo hvis ingen finnes`() {
        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            val jobbId = jobbRepository.leggTil(JobbInput(TøysTestJobbUtfører))

            val tilleggsinfo = jobbRepository.hentTilleggsinfo(jobbId)
            assertThat(tilleggsinfo).isNotNull()
            assertThat(tilleggsinfo.kommentarer).isEmpty()
        }
    }

    @Test
    fun `leggTilKommentar lagrer kommentar og hentTilleggsinfo returnerer den`() {
        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            val jobbId = jobbRepository.leggTil(JobbInput(TøysTestJobbUtfører))

            jobbRepository.leggTilKommentar(jobbId, Kommentar.ny(skrevetAv = "z123456", tekst = "Ser på saken"))

            val hentet = jobbRepository.hentTilleggsinfo(jobbId)
            assertThat(hentet.kommentarer).hasSize(1)
            assertThat(hentet.kommentarer.first().skrevetAv).isEqualTo("z123456")
            assertThat(hentet.kommentarer.first().tekst).isEqualTo("Ser på saken")
        }
    }

    @Test
    fun `kommentarer akkumuleres ved flere kall til leggTilKommentar`() {
        dataSource.transaction { connection ->
            val jobbRepository = JobbRepository(connection)
            val jobbId = jobbRepository.leggTil(JobbInput(TøysTestJobbUtfører))

            jobbRepository.leggTilKommentar(jobbId, Kommentar.ny(skrevetAv = "z111111", tekst = "Første kommentar"))
            jobbRepository.leggTilKommentar(jobbId, Kommentar.ny(skrevetAv = "z222222", tekst = "Andre kommentar"))

            val hentet = jobbRepository.hentTilleggsinfo(jobbId)
            assertThat(hentet.kommentarer).hasSize(2)
            assertThat(hentet.kommentarer.map { it.tekst }).containsExactly("Første kommentar", "Andre kommentar")
        }
    }
}

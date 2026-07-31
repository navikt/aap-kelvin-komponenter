package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.TøysOgTullTestJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FlytJobbRepositoryImplTest {
    @AutoClose
    private val dataSource = TestDataSource()

    init {
        JobbType.leggTil(TøysOgTullTestJobbUtfører)
    }

    @Test
    fun hentJobberMedHistorikkForSak() {
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                JobbInput(TøysOgTullTestJobbUtfører).forSak(1L)
            )
            val jobber = flytJobbRepository.hentJobberMedHistorikkForSak(1L)

            val fakeTidspunkt = LocalDateTime.now()
            assertEquals(1, jobber.size)
            assertThat(jobber.single().historikk).hasSize(1)
            assertThat(jobber[0].historikk[0].copy(opprettet = fakeTidspunkt)).isEqualTo(
                JobbHistorikk(
                    jobbId = 1,
                    status = JobbStatus.KLAR,
                    opprettet = fakeTidspunkt,
                    feilmelding = null
                )
            )
        }

        dataSource.transaction { connection ->
            JobbRepository(connection).skjedulerJobber()
            JobbRepository(connection).plukkJobbV2()
            
            val flytJobbRepository = FlytJobbRepository(connection)
            val jobber = flytJobbRepository.hentJobberMedHistorikkForSak(1L)
            
            val fakeTidspunkt = LocalDateTime.now()
            assertEquals(1, jobber.size)
            assertThat(jobber.single().historikk).hasSize(2)
            assertThat(jobber[0].historikk.map{it.copy(opprettet = fakeTidspunkt)}).containsExactly(
                JobbHistorikk(
                    jobbId = 1,
                    status = JobbStatus.KLAR,
                    opprettet = fakeTidspunkt,
                    feilmelding = null
                ),
                JobbHistorikk(
                    jobbId = 1,
                    status = JobbStatus.PLUKKET,
                    opprettet = fakeTidspunkt,
                    feilmelding = null
                )
            )
        }
    }
}
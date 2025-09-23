package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.help.TøysOgTullTestJobbUtfører
import no.nav.aap.motor.help.TøysTestJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class JobbRepositoryTest {

    private val dataSource = InitTestDatabase.freshDatabase()

    init {
        JobbType.leggTil(TøysOgTullTestJobbUtfører)
        JobbType.leggTil(TøysTestJobbUtfører)
        JobbType.leggTil(TullTestJobbUtfører)
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
}
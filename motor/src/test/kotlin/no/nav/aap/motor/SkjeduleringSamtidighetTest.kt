package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.TullTestJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Tester samtidigheten i [JobbRepository.skjedulerJobber] med ekte parallelle transaksjoner.
 *
 * Uten advisory locken ville denne testen hengt til `queryTimeout` (30 s) slo inn. Derfor er
 * [Timeout] satt lavere enn det - en regresjon gir timeout, ikke en tilfeldig treg test.
 */
class SkjeduleringSamtidighetTest {

    @AutoClose
    private val dataSource = TestDataSource()

    init {
        JobbType.leggTil(TullTestJobbUtfører)
    }

    @Test
    @Timeout(20)
    fun `skjedulering hopper over når en annen transaksjon holder advisory locken`() {
        val jobbId = dataSource.transaction { connection ->
            JobbRepository(connection).leggTil(
                JobbInput(TullTestJobbUtfører).medNesteKjøring(LocalDateTime.now().minusMinutes(1))
            )
        }

        val låsenErTatt = CountDownLatch(1)
        val slippLåsen = CountDownLatch(1)
        val resultatILåsholder = AtomicReference<SkjeduleringResultat>()
        val feilILåsholder = AtomicReference<Throwable>()

        val låsholder = Thread {
            try {
                dataSource.transaction { connection ->
                    resultatILåsholder.set(JobbRepository(connection).skjedulerJobber())
                    låsenErTatt.countDown()
                    // Holder transaksjonen - og dermed advisory locken - åpen.
                    slippLåsen.await(15, TimeUnit.SECONDS)
                }
            } catch (e: Throwable) {
                feilILåsholder.set(e)
                låsenErTatt.countDown()
            }
        }
        låsholder.start()

        try {
            assertThat(låsenErTatt.await(15, TimeUnit.SECONDS)).isTrue()
            assertThat(feilILåsholder.get()).isNull()

            /* Det avgjørende: dette kallet må returnere umiddelbart i stedet for å vente på
               låsholderen. pg_try_advisory_xact_lock venter aldri. */
            val resultat = dataSource.transaction { connection ->
                JobbRepository(connection).skjedulerJobber()
            }

            assertThat(resultat).isEqualTo(SkjeduleringResultat.HoppetOver)
        } finally {
            slippLåsen.countDown()
            låsholder.join(15_000)
        }

        assertThat(resultatILåsholder.get()).isInstanceOf(SkjeduleringResultat.Utført::class.java)
        assertThat(erKjørbar(jobbId)).isTrue()
    }

    private fun erKjørbar(jobbId: Long): Boolean =
        dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst("select kjorbar from jobb where id = ?") {
                setParams { setLong(1, jobbId) }
                setRowMapper { row -> row.getBoolean("kjorbar") }
            }
        }
}

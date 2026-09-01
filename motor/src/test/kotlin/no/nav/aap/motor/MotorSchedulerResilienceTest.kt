package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MotorSchedulerResilienceTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun afterEach() {
        dataSource.close()
    }

    /**
     * Regresjon: I V2 er det kun Scheduler som setter `kjorbar = true`, og plukkJobbV2 plukker
     * bare kjørbare jobber. `scheduleWithFixedDelay` avbryter periodisk kjøring permanent dersom
     * en kjøring kaster noe som slipper ut. Tidligere fanget Scheduler kun `Exception`, så en
     * `Error` (f.eks. OOM) ville drept scheduleren for godt -> ingen jobber ble plukket før restart.
     *
     * Her kaster `enableV2` en Error de første kjøringene. Etter fiksen (catch Throwable) skal
     * scheduleren overleve og til slutt markere jobben kjørbar, slik at den blir prosessert.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `scheduler overlever Error fra en tick og fortsetter å markere jobber kjørbare`() {
        val kallTeller = AtomicInteger(0)
        val antallSomKaster = 8

        val motor = MotorImpl(
            dataSource = dataSource,
            antallKammer = 2,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører),
            enableV2 = {
                if (kallTeller.getAndIncrement() < antallSomKaster) {
                    throw OutOfMemoryError("simulert Error i skjedulering/plukking")
                }
                true
            },
        )

        val payload = UUID.randomUUID().toString()
        dataSource.transaction {
            JobbRepository(it).leggTil(JobbInput(TullTestJobbUtfører).medPayload(payload))
        }

        motor.start()

        try {
            val svar = ventPåVerdiITestTabell(maxSekunder = 25)
            assertThat(svar)
                .withFailMessage("Jobben ble aldri plukket - scheduleren døde sannsynligvis av Error-en")
                .isEqualTo(payload)
        } finally {
            motor.stop()
        }
    }

    private fun ventPåVerdiITestTabell(maxSekunder: Long): String? {
        val slutt = LocalDateTime.now().plusSeconds(maxSekunder)
        while (LocalDateTime.now().isBefore(slutt)) {
            val verdi = dataSource.transaction(readOnly = true) { conn: DBConnection ->
                conn.queryFirstOrNull("SELECT value FROM TEST_TABLE") {
                    setRowMapper { it.getString("value") }
                }
            }
            if (verdi != null) return verdi
            Thread.sleep(100)
        }
        return null
    }
}

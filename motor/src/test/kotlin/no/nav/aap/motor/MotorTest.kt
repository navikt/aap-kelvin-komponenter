package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger(MotorTest::class.java)

class MotorTest {
    private val dataSource = InitTestDatabase.dataSource

    @Test
    fun `test å kjøre en enkel jobb`() {
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 2,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører)
        )

        motor.start()

        val randomString = UUID.randomUUID().toString()

        dataSource.transaction {
            JobbRepository(it).leggTil(JobbInput(TullTestJobbUtfører).medPayload(randomString))
        }

        ventPåSvarITestTabell()

        val writtenValue = dataSource.transaction {
            it.queryFirst("SELECT * FROM TEST_TABLE") {
                setRowMapper { it.getString("value") }
            }
        }

        assertThat(writtenValue).isEqualTo(randomString)

        motor.stop()
    }

    private fun ventPåSvarITestTabell() {
        val timeInMillis = measureTimeMillis {
            dataSource.transaction(readOnly = true) {
                val max = LocalDateTime.now().plusSeconds(30)
                val x = {
                    it.queryFirst<Int>("SELECT COUNT(*) FROM TEST_TABLE") {
                        setRowMapper { it.getInt("count") }
                    }
                }
                while (x.invoke() < 1 && max.isAfter(LocalDateTime.now())) {
                    Thread.sleep(50L)
                }
            }
        }
        logger.info("Waited $timeInMillis millis.")
    }

}
package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger(MotorTest::class.java)

class MotorTest {
    private val dataSource = InitTestDatabase.dataSource

    @BeforeEach
    fun beforeEach() {
        InitTestDatabase.clean()
        InitTestDatabase.migrate()
    }

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

        val svare = ventPåSvarITestTabell {
            it.queryFirstOrNull("SELECT value FROM TEST_TABLE") {
                setRowMapper { (it.getString("value")) }
            }
        }

        assertThat(svare).isEqualTo(randomString)

        motor.stop()
    }

    @Test
    fun `naiv last-test for motor`() {
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 8,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører)
        )

        val antallJobber = 1000

        (1..antallJobber).forEach { _ ->
            dataSource.transaction { conn ->
                val randomString = UUID.randomUUID().toString()
                JobbRepository(conn).leggTil(JobbInput(TullTestJobbUtfører).medPayload(randomString))
            }
        }

        motor.start()

        ventPåSvarITestTabell {
            val count = it.queryFirst("SELECT COUNT(*) FROM TEST_TABLE") {
                setRowMapper { it.getInt("count") }
            }
            if (count == antallJobber) count else null
        }
        motor.stop()
    }

    private fun <E> ventPåSvarITestTabell(x: (conn: DBConnection) -> E?): E {
        var res: E? = null
        val timeInMillis = measureTimeMillis {
            dataSource.transaction(readOnly = true) {
                val max = LocalDateTime.now().plusSeconds(300)
                var found = false
                while (!found && max.isAfter(LocalDateTime.now())) {
                    val tried = x.invoke(it)
                    if (tried != null) {
                        found = true
                        res = tried
                    } else {
                        Thread.sleep(50L)
                    }
                }
            }
        }
        logger.info("Waited $timeInMillis millis.")
        return requireNotNull(res)
    }
}
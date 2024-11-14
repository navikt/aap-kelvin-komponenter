package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.help.RekkefølgeTestJobbUtfører
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.testutil.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger(MotorTest::class.java)

class MotorTest {
    private val dataSource = InitTestDatabase.dataSource

    private val util = TestUtil(dataSource, JobbType.cronTypes())

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
    fun `prøver igjen retries ganger`() {
        val antallRetries = 10
        var x = 0
        val jobbutfører = object : Jobb {
            override fun konstruer(connection: DBConnection): JobbUtfører {
                return object : JobbUtfører {
                    override fun utfør(input: JobbInput) {
                        x++
                        throw IllegalStateException("test")
                    }
                }
            }

            override fun type(): String {
                return "type"
            }

            override fun navn(): String {
                return "navn"
            }

            override fun beskrivelse(): String {
                return "beskrivelse"
            }

            override fun retries(): Int {
                return antallRetries
            }
        }
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 2,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(jobbutfører)
        )

        dataSource.transaction {
            JobbRepository(it).leggTil(
                JobbInput(jobbutfører)
            )
        }

        motor.start()

        util.ventPåSvar()

        assertThat(x).isEqualTo(antallRetries)
    }

    @Test
    fun `burde ikke feile om to jobber for samme behandling starter samtidig, men heller legge i kø`() {
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 5,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(RekkefølgeTestJobbUtfører)
        )

        dataSource.transaction { conn ->
            (1..100).forEach {
                JobbRepository(conn).leggTil(
                    JobbInput(RekkefølgeTestJobbUtfører).medPayload(it.toString()).forBehandling(0, 1)
                )
            }
        }


        motor.start()

        util.ventPåSvar()

        val svare = ventPåSvarITestTabell { conn ->
            val x = conn.queryFirstOrNull("SELECT count(*) FROM ORDER_TABLE") {
                setRowMapper { (it.getLong("count")) }
            }
            if (x == 100L) x else null
        }

        assertThat(svare).isEqualTo(100L)

        val innsattData = dataSource.transaction {
            it.queryList("SELECT value, trad_navn, opprettet_tid FROM ORDER_TABLE ORDER BY opprettet_tid") {
                setRowMapper {
                    Triple(it.getString("trad_navn"), it.getLocalDateTime("OPPRETTET_TID"), it.getString("value"))
                }
            }
        }

        // Verifiser at vi faktisk kjører på to kjerner
        //assertThat(innsattData.map { it.first }.toSet().size).isGreaterThan(1)
        assertThat(innsattData.map { it.second }).isSorted()
        assertThat(innsattData.map { it.third }.map { it.toInt() }).isSorted()

        motor.stop()
    }

    // Har timeout her for å feile om ting begynner å ta tid
    @Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS)
    @Test
    fun `naiv last-test for motor`() {
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 8,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører)
        )

        val antallJobber = 1000

        dataSource.transaction { conn ->
            (1..antallJobber).forEach { _ ->
                val randomString = UUID.randomUUID().toString()
                JobbRepository(conn).leggTil(JobbInput(TullTestJobbUtfører).medPayload(randomString))
            }
        }

        motor.start()

        util.ventPåSvar()

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
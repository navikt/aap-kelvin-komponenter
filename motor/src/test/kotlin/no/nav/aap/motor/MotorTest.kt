package no.nav.aap.motor

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.help.RekkefølgeTestJobbUtfører
import no.nav.aap.motor.help.EnAnnenRekkefølgeTestJobbUtfører
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.testutil.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger(MotorTest::class.java)

class MotorTest {
    @AutoClose
    private val dataSource = InitTestDatabase.freshDatabase()

    @BeforeEach
    fun beforeEach() {
        val flyway = InitTestDatabase.flywayFor(dataSource)
        flyway.clean()
        flyway.migrate()
    }

    private val util = TestUtil(dataSource, JobbType.cronTypes())

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

        InitTestDatabase.closerFor(dataSource)
    }

    @Test
    fun `prøver igjen retries ganger`() {
        val antallRetries = 5
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
        val prometheus = SimpleMeterRegistry()
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 2,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(jobbutfører),
            prometheus = prometheus
        )

        dataSource.transaction {
            JobbRepository(it).leggTil(
                JobbInput(jobbutfører)
            )
        }

        motor.start()

        util.ventPåSvar()

        assertThat(x).isEqualTo(antallRetries)
        assertThat(prometheus.counter("motor_jobb_feilet", "type", "type").count()).isEqualTo(1.0)

        motor.stop()
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

        val resultat = dataSource.transaction {
            it.queryList("SELECT value, trad_navn, opprettet_tid FROM ORDER_TABLE ORDER BY opprettet_tid") {
                setRowMapper(mapOrderResultat())
            }
        }

        // Verifiser at vi faktisk kjører på flere kjerner - dette fungerer ikke nå fordi det ligger en Thread.sleep(500)
        // som blokkerer alle andre forbrenningskammer enn den som først starter å konsumere dersom de ikke får resultat
        // når plukkJobb kalles. Burde se på en løsning for å konfigurere dette.
        //assertThat(resultat.map { it.trådNavn }.toSet().size).isGreaterThan(1)
        assertThat(resultat.map { it.opprettet }).isSorted()
        assertThat(resultat.map { it.value }.map { it.toInt() }).isSorted()

        motor.stop()
    }

    @Test
    fun `skal kunne prosessere jobber som håndterer samme behandling samtidig hvis jobb er av ulik type`() {
        val separator = "|"
        val jobb1 = RekkefølgeTestJobbUtfører
        val jobb2 = EnAnnenRekkefølgeTestJobbUtfører

        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 5,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(jobb1, jobb2)
        )

        var nesteKjøring = LocalDateTime.now().minusDays(1)

        // Oppretter oppgaver for både jobb1 og for jobb2
        dataSource.transaction { conn ->
            (1..50).forEach { counter ->
                nesteKjøring = nesteKjøring.plusSeconds(1)
                JobbRepository(conn).leggTil(
                    JobbInput(jobb1).medPayload("${jobb1.navn}$separator$counter").medNesteKjøring(nesteKjøring).forBehandling(0, 1)
                )
            }
            (51..100).forEach { counter ->
                nesteKjøring = nesteKjøring.plusSeconds(1)
                JobbRepository(conn).leggTil(
                    JobbInput(jobb2).medPayload("${jobb2.navn}$separator$counter").medNesteKjøring(nesteKjøring).forBehandling(0, 1)
                )
            }
        }

        motor.start()

        util.ventPåSvar()

        motor.stop()

        val resultat = dataSource.transaction {
            it.queryList("SELECT value, trad_navn, opprettet_tid FROM ORDER_TABLE ORDER BY opprettet_tid") {
                setRowMapper(mapOrderResultat())
            }
        }

        // Verifisere at alle opprettede jobber er prosessert
        assertThat(resultat.size).isEqualTo(100L)

        // Verifiser at vi faktisk kjørte på flere kjerner ved å sjekke distinkte trådnavn
        assertThat(resultat.map { it.trådNavn }.toSet().size).isGreaterThan(1)

        // Sjekker at hver jobbtype kjører i rekkefølge når de er gruppert innenfor en behandling
        assertThat(resultat.filter { it.value.startsWith(jobb1.navn) }.map { it.value.split(separator)[1].toInt()} ).isSorted()
        assertThat(resultat.filter { it.value.startsWith(jobb2.navn) }.map { it.value.split(separator)[1].toInt() }).isSorted()

        // Sjekker så at de to jobbtypene prosesseres om hverandre og at resultatet i helhet derfor ikke er sortert
        val values = resultat.map { it.value.split(separator)[1].toInt() }
        assertThat(values).isNotEqualTo(values.sorted())

        motor.stop()
    }

    // Har timeout her for å feile om ting begynner å ta tid
    @Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    @Test
    fun `naiv last-test for motor som skal unngå duplikat-kjøring av jobber`() {
        val motor = Motor(
            dataSource = dataSource,
            antallKammer = 8,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører)
        )

        val antallJobber = 1000

        dataSource.transaction { conn ->
            (1..antallJobber).forEach { verdi ->
                JobbRepository(conn).leggTil(JobbInput(TullTestJobbUtfører).medPayload(verdi))
            }
        }

        motor.start()

        util.ventPåSvar()

        val verdier: List<String> = dataSource.transaction { conn ->
             conn.queryList<String>("""
                select * from test_table 
            """.trimIndent()){
                setRowMapper { row -> row.getString("value") }
            }
        }
        assertThat(verdier).isEqualTo(verdier.distinct()).withFailMessage("Skal ikke ha duplikate verdier i test_table-tabellen - da har samme jobb blitt plukket og kjørt flere ganger")
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

    private fun mapOrderResultat(): (Row) -> OrderResultat = { row ->
        OrderResultat(
            value = row.getString("value"),
            opprettet = row.getLocalDateTime("opprettet_tid"),
            trådNavn = row.getString("trad_navn"),
        )
    }

    private data class OrderResultat(val value: String, val opprettet: LocalDateTime, val trådNavn: String)

}
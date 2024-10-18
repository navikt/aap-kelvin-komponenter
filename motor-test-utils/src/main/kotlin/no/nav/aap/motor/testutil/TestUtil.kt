package no.nav.aap.motor.testutil

import no.nav.aap.komponenter.dbconnect.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

public class TestUtil(private val datasource: DataSource, private val cronTypes: List<String>) {
    private val log = LoggerFactory.getLogger(TestUtil::class.java)

    public fun ventPåSvar(sakId: Long? = null, behandlingId: Long? = null, maxTid: Long = 20) {
        val timeInMillis = measureTimeMillis {
            datasource.transaction(readOnly = true) {
                val maxTid = LocalDateTime.now().plusSeconds(maxTid)
                val testJobbRepository = TestJobbRepository(it, cronTypes)
                while ((testJobbRepository.harJobb(sakId, behandlingId)) && maxTid.isAfter(LocalDateTime.now())) {
                    Thread.sleep(50L)
                }
                if (maxTid.isAfter(LocalDateTime.now())) {
                    log.warn("Avbryter venting nå, da $maxTid sekunder har gått.")
                }
            }
        }
        log.info("Ventet på at prosessering skulle fullføre, det tok {}", Duration.ofMillis(timeInMillis))
    }
}
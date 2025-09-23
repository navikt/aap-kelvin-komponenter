package no.nav.aap.motor.testutil

import java.time.LocalDateTime
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import no.nav.aap.motor.JobbSpesifikasjon
import no.nav.aap.motor.JobbType
import no.nav.aap.motor.Motor
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

public class ManuellMotorImpl(
    private val dataSource: DataSource,
    jobber: List<JobbSpesifikasjon>,
    private val repositoryRegistry: RepositoryRegistry? = null,
    private val gatewayProvider: GatewayProvider? = null,
) : Motor {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        for (oppgave in jobber) {
            JobbType.leggTil(oppgave)
        }

        for (jobb in jobber) {
            if (jobb is ProviderJobbSpesifikasjon) {
                require(repositoryRegistry != null) {
                    "kan ikke ha jobber med ProviderJobbKonstruktør uten at Motor er gitt et RepositoryRegistry"
                }
            }
            if (jobb is ProvidersJobbSpesifikasjon) {
                require(repositoryRegistry != null) {
                    "kan ikke ha jobber med ProvidersJobbKonstruktør uten at Motor er gitt et RepositoryRegistry"
                }
                require(gatewayProvider != null) {
                    "kan ikke ha jobber med ProvidersJobbKonstruktør uten at Motor er gitt en GatewayProvider"
                }
            }
        }
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun kjører(): Boolean {
        return true
    }

    override fun close() {
    }

    public fun kjørJobber() {
        try {
            var plukker = true
            while (plukker) {
                dataSource.transaction { connection ->
                    val repository = JobbRepository(connection)
                    val plukketJobb = repository.plukkJobb()
                    if (plukketJobb != null) {
                        utfør(plukketJobb, connection)
                    }

                    if (plukker && plukketJobb == null) {
                        plukker = false
                    }
                }
            }
        } catch (exception: Throwable) {
            log.error("Feil under plukking av jobber", exception)
        }
    }

    private fun utfør(jobbInput: JobbInput, connection: DBConnection) {
        try {
            dataSource.transaction { nyConnection ->
                val startTid = System.currentTimeMillis()
                log.info("Starter på jobb :: {}", jobbInput.toString())

                jobbInput.kjør(connection, repositoryRegistry, gatewayProvider)

                val tid = System.currentTimeMillis() - startTid
                log.info("Fullført jobb :: {}. Tok $tid ms.", jobbInput.toString())
                if (jobbInput.erScheduledOppgave()) {
                    JobbRepository(nyConnection).leggTil(
                        jobbInput.medNesteKjøring(
                            jobbInput.cron()!!.nextLocalDateTimeAfter(
                                LocalDateTime.now()
                            )
                        )
                    )
                }
            }
            JobbRepository(connection).markerSomFerdig(jobbInput)
        } catch (exception: Throwable) {
            // Kjører feil
            log.warn("Feil under prosessering av jobb {}", jobbInput, exception)
            JobbRepository(connection).markerSomFeilet(jobbInput, exception)
        }
    }
}
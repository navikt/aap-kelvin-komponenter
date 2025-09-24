package no.nav.aap.motor

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.JobbLogInfoProviderHolder
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.trace.JobbInfoSpanBuilder
import no.nav.aap.motor.trace.OpentelemetryUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.Closeable
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

public interface Motor : Closeable {
    public fun start()
    public fun stop()
    public fun kjører(): Boolean

    public companion object {
        public operator fun invoke(
            dataSource: DataSource,
            antallKammer: Int = 8,
            logInfoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider,
            jobber: List<JobbSpesifikasjon>,
            prometheus: MeterRegistry = SimpleMeterRegistry(),
            repositoryRegistry: RepositoryRegistry? = null,
            gatewayProvider: GatewayProvider? = null,
        ): Motor = MotorImpl(
            dataSource = dataSource,
            antallKammer = antallKammer,
            logInfoProvider = logInfoProvider,
            jobber = jobber,
            prometheus = prometheus,
            repositoryRegistry = repositoryRegistry,
            gatewayProvider = gatewayProvider,
        )
    }
}

public class MotorImpl(
    private val dataSource: DataSource,
    private val antallKammer: Int = 8,
    logInfoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider,
    jobber: List<JobbSpesifikasjon>,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
    private val repositoryRegistry: RepositoryRegistry? = null,
    private val gatewayProvider: GatewayProvider? = null,
) : Motor {

    init {
        JobbLogInfoProviderHolder.set(logInfoProvider)
        for (oppgave in jobber) {
            JobbType.leggTil(oppgave)
        }

        for (jobb in jobber) {
            if (jobb is ProviderJobbSpesifikasjon) {
                requireNotNull(repositoryRegistry) {
                    "kan ikke ha jobber med ProviderJobbKonstruktør uten at Motor er gitt et RepositoryRegistry"
                }
            }
            if (jobb is ProvidersJobbSpesifikasjon) {
                requireNotNull(repositoryRegistry) {
                    "kan ikke ha jobber med ProvidersJobbKonstruktør uten at Motor er gitt et RepositoryRegistry"
                }
                requireNotNull(gatewayProvider) {
                    "kan ikke ha jobber med ProvidersJobbKonstruktør uten at Motor er gitt en GatewayProvider"
                }
            }
        }
    }

    private val log = LoggerFactory.getLogger(Motor::class.java)

    // Benytter virtuals threads istedenfor plattform tråder
    private val executor = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual()
            .name("forbrenningskammer-", 1L)
            .factory()
    )
    private val watchdogExecutor = Executors.newScheduledThreadPool(1)

    @Volatile
    private var stopped = false
    private var started = false
    private val workers = HashMap<Int, Future<*>>()
    private var lastWatchdogLog = LocalDateTime.now()

    public override fun start() {
        log.info("Starter prosessering av jobber")
        IntRange(1, antallKammer).forEach { i ->
            val kammer = Forbrenningskammer(dataSource)
            workers[i] = executor.submit(kammer) // Legger inn en liten spread så det ikke pumpes på tabellen likt
            if (i != antallKammer) {
                Thread.sleep(100)
            }
        }
        log.info("Startet prosessering av jobber")
        watchdogExecutor.schedule(Watchdog(), 1, TimeUnit.MINUTES)
        started = true
    }

    public override fun stop() {
        log.info("Avslutter prosessering av jobber")
        stopped = true
        watchdogExecutor.shutdownNow()
        executor.shutdown()
        val res = executor.awaitTermination(10L, TimeUnit.SECONDS)
        if (!res) {
            log.warn("Forbrenningskammer kunne ikke avsluttes innen 10 sekunder.")
        }
        log.info("Avsluttet prosessering av jobber")
    }

    public override fun kjører(): Boolean {
        return started && !stopped
    }

    override fun close() {
        stop()
    }

    private inner class Forbrenningskammer(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(Forbrenningskammer::class.java)

        override fun run() {
            while (!stopped) {
                log.debug("Starter plukking av jobber")
                try {
                    var plukker = true
                    while (plukker && !stopped) {
                        dataSource.transaction { connection ->
                            val repository = JobbRepository(connection)

                            prometheus.gauge("motor_antall_jobber_klar",
                                repository.antallJobber(JobbStatus.KLAR))
                            prometheus.gauge("motor_antall_jobber_feilet",
                                repository.antallJobber(JobbStatus.FEILET))

                            val plukketJobb = repository.plukkJobb()
                            if (plukketJobb != null) {
                                val behandlingId = plukketJobb.behandlingIdOrNull()
                                val sakId = plukketJobb.sakIdOrNull()
                                OpentelemetryUtil.span(
                                    "jobb + ${plukketJobb.type()}",
                                    behandlingId,
                                    sakId,
                                    plukketJobb.id.toString(),
                                    plukketJobb.status().toString(),
                                    JobbInfoSpanBuilder.jobbAttributter(plukketJobb)
                                ) {
                                    utfør(plukketJobb, connection)
                                }
                            }

                            if (plukker && plukketJobb == null) {
                                plukker = false
                            }
                        }
                    }
                } catch (exception: Throwable) {
                    log.error("Feil under plukking av jobber", exception)
                }
                log.debug("Ingen flere jobber å plukke, hviler litt")
                if (!stopped) {
                    Thread.sleep(500)
                }
            }
        }

        private fun utfør(jobbInput: JobbInput, connection: DBConnection) {
            try {
                dataSource.transaction { nyConnection ->
                    setteLogginformasjonForOppgave(connection, jobbInput)

                    val millis = measureTimeMillis {
                        log.info("Starter på jobb :: $jobbInput")
                        jobbInput.kjør(nyConnection, repositoryRegistry, gatewayProvider)
                    }

                    prometheus.timer(jobbInput).record(millis, TimeUnit.MILLISECONDS)
                    log.info("Fullført jobb :: $jobbInput. Tok $millis ms.")

                    if (jobbInput.erScheduledOppgave()) {
                        scheduleNesteKjøring(nyConnection, jobbInput)
                    }
                }
                JobbRepository(connection).markerSomFerdig(jobbInput)
            } catch (exception: Throwable) {
                // Feil under kjøring av jobb, eller under oppdatering av status til 'kjørt'
                log.warn("Feil under prosessering av jobb :: $jobbInput", exception)

                if (jobbInput.maksFeilNådd()) {
                    prometheus.motorFeiletTeller(jobbInput).increment()
                }
                JobbRepository(connection).markerSomFeilet(jobbInput, exception)
            } finally {
                MDC.clear()
            }
        }

        private fun scheduleNesteKjøring(
            nyConnection: DBConnection,
            jobbInput: JobbInput
        ) {
            JobbRepository(nyConnection).leggTil(
                jobbInput.medNesteKjøring(
                    jobbInput.cron()!!.nextLocalDateTimeAfter(
                        LocalDateTime.now()
                    )
                )
            )
        }

        private fun setteLogginformasjonForOppgave(
            connection: DBConnection,
            jobbInput: JobbInput
        ) {
            MDC.put("jobbid", jobbInput.id?.toString())
            MDC.put("jobbType", jobbInput.type())
            MDC.put("sakId", jobbInput.sakIdOrNull().toString())
            MDC.put("behandlingId", jobbInput.behandlingIdOrNull().toString())
            MDC.put("callId", jobbInput.callId() ?: UUID.randomUUID().toString())

            val logInformasjon = JobbLogInfoProviderHolder.get().hentInformasjon(connection, jobbInput)
            if (logInformasjon != null) {
                for (feltMedVerdi in logInformasjon.felterMedVerdi) {
                    MDC.put(feltMedVerdi.key, feltMedVerdi.value)
                }
            }
        }
    }

    /**
     * Watchdog som sjekker om alle workers kjører
     */
    private inner class Watchdog : Runnable {
        private val logger = LoggerFactory.getLogger(Watchdog::class.java)
        override fun run() {
            logger.debug("Sjekker status på workers")
            try {
                val allRunning = workers.values.all { !it.isDone }

                if (!allRunning && !stopped) {
                    val nyeWorkers: MutableList<Pair<Int, Forbrenningskammer>> = mutableListOf()
                    workers.forEach { (key, value) ->
                        if (value.state() in setOf(Future.State.CANCELLED, Future.State.SUCCESS)) {
                            logger.info("Fant workers som uventet har stoppet [{}]", value)
                            nyeWorkers.addLast(Pair(key, Forbrenningskammer(dataSource)))
                        } else if (value.state() == Future.State.FAILED) {
                            logger.info(
                                "Fant workers som uventet har blitt terminert [{}]",
                                value,
                                value.exceptionNow()
                            )
                            nyeWorkers.addLast(Pair(key, Forbrenningskammer(dataSource)))
                        }
                    }
                    nyeWorkers.forEach {
                        workers[it.first] = executor.submit(it.second)
                    }
                } else if (!stopped) {
                    if (lastWatchdogLog.plusMinutes(30).isBefore(LocalDateTime.now())) {
                        logger.info("Alle workers OK")
                        lastWatchdogLog = LocalDateTime.now()
                    }
                }
            } catch (exception: Throwable) {
                logger.warn("Ukjent feil under watchdog-aktivitet.", exception)
            }
            watchdogExecutor.schedule(Watchdog(), 1, TimeUnit.MINUTES)
        }
    }
}

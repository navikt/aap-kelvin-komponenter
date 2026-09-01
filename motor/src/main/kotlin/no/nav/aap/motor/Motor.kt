package no.nav.aap.motor

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.trace.Span
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.log.SECURE_LOGGER
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.JobbLogInfoProviderHolder
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.trace.JobbInfoSpanBuilder
import no.nav.aap.motor.trace.OpentelemetryUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.Closeable
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

public interface Motor : Closeable {
    public fun start()
    public fun stop(timeout: Duration = 10.seconds)
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
            enableV2: () -> Boolean = { false },
        ): Motor = MotorImpl(
            dataSource = dataSource,
            antallKammer = antallKammer,
            logInfoProvider = logInfoProvider,
            jobber = jobber,
            prometheus = prometheus,
            repositoryRegistry = repositoryRegistry,
            gatewayProvider = gatewayProvider,
            enableV2 = enableV2,
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
    private val enableV2: () -> Boolean = { false },
) : Motor {

    private val antallJobberKlar = AtomicInteger()
    private val antallJobberFeilet = AtomicInteger()

    private val schedulerSistFullført = AtomicLong(Instant.now().epochSecond)

    init {
        prometheus.gauge("motor_antall_jobber_klar", antallJobberKlar)
        prometheus.gauge("motor_antall_jobber_feilet", antallJobberFeilet)
        Gauge.builder("motor_scheduler_siste_kjoring_timestamp_seconds") { schedulerSistFullført.get().toDouble() }.register(prometheus)
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
    private val watchdogExecutor =
        Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("motor-watchdog").factory())
    private val metricExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("motor-metrics").factory())
    private val schedulerExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().name("motor-scheduler").factory())

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
        metricExecutor.scheduleWithFixedDelay(MetricsUpdater(), 0, 60, TimeUnit.SECONDS)
        schedulerExecutor.scheduleWithFixedDelay(Scheduler(), 0, 100, TimeUnit.MILLISECONDS)
        started = true
    }

    public override fun stop(timeout: Duration) {
        log.info("Avslutter prosessering av jobber")
        stopped = true
        watchdogExecutor.shutdownNow()
        metricExecutor.shutdownNow()
        schedulerExecutor.shutdownNow()
        executor.shutdown()
        val res = executor.awaitTermination(timeout.inWholeSeconds, TimeUnit.SECONDS)
        if (!res) {
            log.warn("Forbrenningskammer kunne ikke avsluttes innen ${timeout.inWholeSeconds} sekunder.")
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
        private val kammerId = forbrenningskammerId.getAndIncrement().toString()

        // Én AtomicLong per jobb-type, registrert én gang i prometheus for å unngå DuplicateLabelsException
        private val sistePlukketTimestamps = ConcurrentHashMap<String, AtomicLong>()

        private fun oppdaterSistePlukk(jobbType: String) {
            sistePlukketTimestamps
                .getOrPut(jobbType) {
                    AtomicLong(Instant.now().epochSecond).also { tidspunkt ->
                        Gauge.builder("motor_siste_plukk_timestamp_seconds") { tidspunkt.get().toDouble() }
                            .tag("forbrenningskammer", kammerId)
                            .tag("jobb_type", jobbType)
                            .register(prometheus)
                    }
                }
                .set(Instant.now().epochSecond)
        }

        override fun run() {
            while (!stopped) {
                log.debug("Starter plukking av jobber")
                try {
                    var plukker = true
                    while (plukker && !stopped) {
                        dataSource.transaction(name = "jobbPlukkTransaction") { connection ->
                            val repository = JobbRepository(connection)
                            val plukketJobb = if (enableV2()) repository.plukkJobbV2() else repository.plukkJobb()

                            /* Ønsker å oppdage trege jobber før jobben har kjørt ferdig (f.eks. pga deadlock).
                            * Registrerer derfor hvert (potensielle) start-tidspunkt for en jobb, slikt at vi i
                            * grafana kan regne ut hvor lenge siden vi sist prøvde å plukke en jobb.
                            *
                            * Metricen gir mening først når jobber tar lenger tid (>= 1 sekund, gitt
                            * Thread.sleep(500) nedenfor).
                            *
                            * Tenkt bruk:
                            * timestamp(motor_siste_plukk_timestamp_seconds) - motor_siste_plukk_timestamp_seconds
                            **/

                            if (plukketJobb != null) {
                                Span.current().updateName("jobbPlukk + ${plukketJobb.type()}")
                                oppdaterSistePlukk(plukketJobb.type())
                                log.info("Plukket jobb $plukketJobb.")
                                val behandlingId = plukketJobb.behandlingIdOrNull()
                                val sakId = plukketJobb.sakIdOrNull()
                                OpentelemetryUtil.span(
                                    navn = "jobb + ${plukketJobb.type()}",
                                    behandlingId = behandlingId,
                                    sakId = sakId,
                                    jobbStatus = plukketJobb.status().toString(),
                                    jobbId = plukketJobb.id.toString(),
                                    spanBuilderTransformer = JobbInfoSpanBuilder.jobbAttributter(plukketJobb)
                                ) {
                                    utfør(plukketJobb, connection)
                                }
                            } else {
                                Span.current().updateName("jobbPlukk + ingenJobb")
                                plukker = false
                            }
                        }
                    }
                } catch (exception: Throwable) {
                    log.error("Feil under plukking av jobber", exception)
                }
                log.debug("Ingen flere jobber å plukke, hviler litt")
                // Nullstill til nå slik at query-en viser ~0 når kammeret er ledig
                val nå = Instant.now().epochSecond
                sistePlukketTimestamps.values.forEach { it.set(nå) }
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
                // Feil under kjøring av jobb, eller under oppdatering av status til 'kjørt', eller scheduling av neste
                log.warn("Feil under prosessering av jobb :: $jobbInput {}. Se secure logs.", exception.javaClass.simpleName)
                SECURE_LOGGER.warn("Feil under prosessering av jobb :: $jobbInput", exception)

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

    private inner class Scheduler : Runnable {
        private val logger = LoggerFactory.getLogger(Scheduler::class.java)
        private var lastErrorLog = Instant.MIN

        override fun run() {
            if (stopped) {
                logger.info("Stopper skjedulering av jobber")
                return
            }
            try {
                if (enableV2()) {
                    val millis = measureTimeMillis {
                        dataSource.transaction {
                            val antallSkjedulert = JobbRepository(it).skjedulerJobber()
                            if (antallSkjedulert > 0) {
                                logger.info("markerte $antallSkjedulert jobber som klare for å kjøre")
                            }
                        }
                    }
                    prometheus.motorSchedulerTimer().record(millis, TimeUnit.MILLISECONDS)
                    if (millis > TREG_SKJEDULERING_TERSKEL.inWholeMilliseconds) {
                        logger.warn(
                            "Skjedulering tok $millis ms. Sjekk radlåser og indekser på JOBB (pg_stat_activity)."
                        )
                    }
                }
            } catch (e: Throwable) {
                prometheus.motorSchedulerFeiletTeller().increment()
                val now = Instant.now()
                if (lastErrorLog.plusSeconds(FEILLOGG_INTERVALL.inWholeSeconds) < now) {
                    logger.error("Scheduler feilet: {}", e.message, e)
                    lastErrorLog = now
                }
            } finally {
                schedulerSistFullført.set(Instant.now().epochSecond)
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

                sjekkScheduler()
            } catch (exception: Throwable) {
                logger.warn("Ukjent feil under watchdog-aktivitet.", exception)
            } finally {
                // Reschedulering må skje uansett, ellers stopper watchdogen permanent.
                // Unngå RejectedExecutionException når executor er avsluttet under stop().
                if (!stopped) {
                    try {
                        watchdogExecutor.schedule(Watchdog(), 1, TimeUnit.MINUTES)
                    } catch (e: java.util.concurrent.RejectedExecutionException) {
                        logger.debug("Watchdog ikke reschedulert, executor er avsluttet.", e)
                    }
                }
            }
        }

        private fun sjekkScheduler() {
            if (stopped || !enableV2()) {
                return
            }
            val sekunderSiden = Instant.now().epochSecond - schedulerSistFullført.get()
            if (sekunderSiden > SCHEDULER_STILLE_TERSKEL.inWholeSeconds) {
                logger.error(
                    "Scheduler har ikke fullført en kjøring på $sekunderSiden sekunder. " +
                            "Sannsynligvis blokkert på en radlås eller en treg spørring mot JOBB."
                )
            }
        }
    }

    private inner class MetricsUpdater : Runnable {
        private val log = LoggerFactory.getLogger(javaClass)
        override fun run() {
            try {
                dataSource.transaction(readOnly = true) { connection ->
                    val repository = JobbRepository(connection)
                    antallJobberKlar.set(repository.antallJobber(JobbStatus.KLAR))
                    antallJobberFeilet.set(repository.antallJobber(JobbStatus.FEILET))
                }
            } catch (e: Throwable) {
                // Se kommentaren i Scheduler.run() - scheduleWithFixedDelay kansellerer permanent
                // hvis noe propagerer ut herfra.
                log.warn("Ukjent feil ved oppdatering av motor-metrics: {}", e.javaClass.name, e)
            }
        }
    }

    public companion object {
        private val forbrenningskammerId = AtomicInteger()

        /** Skjedulering som tar lenger tid enn dette logges som advarsel. */
        private val TREG_SKJEDULERING_TERSKEL = 30.seconds

        /** Hvor lenge Scheduler kan være uten en fullført kjøring før watchdog slår alarm. */
        private val SCHEDULER_STILLE_TERSKEL = 2.minutes

        /** Minste tid mellom to feillogger fra Scheduler, slik at et vedvarende problem ikke spammer loggen. */
        private val FEILLOGG_INTERVALL = 1.minutes
    }
}

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

public class Motor(
    private val dataSource: DataSource,
    private val antallKammer: Int = 8,
    logInfoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider,
    jobber: List<JobbSpesifikasjon>,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
    private val repositoryRegistry: RepositoryRegistry? = null,
    private val gatewayProvider: GatewayProvider? = null,
) : Closeable {

    init {
        JobbLogInfoProviderHolder.set(logInfoProvider)
        for (oppgave in jobber) {
            JobbType.leggTil(oppgave)
        }

        for (jobb in jobber) {
            if (jobb is ProviderJobbSpesifikasjon) {
                require(repositoryRegistry != null) {
                    "kan ikke ha jobber med ProviderJobbKonstruktør uten at Motor er gitt et RepositoryRegistry"
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

    public fun start() {
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

    public fun stop() {
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

    public fun kjører(): Boolean {
        return started && !stopped
    }

    override fun close() {
        stop()
    }

    private inner class Forbrenningskammer(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(Forbrenningskammer::class.java)

        private var plukker = true
        override fun run() {
            while (!stopped) {
                log.debug("Starter plukking av jobber")
                try {
                    while (plukker && !stopped) {
                        dataSource.transaction { connection ->
                            val repository = JobbRepository(connection)
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
                } catch (excetion: Throwable) {
                    log.warn("Feil under plukking av jobber", excetion)
                }
                log.debug("Ingen flere jobber å plukke, hviler litt")
                if (!stopped) {
                    Thread.sleep(500)
                }
                plukker = true
            }
        }

        private fun utfør(jobbInput: JobbInput, connection: DBConnection) {
            try {
                dataSource.transaction { nyConnection ->
                    setteLogginformasjonForOppgave(connection, jobbInput)

                    val startTid = System.currentTimeMillis()
                    log.info("Starter på jobb :: {}", jobbInput.toString())


                    val jobbUtfører = when (val konstruktør = jobbInput.jobb) {
                        is ConnectionJobbSpesifikasjon -> konstruktør.konstruer(nyConnection)
                        is ProviderJobbSpesifikasjon -> konstruktør.konstruer(repositoryRegistry!!.provider(nyConnection), gatewayProvider)
                    }

                    jobbUtfører.utfør(jobbInput)

                    val tid = System.currentTimeMillis() - startTid

                    prometheus.timer(jobbInput).record(tid, TimeUnit.MILLISECONDS)

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
                JobbRepository(connection).markerKjørt(jobbInput)
            } catch (exception: Throwable) {
                // Kjører feil
                log.warn(
                    "Feil under prosessering av jobb {}",
                    jobbInput,
                    exception
                )
                if (jobbInput.skalMarkeresSomFeilet()) {
                    prometheus.motorFeiletTeller(jobbInput).increment()
                }
                JobbRepository(connection).markerFeilet(jobbInput, exception)
            } finally {
                MDC.clear()
            }
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

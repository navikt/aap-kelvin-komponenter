package no.nav.aap.komponenter.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import net.logstash.logback.appender.LogstashTcpSocketAppender
import net.logstash.logback.encoder.LogstashEncoder
import net.logstash.logback.mask.MaskingJsonGeneratorDecorator
import org.slf4j.LoggerFactory

/**
 * Programmatisk oppsett av logback, som en erstatning for en tilsvarende `logback.xml`.
 *
 * Setter opp:
 * - en "json"-appender på rot-loggeren, som skriver til stdout i logstash-format, og maskerer verdier
 *   som ser ut som fødselsnummer/d-nummer.
 * - en "team-logs"-appender, som sender logger til team-logs over TCP, for logger skrevet til
 *   [no.nav.aap.komponenter.log.SECURE_LOGGER].
 * - en "auditLogger"-appender, som sender sporingslogger til NAIS sin audit-logg via syslog. Denne er
 *   valgfri, og krever at `com.papertrailapp:logback-syslog4j` finnes på classpath under kjøring.
 * - dempet loggenivå (WARN) for token-provider-logging i httpklient-biblioteket.
 *
 * **Automatisk oppsett:** [LogbackProduksjonConfigurator] registrerer produksjonsoppsettet under
 * [no.nav.aap.komponenter.log] som en logback [ch.qos.logback.classic.spi.Configurator] via
 * `META-INF/services`. Dette gjør at logback oppdager og kjører oppsettet automatisk ved første
 * logger-oppslag. `infrastructure`s eget testFixtures-source-set registrerer tilsvarende en
 * `LogbackTestConfigurator` med høyere rangering, som overstyrer produksjonsoppsettet når den også
 * finnes på classpath (typisk som `testImplementation`).
 *
 * [konfigurer] finnes i tillegg som en eksplisitt/manuell inngang, f.eks. for tester som vil styre
 * oppsettet selv, eller for å tvinge frem en rekonfigurering.
 */
public object LogbackOppsett {

    private const val TEAM_LOGS_LOGGER_NAME = "team-logs"
    private const val AUDIT_LOGGER_NAME = "auditLogger"
    private const val TOKENPROVIDER_LOGGER_NAME = "no.nav.aap.komponenter.httpklient.httpclient.tokenprovider"
    private const val TEAM_LOGS_DESTINASJON = "team-logs.nais-system:5170"
    private const val STANDARD_TEST_MØNSTER =
        "%cyan(%d{HH:mm:ss.SSS}) %gray([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n%xEx"

    /**
     * Konfigurerer produksjonsoppsettet programmatisk. Kalles typisk helt i starten av `main`, før noe
     * annet logges, eller manuelt fra tester som eksplisitt vil ha produksjonsoppsettet. De fleste
     * konsumenter trenger ikke å kalle denne i det hele tatt - se klassedokumentasjonen om automatisk
     * oppsett via [ch.qos.logback.classic.spi.Configurator].
     *
     * @param auditLogging Slår på/av auditLogger-oppsettet. Standardverdien slår det på automatisk
     * dersom syslog4j-avhengigheten finnes på classpath under kjøring.
     */
    public fun konfigurer(auditLogging: Boolean = erSyslog4jTilgjengelig()) {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.reset()
        konfigurerProduksjon(context, auditLogging)
    }

    /**
     * Bygger det enkle konsoll-oppsettet som brukes i test-scope. Brukes av
     * `no.nav.aap.komponenter.log.LogbackTestConfigurator` i `infrastructure`s testFixtures-source-set.
     *
     * Konsumenter som trenger å tilpasse test-oppsettet (f.eks. et annet mønster, dempede loggenivåer for
     * støyende biblioteker, eller en enkel konsoll-variant av auditLogger) kan registrere sin egen
     * [ch.qos.logback.classic.spi.Configurator] (i sine egne test-kilder, med
     * `@ConfiguratorRank(ConfiguratorRank.CUSTOM_TOP_PRIORITY)` slik at den vinner over denne modulens
     * standardoppsett) som kaller denne funksjonen med tilpassede argumenter.
     *
     * @param pattern Loggmønsteret som brukes av konsoll-appenderen på rot-loggeren.
     * @param loggerNivåer Ekstra loggenivåer som settes på navngitte loggere, f.eks. for å dempe støy fra
     * tredjepartsbiblioteker (`io.netty`, `org.postgresql`, `com.zaxxer.hikari` og lignende).
     * @param inkluderAuditLogger Legger i tillegg til en enkel konsoll-variant av `auditLogger`
     * (`%m%n%xEx`-mønster, uten additivity), for tester som logger til [no.nav.aap.komponenter.log]s
     * auditLogger og forventer at den er konfigurert.
     */
    public fun konfigurerTest(
        context: LoggerContext,
        pattern: String = STANDARD_TEST_MØNSTER,
        loggerNivåer: Map<String, Level> = emptyMap(),
        inkluderAuditLogger: Boolean = false,
    ) {
        val konsoll = konsollAppender(context, pattern)

        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).apply {
            level = Level.INFO
            addAppender(konsoll)
        }

        // Speiler oppsettet i produksjon der team-logs-loggeren ikke er additiv, men send den til den
        // samme konsoll-appenderen som resten av loggingen i test-scope, i stedet for til team-logs over TCP.
        context.getLogger(TEAM_LOGS_LOGGER_NAME).apply {
            level = Level.TRACE
            isAdditive = false
            addAppender(konsoll)
        }

        loggerNivåer.forEach { (navn, nivå) -> context.getLogger(navn).level = nivå }

        if (inkluderAuditLogger) {
            context.getLogger(AUDIT_LOGGER_NAME).apply {
                level = Level.INFO
                isAdditive = false
                addAppender(testAuditLoggerAppender(context))
            }
        }
    }

    /**
     * Bygger produksjonsoppsettet (json/team-logs/tokenprovider/auditLogger). Brukes av
     * [LogbackProduksjonConfigurator].
     */
    public fun konfigurerProduksjon(context: LoggerContext, auditLogging: Boolean = erSyslog4jTilgjengelig()) {
        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).apply {
            level = Level.INFO
            addAppender(jsonAppender(context))
        }

        context.getLogger(TEAM_LOGS_LOGGER_NAME).apply {
            level = Level.INFO
            isAdditive = false
            addAppender(teamLogsAppender(context))
        }

        context.getLogger(TOKENPROVIDER_LOGGER_NAME).level = Level.WARN

        if (auditLogging) {
            konfigurerAuditLogger(context)
        }
    }

    private fun konsollAppender(context: LoggerContext, pattern: String = STANDARD_TEST_MØNSTER): Appender<ILoggingEvent> {
        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            this.pattern = pattern
            start()
        }
        return ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            name = "console"
            this.encoder = encoder
            start()
        }
    }

    private fun testAuditLoggerAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            pattern = "%m%n%xEx"
            start()
        }
        return ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            name = "auditLogger"
            this.encoder = encoder
            start()
        }
    }

    private fun jsonAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val maskering = MaskingJsonGeneratorDecorator().apply {
            // Masker fødselsnummer/d-nummer (11 sifre), men behold de 6 første sifrene (fødselsdato).
            addValueMask(
                MaskingJsonGeneratorDecorator.ValueMask("\\b(\\d{6})\\d{5}\\b", "$1*****"),
            )
            // Må startes eksplisitt, i motsetning til i XML-oppsett der Joran starter nøstede
            // LifeCycle-komponenter automatisk. Uten dette forblir `delegate` null og decorate() feiler med NPE.
            start()
        }
        val encoder = LogstashEncoder().apply {
            this.context = context
            addDecorator(maskering)
            start()
        }
        return ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            name = "json"
            this.encoder = encoder
            start()
        }
    }

    private fun teamLogsAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val customFields = customFieldsJson(
            "google_cloud_project" to (System.getenv("GOOGLE_CLOUD_PROJECT") ?: ""),
            "nais_namespace_name" to (System.getenv("NAIS_NAMESPACE") ?: ""),
            "nais_pod_name" to (System.getenv("NAIS_POD_NAME") ?: ""),
            "nais_container_name" to (System.getenv("NAIS_APP_NAME") ?: ""),
        )
        val encoder = LogstashEncoder().apply {
            this.context = context
            setCustomFields(customFields)
            isIncludeContext = false
            start()
        }
        return LogstashTcpSocketAppender().apply {
            this.context = context
            name = TEAM_LOGS_LOGGER_NAME
            addDestination(TEAM_LOGS_DESTINASJON)
            this.encoder = encoder
            start()
        }
    }

    private fun customFieldsJson(vararg felter: Pair<String, String>): String =
        felter.joinToString(prefix = "{", postfix = "}") { (navn, verdi) ->
            "\"$navn\":\"${verdi.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }

    private fun konfigurerAuditLogger(context: LoggerContext) {
        try {
            val appender = AuditLoggerAppenderFactory.byggAppender(context)
            context.getLogger(AUDIT_LOGGER_NAME).apply {
                level = Level.INFO
                isAdditive = false
                addAppender(appender)
            }
        } catch (feil: NoClassDefFoundError) {
            LoggerFactory.getLogger(LogbackOppsett::class.java).error(
                "Fant ikke com.papertrailapp:logback-syslog4j på classpath, " +
                    "hopper over oppsett av auditLogger. Legg til avhengigheten dersom auditlogging er ønsket.",
                feil,
            )
        }
    }

    private fun erSyslog4jTilgjengelig(): Boolean =
        try {
            Class.forName(
                "com.papertrailapp.logback.Syslog4jAppender",
                false,
                LogbackOppsett::class.java.classLoader,
            )
            true
        } catch (_: ClassNotFoundException) {
            false
        }
}

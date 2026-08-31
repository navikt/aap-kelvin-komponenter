package no.nav.aap.komponenter.log

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import com.papertrailapp.logback.Syslog4jAppender
import org.productivity.java.syslog4j.impl.net.tcp.TCPNetSyslogConfig

/**
 * Bygger auditLogger-appenderen (sporingslogg til NAIS sin audit-logg via syslog).
 *
 * Ligger i en egen klasse slik at referanser til `com.papertrailapp`/`syslog4j`-klassene ikke lastes
 * (og dermed ikke feiler) med mindre auditlogging faktisk er slått på og avhengigheten er til stede.
 */
internal object AuditLoggerAppenderFactory {

    private const val AUDIT_HOST = "audit.nais"
    private const val AUDIT_PORT = 6514
    private const val MAX_MELDINGSLENGDE = 128_000

    fun byggAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val layout = PatternLayout().apply {
            this.context = context
            pattern = "%m%n%xEx"
            start()
        }
        val syslogConfig = TCPNetSyslogConfig().apply {
            host = AUDIT_HOST
            port = AUDIT_PORT
            ident = System.getenv("NAIS_APP_NAME") ?: "ukjent-app"
            maxMessageLength = MAX_MELDINGSLENGDE
        }
        return Syslog4jAppender<ILoggingEvent>().apply {
            this.context = context
            name = "auditLogger"
            this.layout = layout
            this.syslogConfig = syslogConfig
            start()
        }
    }
}

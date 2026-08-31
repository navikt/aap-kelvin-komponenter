package no.nav.aap.komponenter.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import net.logstash.logback.appender.LogstashTcpSocketAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

internal class LogbackOppsettTest {

    @Test
    fun `konfigurerer json-appender på rot-loggeren`() {
        LogbackOppsett.konfigurer(auditLogging = false)

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

        assertThat(root.level).isEqualTo(Level.INFO)
        assertThat(root.getAppender("json")).isNotNull
    }

    @Test
    fun `konfigurerer team-logs-logger uten additivity`() {
        LogbackOppsett.konfigurer(auditLogging = false)

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val teamLogs = context.getLogger("team-logs")

        assertThat(teamLogs.level).isEqualTo(Level.INFO)
        assertThat(teamLogs.isAdditive).isFalse
        assertThat(teamLogs.getAppender("team-logs")).isInstanceOf(LogstashTcpSocketAppender::class.java)
    }

    @Test
    fun `demper loggenivå for tokenprovider`() {
        LogbackOppsett.konfigurer(auditLogging = false)

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val logger = context.getLogger("no.nav.aap.komponenter.httpklient.httpclient.tokenprovider")

        assertThat(logger.level).isEqualTo(Level.WARN)
    }

    @Test
    fun `konfigurerer auditLogger når syslog4j-avhengigheten finnes`() {
        LogbackOppsett.konfigurer(auditLogging = true)

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val auditLogger = context.getLogger("auditLogger")

        assertThat(auditLogger.level).isEqualTo(Level.INFO)
        assertThat(auditLogger.isAdditive).isFalse
        assertThat(auditLogger.getAppender("auditLogger")).isNotNull
    }

    @Test
    fun `konfigurerTest bygger et enkelt konsoll-oppsett`() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.reset()
        LogbackOppsett.konfigurerTest(context)

        val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

        assertThat(root.level).isEqualTo(Level.INFO)
        assertThat(root.getAppender("console")).isNotNull
        assertThat(root.getAppender("json")).isNull()
        assertThat(context.getLogger("team-logs").getAppender("team-logs")).isNull()
    }

    @Test
    fun `konfigurerTest tar imot egendefinerte loggenivåer og et eget auditLogger-oppsett`() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.reset()
        LogbackOppsett.konfigurerTest(
            context,
            loggerNivåer = mapOf("io.netty" to Level.INFO, "org.postgresql" to Level.WARN),
            inkluderAuditLogger = true,
        )

        assertThat(context.getLogger("io.netty").level).isEqualTo(Level.INFO)
        assertThat(context.getLogger("org.postgresql").level).isEqualTo(Level.WARN)

        val auditLogger = context.getLogger("auditLogger")
        assertThat(auditLogger.isAdditive).isFalse
        assertThat(auditLogger.getAppender("auditLogger")).isNotNull
    }
}

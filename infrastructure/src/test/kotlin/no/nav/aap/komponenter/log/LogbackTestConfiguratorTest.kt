package no.nav.aap.komponenter.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.ConfiguratorRank
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

internal class LogbackTestConfiguratorTest {

    @Test
    fun `bygger et konsoll-oppsett uten json-team-logs-eller-auditLogger`() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val status = LogbackTestConfigurator().configure(context)

        val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        assertThat(root.level).isEqualTo(Level.INFO)
        assertThat(root.getAppender("console")).isNotNull
        assertThat(root.getAppender("json")).isNull()
        assertThat(context.getLogger("team-logs").getAppender("team-logs")).isNull()
        assertThat(status).isEqualTo(Configurator.ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY)
    }

    @Test
    fun `er rangert høyere enn produksjonsoppsettet slik at den vinner når begge finnes på classpath`() {
        val testRank = LogbackTestConfigurator::class.java.getAnnotation(ConfiguratorRank::class.java).value
        val produksjonRank =
            LogbackProduksjonConfigurator::class.java.getAnnotation(ConfiguratorRank::class.java).value

        assertThat(testRank).isGreaterThan(produksjonRank)
    }
}

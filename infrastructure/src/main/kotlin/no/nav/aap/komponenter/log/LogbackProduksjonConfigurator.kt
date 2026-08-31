package no.nav.aap.komponenter.log

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus
import ch.qos.logback.classic.spi.ConfiguratorRank
import ch.qos.logback.core.spi.ContextAwareBase

/**
 * Registrerer [LogbackOppsett] sitt produksjonsoppsett som en logback [Configurator]. Logback oppdager
 * og kjører denne automatisk ved første logger-oppslag (via [java.util.ServiceLoader], se
 * `META-INF/services/ch.qos.logback.classic.spi.Configurator`) - akkurat slik den ellers ville oppdaget
 * en `logback.xml` på classpath. Ingen kode i konsumerende applikasjon trenger å kalle dette eksplisitt.
 *
 * Rangert med [ConfiguratorRank.CUSTOM_NORMAL_PRIORITY]. `infrastructure`s testFixtures-source-set
 * registrerer en tilsvarende `LogbackTestConfigurator` med høyere rangering
 * ([ConfiguratorRank.CUSTOM_HIGH_PRIORITY]), som dermed overstyrer dette oppsettet når den finnes på
 * classpath samtidig (typisk lagt til som `testImplementation(testFixtures(project(":infrastructure")))`
 * i konsumerende prosjekter).
 */
@ConfiguratorRank(ConfiguratorRank.CUSTOM_NORMAL_PRIORITY)
public class LogbackProduksjonConfigurator : ContextAwareBase(), Configurator {

    override fun configure(loggerContext: LoggerContext): ExecutionStatus {
        loggerContext.reset()
        LogbackOppsett.konfigurerProduksjon(loggerContext)
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY
    }
}

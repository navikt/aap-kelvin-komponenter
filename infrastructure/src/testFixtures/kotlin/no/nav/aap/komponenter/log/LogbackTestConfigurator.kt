package no.nav.aap.komponenter.log

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus
import ch.qos.logback.classic.spi.ConfiguratorRank
import ch.qos.logback.core.spi.ContextAwareBase

/**
 * Test-variant av logback-oppsettet: et enkelt, menneskelesbart konsoll-oppsett, uten json/team-logs/
 * auditLogger. Registrert som en logback [Configurator] via `META-INF/services`, akkurat som
 * [LogbackProduksjonConfigurator], men i `infrastructure`s eget `testFixtures`-source-set (satt opp med
 * Gradle-pluginen `java-test-fixtures`), slik at den kun havner på classpath når et prosjekt eksplisitt
 * legger til `testImplementation(testFixtures(project(":infrastructure")))`.
 *
 * Rangert med [ConfiguratorRank.CUSTOM_HIGH_PRIORITY] - høyere enn [LogbackProduksjonConfigurator]s
 * [ConfiguratorRank.CUSTOM_NORMAL_PRIORITY]. Logback sorterer alle oppdagede [Configurator]er etter
 * rangering og prøver dem i rekkefølge til en returnerer [ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY].
 * Når testFixtures-artifaktet legges til som `testImplementation` i et prosjekt som også har
 * `infrastructure` som (transitiv) avhengighet, vil derfor dette test-oppsettet automatisk overstyre
 * produksjonsoppsettet i test-scope - uten at noen testkode må be om det eksplisitt. Dette tilsvarer
 * hvordan `logback-test.xml` på classpath overstyrer `logback.xml`.
 */
@ConfiguratorRank(ConfiguratorRank.CUSTOM_HIGH_PRIORITY)
public class LogbackTestConfigurator : ContextAwareBase(), Configurator {

    override fun configure(loggerContext: LoggerContext): ExecutionStatus {
        loggerContext.reset()
        LogbackOppsett.konfigurerTest(loggerContext)
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY
    }
}

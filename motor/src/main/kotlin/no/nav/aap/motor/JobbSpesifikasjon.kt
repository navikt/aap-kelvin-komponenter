package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.cron.CronExpression
import java.time.Duration

public sealed interface JobbSpesifikasjon {
    public val type: String

    public val navn: String

    public val beskrivelse: String

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    public val retries: Int
        get() = 3

    /**
     * Backoff-tid ved en feilet jobb - kan kun settes på selvstendige jobber som ikke må kjøres i sekvens med andre
     * F.eks. skal ikke prosesserBehandlingJobb og lignende som er en del av innebygd orkestrering bruke denne.
     * Iverksetting til utbetaling har behov da det er nyttig å kunne rekjøre senere
     */
    public val retryBackoffTid: Duration?
        get() = null

    /**
     * ved fullføring vil oppgaven schedulere seg selv etter dette mønsteret
     */
    public val cron: CronExpression?
        get() = null
}

public interface ProviderJobbSpesifikasjon: JobbSpesifikasjon {
    public fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører
}

public interface ProvidersJobbSpesifikasjon: JobbSpesifikasjon {
    public fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører
}

public interface ConnectionJobbSpesifikasjon: JobbSpesifikasjon {
    public fun konstruer(connection: DBConnection): JobbUtfører
}
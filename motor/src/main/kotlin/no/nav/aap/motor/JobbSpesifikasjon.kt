package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.cron.CronExpression

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
     * ved fullføring vil oppgaven schedulere seg selv etter dette mønsteret
     */
    public val cron: CronExpression?
        get() = null
}

public interface ProviderJobbSpesifikasjon: JobbSpesifikasjon {
    public fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører
}

public interface ConnectionJobbSpesifikasjon: JobbSpesifikasjon {
    public fun konstruer(connection: DBConnection): JobbUtfører
}

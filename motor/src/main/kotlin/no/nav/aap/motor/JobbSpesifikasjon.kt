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
     * Backoff-tid ved en feilet jobb. Kan brukes både på selvstendige jobber og på jobber som
     * inngår i en eksklusivitetsgruppe (samme sak_id/behandling_id/type).
     *
     * For jobber i en eksklusivitetsgruppe: jobben beholder sin plass i køen (kjorbar=true) mens
     * den venter på backoff-tiden – ingen andre jobber i samme gruppe kan starte i mellomtiden.
     * Se `JobbRepository.skjedulerEkskluderendeJobber` for detaljer om hvordan dette garanteres.
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
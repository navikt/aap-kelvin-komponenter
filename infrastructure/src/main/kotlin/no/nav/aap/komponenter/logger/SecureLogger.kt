package no.nav.aap.komponenter.logger

import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

/**
 * Felles objekt for bruk av "team-logs". Legger på en marker "TEAM_LOGS" for å kunne filtrere.
 * @see: https://docs.nais.io/observability/logging/how-to/team-logs/
 **/
public object SecureLogger {
    private val marker = MarkerFactory.getMarker("TEAM_LOGS")
    private val delegate = LoggerFactory.getLogger("team-logs")

    public fun info(msg: String) {
        delegate.info(marker, msg)
    }

    public fun info(msg: String, throwable: Throwable? = null) {
        delegate.info(marker, msg, throwable)
    }

    public fun warn(msg: String, throwable: Throwable? = null) {
        delegate.warn(marker, msg, throwable)
    }

    public fun error(msg: String, throwable: Throwable? = null) {
        delegate.error(marker, msg, throwable)
    }
}

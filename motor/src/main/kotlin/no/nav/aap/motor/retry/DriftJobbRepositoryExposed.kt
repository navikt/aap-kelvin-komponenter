package no.nav.aap.motor.retry

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import no.nav.aap.motor.JobbType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

public class DriftJobbRepositoryExposed(connection: DBConnection) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val retryFeiledeOppgaverRepository = RetryFeiledeJobberRepository(connection)
    private val jobbRepository = JobbRepository(connection)

    public fun settNesteKjøring(jobbId: Long, nesteKjøring: LocalDateTime): Int {
        return jobbRepository.settNesteKjøring(jobbId, nesteKjøring)
    }

    public fun markerAlleFeiledeForKlare(): Int {
        return retryFeiledeOppgaverRepository.markerAlleFeiledeForKlare()
    }

    public fun markerFeilendeForKlar(jobbId: Long): Int {
        return retryFeiledeOppgaverRepository.markerFeiledeForKlare(jobbId)
    }

    public fun markerSomAvbrutt(jobbId: Long): Int {
        return retryFeiledeOppgaverRepository.markerSomAvbrutt(jobbId)
    }

    public fun hentAlleFeilende(): List<Pair<JobbInput, String?>> {
        return retryFeiledeOppgaverRepository.hentAlleFeilede()
    }

    public fun hentInfoOmGjentagendeJobber(): List<JobbInput> {
        try {
            return JobbType.cronTypes().mapNotNull { retryFeiledeOppgaverRepository.hentInfoOmSisteAvType(it) }
        } catch (e: Throwable) {
            log.error("Feil under henting av info om gjentagende jobber", e)
            return emptyList()
        }
    }

    public fun hentSisteJobber(antall: Int): List<Pair<JobbInput, String?>> {
        return retryFeiledeOppgaverRepository.hentInfoOmSiste(antall)
    }
}
